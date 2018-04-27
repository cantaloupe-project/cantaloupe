package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.resource.CachedImageRepresentation;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import org.restlet.data.Disposition;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
        //    having to get it from a source later.
        if (!isResolvingFirst()) {
            final Info info = cacheFacade.getInfo(identifier);
            if (info != null) {
                ops.applyNonEndpointMutations(info, getDelegateProxy());

                InputStream cacheStream = null;
                try {
                    cacheStream = cacheFacade.newDerivativeImageInputStream(ops);
                } catch (IOException e) {
                    // Don't rethrow -- it's still possible to service the
                    // request.
                    getLogger().severe(e.getMessage());
                }

                if (cacheStream != null) {
                    addLinkHeader(params);
                    commitCustomResponseHeaders();

                    return new CachedImageRepresentation(
                            cacheStream,
                            params.getOutputFormat().toFormat().getPreferredMediaType(),
                            disposition);
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
                sourceFormat = source.getSourceFormat();
            }
        }

        // Obtain an instance of the processor assigned to that format. This
        // must eventually be close()d, but we don't want to close it here
        // unless there is an error.
        final Processor processor = new ProcessorFactory().
                newProcessor(sourceFormat);

        try {
            // Connect it to the source.
            tempFileFuture = new ProcessorConnector().connect(
                    source, processor, identifier, sourceFormat);

            final Info info = getOrReadInfo(ops.getIdentifier(), processor);
            Dimension fullSize;
            try {
                fullSize = info.getSize(getPageIndex());
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalClientArgumentException(e);
            }

            getRequestContext().setOperationList(ops, fullSize);

            StringRepresentation redirectingRep = checkRedirect();
            if (redirectingRep != null) {
                return redirectingRep;
            }

            checkAuthorization();

            validateRequestedArea(ops, sourceFormat, fullSize);

            try {
                processor.validate(ops, fullSize);
            } catch (IllegalArgumentException e) {
                throw new IllegalClientArgumentException(e.getMessage(), e);
            }

            final Dimension resultingSize = ops.getResultingSize(info.getSize());
            validateSize(resultingSize, info.getOrientationSize());

            try {
                ops.applyNonEndpointMutations(info, getDelegateProxy());
            } catch (IllegalStateException e) {
                // applyNonEndpointMutations() will freeze the instance, and it
                // may have already been called. That's fine.
            }

            // Find out whether the processor supports the source format by asking
            // it whether it offers any output formats for it.
            Set<Format> availableOutputFormats = processor.getAvailableOutputFormats();
            if (!availableOutputFormats.isEmpty()) {
                if (!availableOutputFormats.contains(ops.getOutputFormat())) {
                    Exception e = new UnsupportedOutputFormatException(
                            processor, ops.getOutputFormat());
                    getLogger().warning(e.getMessage() + ": " + getReference());
                    throw e;
                }
            } else {
                throw new UnsupportedSourceFormatException(sourceFormat);
            }

            addLinkHeader(params);
            commitCustomResponseHeaders();
            return new ImageRepresentation(info, processor, ops, disposition,
                    isBypassingCache(), () -> {
                if (tempFileFuture != null) {
                    Path tempFile = tempFileFuture.get();
                    if (tempFile != null) {
                        Files.deleteIfExists(tempFile);
                    }
                }
                return null;
            });
        } catch (Throwable t) {
            processor.close();
            throw t;
        }
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

    private void validateSize(Dimension resultingSize, Dimension virtualSize) {
        final Configuration config = Configuration.getInstance();

        if (config.getBoolean(Key.IIIF_2_RESTRICT_TO_SIZES, false)) {
            final List<ImageInfo.Size> sizes =
                    new ImageInfoFactory().getSizes(virtualSize);

            boolean ok = false;
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
    }

    private boolean isResolvingFirst() {
        return Configuration.getInstance().
                getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true);
    }

}
