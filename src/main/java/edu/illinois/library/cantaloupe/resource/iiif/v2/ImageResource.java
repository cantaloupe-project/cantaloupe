package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resource.CachedImageRepresentation;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Handles IIIF Image API 2.x image requests.
 *
 * @see <a href="http://iiif.io/api/image/2.1/#image-request-parameters">Image
 * Request Operations</a>
 */
public class ImageResource extends IIIF2Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    /**
     * Responds to image requests.
     */
    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }

        final Configuration config     = Configuration.getInstance();
        final List<String> args        = getPathArguments();
        final Identifier identifier    = getIdentifier();
        final CacheFacade cacheFacade  = new CacheFacade();
        final boolean isBypassingCache = isBypassingCache();

        // Assemble the URI parameters into a Parameters object.
        final Parameters params = new Parameters(
                identifier, args.get(1), args.get(2),
                args.get(3), args.get(4), args.get(5));
        final OperationList ops = params.toOperationList();
        ops.getOptions().putAll(getRequest().getReference().getQuery().toMap());

        final String disposition = getRepresentationDisposition(
                getRequest().getReference().getQuery()
                        .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG),
                ops.getIdentifier(), ops.getOutputFormat());

        Format sourceFormat = Format.UNKNOWN;

        // If we are using a cache, and don't need to resolve first:
        // 1. If the cache contains an image matching the request, skip all the
        //    setup and just return the cached image.
        // 2. Otherwise, if the cache contains a relevant info, get it to avoid
        //    having to get it from a source later.
        if (!isBypassingCache && !isResolvingFirst()) {
            final Info info = cacheFacade.getInfo(identifier);
            if (info != null) {
                ops.setScaleConstraint(getScaleConstraint());
                ops.applyNonEndpointMutations(info, getDelegateProxy());

                InputStream cacheStream = null;
                try {
                    cacheStream = cacheFacade.newDerivativeImageInputStream(ops);
                } catch (IOException e) {
                    // Don't rethrow -- it's still possible to service the
                    // request.
                    LOGGER.error(e.getMessage());
                }

                if (cacheStream != null) {
                    addHeaders(disposition,
                            params.getOutputFormat().toFormat().getPreferredMediaType().toString());

                    new CachedImageRepresentation(cacheStream)
                            .write(getResponse().getOutputStream());
                    return;
                } else {
                    Format infoFormat = info.getSourceFormat();
                    if (infoFormat != null) {
                        sourceFormat = infoFormat;
                    }
                }
            }
        }

        final Source source = new SourceFactory().newSource(
                identifier, getDelegateProxy());

        // If we are resolving first, or if the source image is not present in
        // the source cache (if enabled), check access to it in preparation for
        // retrieval.
        final Path sourceImage = cacheFacade.getSourceCacheFile(identifier);
        if (sourceImage == null || isResolvingFirst()) {
            try {
                source.checkAccess();
            } catch (NoSuchFileException e) { // this needs to be rethrown!
                if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                    // If the image was not found, purge it from the cache.
                    cacheFacade.purgeAsync(ops.getIdentifier());
                }
                throw e;
            }
        }

        // If we don't know the format yet, get it.
        if (Format.UNKNOWN.equals(sourceFormat)) {
            // If we are not resolving first, and there is a hit in the source
            // cache, read the format from the source-cached-file, as we will
            // expect source cache access to be more efficient.
            // Otherwise, read it from the source.
            if (!isResolvingFirst() && sourceImage != null) {
                List<MediaType> mediaTypes = MediaType.detectMediaTypes(sourceImage);
                if (!mediaTypes.isEmpty()) {
                    sourceFormat = mediaTypes.get(0).toFormat();
                }
            } else {
                sourceFormat = source.getFormat();
            }
        }

        // Obtain an instance of the processor assigned to that format.
        try (final Processor processor =
                     new ProcessorFactory().newProcessor(sourceFormat)) {
            // Connect it to the source.
            tempFileFuture = new ProcessorConnector().connect(
                    source, processor, identifier, sourceFormat);

            final Info info = getOrReadInfo(ops.getIdentifier(), processor);
            Dimension fullSize;
            try {
                fullSize = info.getSize(getPageIndex());

                ops.setScaleConstraint(getScaleConstraint());
                ops.applyNonEndpointMutations(info, getDelegateProxy());
                ops.freeze();

                getRequestContext().setOperationList(ops, fullSize);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                throw new IllegalClientArgumentException(e);
            }

            if (!authorize()) {
                return;
            }

            processor.validate(ops, fullSize);

            final Dimension virtualSize = info.getOrientationSize();
            final Dimension resultingSize = ops.getResultingSize(info.getSize());
            validateScale(virtualSize, (Scale) ops.getFirst(Scale.class));
            validateSize(resultingSize, virtualSize, processor);

            // Find out whether the processor supports the source format by asking
            // it whether it offers any output formats for it.
            Set<Format> availableOutputFormats = processor.getAvailableOutputFormats();
            if (!availableOutputFormats.isEmpty()) {
                if (!availableOutputFormats.contains(ops.getOutputFormat())) {
                    Exception e = new UnsupportedOutputFormatException(
                            processor, ops.getOutputFormat());
                    LOGGER.warn("{}: {}",
                            e.getMessage(),
                            getRequest().getReference());
                    throw e;
                }
            } else {
                throw new UnsupportedSourceFormatException(sourceFormat);
            }

            addHeaders(params, fullSize, disposition,
                    params.getOutputFormat().toFormat().getPreferredMediaType().toString());

            new ImageRepresentation(info, processor, ops, isBypassingCache)
                    .write(getResponse().getOutputStream());

            // Notify the health checker of a successful response -- after the
            // response has been written successfully, obviously.
            HealthChecker.addSourceProcessorPair(source, processor, ops);
        }
    }

    /**
     * Adds {@code Content-Disposition} and {@code Content-Type} response
     * headers.
     */
    private void addHeaders(String disposition,
                            String contentType) {
        // Content-Disposition
        if (disposition != null) {
            getResponse().setHeader("Content-Disposition", disposition);
        }
        // Content-Type
        getResponse().setHeader("Content-Type", contentType);
    }

    /**
     * Invokes {@link #addHeaders(String, String)} and also adds an {@code Link}
     * header.
     */
    private void addHeaders(Parameters params,
                            Dimension fullSize,
                            String disposition,
                            String contentType) {
        addHeaders(disposition, contentType);

        final Identifier identifier = params.getIdentifier();
        final String paramsStr = params.toCanonicalString(fullSize).replaceFirst(
                identifier.toString(), getPublicIdentifier());
        getResponse().setHeader("Link",
                String.format("<%s%s/%s>;rel=\"canonical\"",
                        getPublicRootReference(),
                        Route.IIIF_2_PATH,
                        paramsStr));
    }

    private void validateSize(Dimension resultingSize,
                              Dimension virtualSize,
                              Processor processor) throws SizeRestrictedException {
        final Configuration config = Configuration.getInstance();

        if (config.getBoolean(Key.IIIF_2_RESTRICT_TO_SIZES, false)) {
            final ImageInfoFactory factory = new ImageInfoFactory(
                    processor.getSupportedFeatures(),
                    processor.getSupportedIIIF2Qualities(),
                    processor.getAvailableOutputFormats());

            final List<ImageInfo.Size> sizes = factory.getSizes(virtualSize);

            boolean ok = false;
            for (ImageInfo.Size size : sizes) {
                if (size.width == resultingSize.intWidth() &&
                        size.height == resultingSize.intHeight()) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new SizeRestrictedException();
            }
        }
    }

    private boolean isResolvingFirst() {
        return Configuration.getInstance().
                getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true);
    }

}
