package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.CachedImageRepresentation;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.util.Series;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
                getReference().getQueryAsForm()
                        .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG),
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

        // Setup the resolver context.
        final RequestContext requestContext = new RequestContext();
        requestContext.setRequestURI(getReference().toString());
        requestContext.setRequestHeaders(getRequest().getHeaders().getValuesMap());
        requestContext.setClientIP(getCanonicalClientIpAddress());
        requestContext.setCookies(getRequest().getCookies().getValuesMap());
        resolver.setContext(requestContext);

        // Determine the format of the source image.
        Format sourceFormat;
        try {
            sourceFormat = resolver.getSourceFormat();
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
        final Processor processor = new ProcessorFactory().
                getProcessor(sourceFormat);

        // Connect it to the resolver.
        new ProcessorConnector(resolver, processor, identifier).connect();

        final Info info = getOrReadInfo(ops.getIdentifier(), processor);
        final Dimension fullSize = info.getSize();

        StringRepresentation redirectingRep = checkAuthorization(ops, fullSize);
        if (redirectingRep != null) {
            return redirectingRep;
        }

        validateRequestedArea(ops, sourceFormat, info);

        processor.validate(ops, fullSize);

        if (config.getBoolean(Key.IIIF_2_RESTRICT_TO_SIZES, false)) {
            final ImageInfo imageInfo = ImageInfoFactory.newImageInfo(
                    identifier, null, processor, info);
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

        return new ImageRepresentation(info, processor, ops, disposition,
                isBypassingCache());
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
