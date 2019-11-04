package edu.illinois.library.cantaloupe.resource.iiif.v1;

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
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles IIIF Image API 1.1 image requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#url-syntax-image-request">Image
 * Request Operations</a>
 */
@SuppressWarnings("Duplicates")
public class ImageResource extends IIIF1Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageResource.class);

    /**
     * Format to assume when no extension is present in the URI.
     */
    private static final Format DEFAULT_FORMAT = Format.JPG;

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
     * <p>Responds to image requests.</p>
     *
     * <p>N.B.: This method only respects {@link
     * Key#CACHE_SERVER_RESOLVE_FIRST} for infos, as doing so with images is
     * not really possible with current API.</p>
     */
    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }

        final Configuration config         = Configuration.getInstance();
        final Identifier identifier        = getIdentifier();
        final CacheFacade cacheFacade      = new CacheFacade();
        final boolean isBypassingCacheRead = isBypassingCacheRead();
        final boolean isBypassingCache     = isBypassingCache();

        Iterator<Format> formatIterator = Collections.emptyIterator();
        boolean isFormatKnownYet = false;

        // If we are using a cache, and don't need to resolve first, see if we
        // can pluck an info from it. This will be more efficient than getting
        // it from a source.
        if (!isBypassingCache && !isBypassingCacheRead && !isResolvingFirst()) {
            try {
                Optional<Info> optInfo = cacheFacade.getInfo(identifier);
                if (optInfo.isPresent()) {
                    Info info = optInfo.get();
                    Format infoFormat = info.getSourceFormat();
                    if (infoFormat != null) {
                        formatIterator = Collections.singletonList(infoFormat).iterator();
                        isFormatKnownYet = true;
                    }
                }
            } catch (IOException e) {
                // Don't rethrow -- it's still possible to service the request.
                LOGGER.error(e.getMessage());
            }
        }

        final Source source = new SourceFactory().newSource(
                identifier, getDelegateProxy());

        // If we are resolving first, or if the source image is not present in
        // the source cache (if enabled), check access to it in preparation for
        // retrieval.
        final Optional<Path> sourceImage = cacheFacade.getSourceCacheFile(identifier);
        if (sourceImage.isEmpty() || isResolvingFirst()) {
            try {
                source.checkAccess();
            } catch (NoSuchFileException e) { // this needs to be rethrown!
                if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                    // If the image was not found, purge it from the cache.
                    cacheFacade.purgeAsync(identifier);
                }
                throw e;
            }
        }

        if (!isFormatKnownYet) {
            // If we are not resolving first, and there is a hit in the source
            // cache, read the format from the source-cached-file, as we will
            // expect source cache access to be more efficient.
            // Otherwise, read it from the source.
            if (!isResolvingFirst() && sourceImage.isPresent()) {
                List<MediaType> mediaTypes = MediaType.detectMediaTypes(sourceImage.get());
                if (!mediaTypes.isEmpty()) {
                    formatIterator = mediaTypes
                            .stream()
                            .map(MediaType::toFormat)
                            .iterator();
                }
            } else {
                formatIterator = source.getFormatIterator();
            }
        }

        while (formatIterator.hasNext()) {
            final Format format = formatIterator.next();
            // Obtain an instance of the processor assigned to this format.
            String processorName = "unknown processor";
            try (Processor processor = new ProcessorFactory().newProcessor(format)) {
                processorName = processor.getClass().getSimpleName();
                // Connect it to the source.
                tempFileFuture = new ProcessorConnector().connect(
                        source, processor, identifier, format);

                final Set<Format> availableOutputFormats =
                        processor.getAvailableOutputFormats();

                final OperationList ops = getOperationList(availableOutputFormats);

                final String disposition = getRepresentationDisposition(
                        ops.getIdentifier(), ops.getOutputFormat());

                final Info info = getOrReadInfo(identifier, processor);
                Dimension fullSize;
                try {
                    fullSize = info.getSize(getPageIndex());
                    getRequestContext().setMetadata(info.getMetadata());

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
                validateScale(info.getMetadata().getOrientation().adjustedSize(fullSize),
                        (Scale) ops.getFirst(Scale.class));

                addHeaders(processor, ops.getOutputFormat(), disposition);

                new ImageRepresentation(info, processor, ops,
                        isBypassingCacheRead, isBypassingCache)
                        .write(getResponse().getOutputStream());

                // Notify the health checker of a successful response.
                HealthChecker.addSourceProcessorPair(source, processor, ops);
                return;
            } catch (SourceFormatException e) {
                LOGGER.debug("Format inferred by {} disagrees with the one " +
                                "supplied by {} ({}) for {}; trying again",
                        processorName, source, format, identifier);
            }
        }
        throw new SourceFormatException();
    }

    private void addHeaders(Processor processor,
                            Format outputFormat,
                            String disposition) {
        if (disposition != null) {
            getResponse().setHeader("Content-Disposition", disposition);
        }
        getResponse().setHeader("Content-Type",
                outputFormat.getPreferredMediaType().toString());

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                processor.getAvailableOutputFormats());
        getResponse().setHeader("Link",
                String.format("<%s>;rel=\"profile\";", complianceLevel.getUri()));
    }

    private OperationList getOperationList(Set<Format> availableOutputFormats) {
        final List<String> args = getPathArguments();

        // If the URI path contains a format extension, try to use that.
        // Otherwise, negotiate it based on the Accept header per Image API 1.1
        // spec section 4.5.
        String outputFormat;
        try {
            outputFormat = args.get(5);
        } catch (IndexOutOfBoundsException e) {
            outputFormat = getEffectiveOutputFormat(availableOutputFormats).
                    getPreferredExtension();
        }

        final Identifier identifier = getIdentifier();
        final Parameters params = new Parameters(
                identifier, args.get(1), args.get(2),
                args.get(3), args.get(4), outputFormat);

        final OperationList ops = params.toOperationList();
        ops.getOptions().putAll(getRequest().getReference().getQuery().toMap());
        return ops;
    }

    /**
     * @param limitToFormats Set of OutputFormats to limit the result to.
     * @return The best output format based on the URI extension, {@literal
     *         Accept} header, or default, as outlined by the Image API 1.1
     *         spec.
     */
    private Format getEffectiveOutputFormat(Set<Format> limitToFormats) {
        // Check for a format extension in the URI.
        final String extension = getRequest().getReference().getPathExtension();

        Format format = null;
        if (extension != null) {
            format = Format.getAllFormats()
                    .stream()
                    .filter(f -> f.getPreferredExtension().equals(extension))
                    .findFirst()
                    .orElse(null);
        }
        if (format == null) { // if none, check the Accept header.
            String contentType = negotiateContentType(limitToFormats.stream()
                    .map(Format::getPreferredMediaType)
                    .map(MediaType::toString)
                    .collect(Collectors.toList()));
            if (contentType != null) {
                format = new MediaType(contentType).toFormat();
            } else {
                format = DEFAULT_FORMAT;
            }
        }
        return format;
    }

    private boolean isResolvingFirst() {
        return Configuration.getInstance().
                getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true);
    }

}
