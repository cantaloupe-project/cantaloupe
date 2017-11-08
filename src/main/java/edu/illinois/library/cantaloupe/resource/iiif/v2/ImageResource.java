package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
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
import edu.illinois.library.cantaloupe.resource.SourceImageWrangler;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.data.Reference;
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

    public static final String CONTENT_DISPOSITION_CONFIG_KEY =
            "endpoint.iiif.content_disposition";
    public static final String RESTRICT_TO_SIZES_CONFIG_KEY =
            "endpoint.iiif.2.restrict_to_sizes";

    /**
     * Responds to image requests.
     *
     * @return ImageRepresentation
     */
    @Get
    public Representation doGet() throws Exception {
        final Map<String,Object> attrs = getRequest().getAttributes();
        final String urlIdentifier = (String) attrs.get("identifier");
        final String decodedIdentifier = Reference.decode(urlIdentifier);
        final String reSlashedIdentifier = decodeSlashes(decodedIdentifier);
        final Identifier identifier = new Identifier(reSlashedIdentifier);

        // Assemble the URI parameters into a Parameters object
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

        final Disposition disposition = getRepresentationDisposition(
                ops.getIdentifier(), ops.getOutputFormat());

        // If we don't need to resolve first, and are using a cache, and the
        // cache contains an image matching the request, skip all the setup and
        // just return the cached image.
        if (!ConfigurationFactory.getInstance().
                getBoolean(Cache.RESOLVE_FIRST_CONFIG_KEY, true)) {
            DerivativeCache cache = CacheFactory.getDerivativeCache();
            if (cache != null) {
                InputStream inputStream = cache.newDerivativeImageInputStream(ops);
                if (inputStream != null) {
                    this.addLinkHeader(params);
                    return new CachedImageRepresentation(
                            params.getOutputFormat().getPreferredMediaType(),
                            disposition, inputStream);
                }
            }
        }

        Resolver resolver = ResolverFactory.getResolver(ops.getIdentifier());
        // Determine the format of the source image
        Format format = Format.UNKNOWN;
        try {
            format = resolver.getSourceFormat();
        } catch (FileNotFoundException e) {
            if (ConfigurationFactory.getInstance().
                    getBoolean(Cache.PURGE_MISSING_CONFIG_KEY, false)) {
                // if the image was not found, purge it from the cache
                final Cache cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    cache.purge(ops.getIdentifier());
                }
            }
            throw e;
        }

        final Processor processor = ProcessorFactory.getProcessor(format);

        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        final Info info = getOrReadInfo(ops.getIdentifier(), processor);
        final Dimension fullSize = info.getSize();

        processor.validate(ops, fullSize);

        StringRepresentation redirectingRep = checkAuthorization(ops, fullSize);
        if (redirectingRep != null) {
            return redirectingRep;
        }

        // Will throw an exception if anything is wrong.
        checkRequest(ops, fullSize);

        if (ConfigurationFactory.getInstance().
                getBoolean(RESTRICT_TO_SIZES_CONFIG_KEY, false)) {
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

        addNonEndpointOperations(ops, fullSize);

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

        this.addLinkHeader(params);

        // Add client cache header(s) if configured to do so. We do this later
        // rather than sooner to prevent them from being sent along with an
        // error response.
        getResponseCacheDirectives().addAll(getCacheDirectives());

        return getRepresentation(ops, format, info, disposition, processor);
    }

    private void addLinkHeader(Parameters params) {
        final Series<Header> requestHeaders = getRequest().getHeaders();
        final Identifier identifier = params.getIdentifier();
        final String paramsStr = params.toString().replaceFirst(
                identifier.toString(), getPublicIdentifier());

        getResponse().getHeaders().add("Link",
                String.format("<%s%s/%s>;rel=\"canonical\"",
                getPublicRootRef(getRequest().getRootRef(), requestHeaders),
                WebApplication.IIIF_2_PATH, paramsStr));
    }

}
