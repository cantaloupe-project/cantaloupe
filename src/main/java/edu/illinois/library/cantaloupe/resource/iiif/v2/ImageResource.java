package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.cache.DerivativeFileCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.CachedImageRepresentation;
import edu.illinois.library.cantaloupe.resource.SourceImageWrangler;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.util.Series;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles IIIF Image API 2.x image requests.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-request-parameters">Image
 * Request Operations</a>
 */
public class ImageResource extends IIIF2Resource {

    /**
     * Responds to IIIF Image requests.
     */
    @Get
    public Representation doGet() throws Exception {
        final Configuration config = Configuration.getInstance();
        final Map<String,Object> attrs = getRequest().getAttributes();
        final Identifier identifier = getIdentifier();
        // Assemble the URI parameters into a Parameters object.
        final Parameters params = new Parameters(
                identifier,
                (String) attrs.get("region"),
                (String) attrs.get("size"),
                (String) attrs.get("rotation"),
                (String) attrs.get("quality"),
                (String) attrs.get("format"));
        final OperationList ops = params.toOperationList();
        ops.getOptions().putAll(
                getReference().getQueryAsForm(true).getValuesMap());

        addLinkHeader(params);

        final Disposition disposition = getRepresentationDisposition(
                ops.getIdentifier(), ops.getOutputFormat());

        // If we don't need to resolve first, and are using a cache, and the
        // cache contains an image matching the request, skip all the setup and
        // just return the cached image.
        final DerivativeCache cache = CacheFactory.getDerivativeCache();
        if (!config.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)) {
            if (cache != null) {
                InputStream inputStream = cache.newDerivativeImageInputStream(ops);
                if (inputStream != null) {
                    commitCustomResponseHeaders();
                    return new CachedImageRepresentation(
                            params.getOutputFormat().getPreferredMediaType(),
                            disposition, inputStream);
                }
            }
        }

        Resolver resolver = new ResolverFactory().getResolver(identifier);
        // Determine the format of the source image.
        Format format = Format.UNKNOWN;
        try {
            format = resolver.getSourceFormat();
        } catch (FileNotFoundException e) { // this needs to be rethrown
            if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                // if the image was not found, purge it from the cache
                if (cache != null) {
                    cache.purge(ops.getIdentifier());
                }
            }
            throw e;
        }

        // Obtain an instance of the processor assigned to that format.
        final Processor processor = new ProcessorFactory().getProcessor(format);

        // Connect it to the resolver.
        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        final Dimension fullSize =
                getOrReadInfo(ops.getIdentifier(), processor).getSize();

        processor.validate(ops, fullSize);

        StringRepresentation redirectingRep = checkAuthorization(ops, fullSize);
        if (redirectingRep != null) {
            return redirectingRep;
        }

        // Will throw an exception if anything is wrong.
        checkRequest(ops, fullSize);

        if (config.getBoolean(Key.IIIF_2_RESTRICT_TO_SIZES, false)) {
            final ImageInfo imageInfo = ImageInfoFactory.newImageInfo(
                    identifier, null, processor,
                    getOrReadInfo(identifier, processor));
            final Dimension resultingSize = ops.getResultingSize(fullSize);
            boolean ok = false;
            @SuppressWarnings("unchecked")
            List<ImageInfo.Size> sizes =
                    (List<ImageInfo.Size>) imageInfo.get("sizes");
            for (ImageInfo.Size size : sizes) {
                if (size.width == resultingSize.width &&
                        size.height == resultingSize.height) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new SizeRestrictedException();
            }
        }

        ops.applyNonEndpointMutations(fullSize,
                getCanonicalClientIpAddress(),
                getReference().toUrl(),
                getRequest().getHeaders().getValuesMap(),
                getCookies().getValuesMap());

        // If the reverse proxy supports X-Sendfile, and a file-based
        // derivative cache is enabled, and it contains the info, add an
        // X-Sendfile header. This has to be done *after*
        // OperationList.applyNonEndpointMutations() has been called.
        if (isXSendfileSupported() && cache != null &&
                cache instanceof DerivativeFileCache) {
            DerivativeFileCache fileCache = (DerivativeFileCache) cache;
            if (fileCache.derivativeImageExists(ops)) {
                final Path path = fileCache.getPath(ops);
                addXSendfileHeader(path, fileCache.getRootPath());
                // The proxy server will take it from here.
                Representation rep = new EmptyRepresentation();
                rep.setMediaType(new org.restlet.data.MediaType(ops.getOutputFormat().getPreferredMediaType().toString()));
                return rep;
            }
        }

        // Find out whether the processor supports that source format by
        // asking it whether it offers any output formats for it
        Set<Format> availableOutputFormats = processor.getAvailableOutputFormats();
        if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            String msg = String.format("%s does not support the \"%s\" output format",
                    processor.getClass().getSimpleName(),
                    ops.getOutputFormat().getPreferredExtension());
            getLogger().warning(msg + ": " + this.getReference());
            throw new UnsupportedOutputFormatException(msg);
        }

        commitCustomResponseHeaders();

        return getRepresentation(ops, format, disposition, processor);
    }

    private void addLinkHeader(Parameters params) {
        final Series<Header> headers = getRequest().getHeaders();
        final Identifier identifier = params.getIdentifier();
        final String canonicalIdentifierStr = headers.getFirstValue(
                "X-IIIF-ID", true, identifier.toString());
        final String paramsStr = params.toString().replaceFirst(
                identifier.toString(), canonicalIdentifierStr);

        getBufferedResponseHeaders().add("Link",
                String.format("<%s%s/%s>;rel=\"canonical\"",
                getPublicRootRef(getRequest().getRootRef(), headers),
                RestletApplication.IIIF_2_PATH, paramsStr));
    }

}
