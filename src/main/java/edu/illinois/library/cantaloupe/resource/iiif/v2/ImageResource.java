package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.resource.CachedImageRepresentation;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import org.restlet.data.Disposition;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import java.awt.Dimension;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
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
     * Responds to image requests.
     */
    @Get
    public Representation doGet() throws Exception {
        final Configuration config = Configuration.getInstance();
        final Map<String,Object> attrs = getRequest().getAttributes();
        final Identifier identifier = getIdentifier();
        final CacheFacade cacheFacade = new CacheFacade();

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

        final Disposition disposition = getRepresentationDisposition(
                getReference().getQueryAsForm()
                        .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG),
                ops.getIdentifier(), ops.getOutputFormat());

        Format sourceFormat = Format.UNKNOWN;

        // If we don't need to resolve first, and are using a cache:
        // 1. If the cache contains an image matching the request, skip all the
        //    setup and just return the cached image.
        // 2. Otherwise, if the cache contains a relevant info, get it to avoid
        //    having to get it from a resolver later.
        if (!config.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)) {
            final Info info = cacheFacade.getInfo(identifier);
            if (info != null) {
                ops.applyNonEndpointMutations(info.getSize(),
                        info.getOrientation(),
                        getCanonicalClientIpAddress(),
                        getReference().toUri(),
                        getRequest().getHeaders().getValuesMap(),
                        getCookies().getValuesMap());
                InputStream inputStream =
                        cacheFacade.newDerivativeImageInputStream(ops);
                if (inputStream != null) {
                    addLinkHeader(params);
                    commitCustomResponseHeaders();
                    return new CachedImageRepresentation(
                            params.getOutputFormat().getPreferredMediaType(),
                            disposition, inputStream);
                } else {
                    Format infoFormat = info.getSourceFormat();
                    if (infoFormat != null) {
                        sourceFormat = infoFormat;
                    }
                }
            }
        }

        final Resolver resolver = new ResolverFactory().
                newResolver(identifier, getRequestContext());

        try {
            resolver.checkAccess();
        } catch (NoSuchFileException e) { // this needs to be rethrown!
            if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                // If the image was not found, purge it from the cache.
                cacheFacade.purgeAsync(ops.getIdentifier());
            }
            throw e;
        }

        // If we don't have the format yet, get it from the resolver.
        if (Format.UNKNOWN.equals(sourceFormat)) {
            sourceFormat = resolver.getSourceFormat();
        }

        // Obtain an instance of the processor assigned to that format.
        final Processor processor = new ProcessorFactory().
                newProcessor(sourceFormat);

        // Connect it to the resolver.
        new ProcessorConnector().connect(resolver, processor, identifier);

        final Info info = getOrReadInfo(ops.getIdentifier(), processor);
        final Dimension fullSize = info.getSize();

        StringRepresentation redirectingRep = checkAuthorization(ops, fullSize);
        if (redirectingRep != null) {
            return redirectingRep;
        }

        validateRequestedArea(ops, sourceFormat, info);

        processor.validate(ops, fullSize);

        if (config.getBoolean(Key.IIIF_2_RESTRICT_TO_SIZES, false)) {
            final ImageInfo<String, Object> imageInfo =
                    new ImageInfoFactory().newImageInfo(
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

        try {
            ops.applyNonEndpointMutations(fullSize,
                    info.getOrientation(),
                    getCanonicalClientIpAddress(),
                    getReference().toUri(),
                    getRequest().getHeaders().getValuesMap(),
                    getCookies().getValuesMap());
        } catch (IllegalStateException e) {
            // applyNonEndpointMutations() will freeze the instance, and it
            // may have already been called. That's fine.
        }

        // Find out whether the processor supports the source format by asking
        // it whether it offers any output formats for it.
        Set<Format> availableOutputFormats = processor.getAvailableOutputFormats();
        if (!availableOutputFormats.isEmpty()) {
            if (!availableOutputFormats.contains(ops.getOutputFormat())) {
                String msg = String.format("%s does not support the \"%s\" output format",
                        processor.getClass().getSimpleName(),
                        ops.getOutputFormat().getPreferredExtension());
                getLogger().warning(msg + ": " + getReference());
                throw new UnsupportedOutputFormatException(msg);
            }
        } else {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }

        addLinkHeader(params);
        commitCustomResponseHeaders();
        return new ImageRepresentation(info, processor, ops, disposition,
                isBypassingCache());
    }

    private void addLinkHeader(Parameters params) {
        final Identifier identifier = params.getIdentifier();
        final String paramsStr = params.toString().replaceFirst(
                identifier.toString(), getPublicIdentifier());

        getBufferedResponseHeaders().add("Link",
                String.format("<%s%s/%s>;rel=\"canonical\"",
                getPublicRootReference(),
                RestletApplication.IIIF_2_PATH, paramsStr));
    }

}
