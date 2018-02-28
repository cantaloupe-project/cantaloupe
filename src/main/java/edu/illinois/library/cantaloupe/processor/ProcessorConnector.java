package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.cache.CacheDisabledException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.PathStreamSource;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Establishes the best connection between a {@link Resolver} and a
 * {@link Processor}.
 */
public final class ProcessorConnector {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProcessorConnector.class);

    /**
     * @return Strategy from the application configuration, or a default.
     */
    static RetrievalStrategy getFallbackRetrievalStrategy() {
        RetrievalStrategy s = RetrievalStrategy.from(
                Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY);
        return (s != null) ? s : RetrievalStrategy.DOWNLOAD;
    }

    /**
     * @return Strategy from the application configuration, or a default.
     */
    static RetrievalStrategy getStreamProcessorRetrievalStrategy() {
        RetrievalStrategy s = RetrievalStrategy.from(
                Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY);
        return (s != null) ? s : RetrievalStrategy.STREAM;
    }

    private static Path getTempFile(Format sourceFormat) {
        return Application.getTempPath().resolve(
                ProcessorConnector.class.getSimpleName() + "-" +
                        RetrievalStrategy.DOWNLOAD + "-" +
                        UUID.randomUUID() + "." +
                        sourceFormat.getPreferredExtension());
    }

    /**
     * <p>Establishes the best (most efficient &amp; compatible) connection
     * between a resolver and a processor according to the application
     * configuration.</p>
     *
     * <ul>
     *     <li>If the resolver is a {@link FileResolver}, the processor will
     *     read from either a {@link Path} or a {@link StreamSource}.</li>
     *     <li>If the resolver is <em>only</em> a {@link StreamResolver} and
     *     the processor is <em>only</em> a {@link StreamProcessor}:
     *         <ul>
     *             <li>If {@link Key#PROCESSOR_STREAM_RETRIEVAL_STRATEGY} is
     *             set to {@link RetrievalStrategy#STREAM}, the processor will
     *             read from the {@link StreamSource} provided by the
     *             resolver.</li>
     *             <li>If it is set to {@link RetrievalStrategy#DOWNLOAD}, the
     *             source image will be downloaded to a temp file, and the
     *             processor will read that.</li>
     *             <li>If it is set to {@link RetrievalStrategy#CACHE}, the
     *             source image will be downloaded to the source cache, and the
     *             processor will read the file returned by {@link
     *             SourceCache#getSourceImageFile(Identifier)}. This will
     *             block all threads that are calling with the same argument,
     *             forcing them to wait for it to download.</li>
     *         </ul>
     *     </li>
     *     <li>If the resolver is <em>only</em> a {@link StreamResolver} and
     *     the processor is <em>only</em> a {@link FileProcessor}:
     *         <ul>
     *             <li>If {@link Key#PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY} is
     *             set to {@link RetrievalStrategy#CACHE}:
     *                 <ul>
     *                     <li>If the source cache is enabled, the source image
     *                     will be downloaded to it, and the processor will
     *                     read the file returned by {@link
     *                     SourceCache#getSourceImageFile(Identifier)}. This
     *                     will block all threads that are calling with the
     *                     same argument, forcing them to wait for it to
     *                     download.</li>
     *                     <li>Otherwise, a {@link CacheDisabledException} will
     *                     be thrown.</li>
     *                 </ul>
     *             </li>
     *             <li>If it is set to {@link RetrievalStrategy#DOWNLOAD}, the
     *             source image will be downloaded to a temp file, and the
     *             processor will read that.</li>
     *             <li>Otherwise, an {@link IncompatibleResolverException}
     *             will be thrown.</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * <p>The processor is guaranteed to have its source set.</p>
     *
     * @param resolver   Resolver to connect.
     * @param processor  Processor to connect.
     * @param identifier Identifier of the source image.
     * @return           Instance representing a download in progress. The
     *                   client should delete the corresponding file when it is
     *                   no longer needed. Will be non-{@literal null} only if
     *                   the current retrieval strategy is {@link
     *                   RetrievalStrategy#DOWNLOAD}.
     */
    public Future<Path> connect(Resolver resolver,
                                Processor processor,
                                Identifier identifier,
                                Format sourceFormat) throws IOException,
            CacheDisabledException, IncompatibleResolverException,
            InterruptedException {
        final String resolverName = resolver.getClass().getSimpleName();
        final String processorName = processor.getClass().getSimpleName();

        if (resolver instanceof FileResolver) {
            if (processor instanceof FileProcessor) {
                LOGGER.info("{} -> {} connection between {} and {}",
                        FileResolver.class.getSimpleName(),
                        FileProcessor.class.getSimpleName(),
                        resolverName,
                        processorName);
                ((FileProcessor) processor).setSourceFile(
                        ((FileResolver) resolver).getPath());
            } else {
                // All FileResolvers are also StreamResolvers.
                LOGGER.info("{} -> {} connection between {} and {}",
                        FileResolver.class.getSimpleName(),
                        StreamProcessor.class.getSimpleName(),
                        resolverName,
                        processorName);
                ((StreamProcessor) processor).setStreamSource(
                        ((StreamResolver) resolver).newStreamSource());
            }
        } else {
            // The resolver is a StreamResolver.
            StreamSource streamSource =
                    ((StreamResolver) resolver).newStreamSource();

            // StreamResolvers and FileProcessors can't work together using
            // StreamStrategy, but they can using one of the other strategies.
            if (!(processor instanceof StreamProcessor)) {
                switch (getFallbackRetrievalStrategy()) {
                    case DOWNLOAD:
                        LOGGER.info("Using {} to work around the " +
                                        "incompatibility of {} (a {}) and {} (a {})",
                                RetrievalStrategy.DOWNLOAD,
                                resolver.getClass().getSimpleName(),
                                StreamResolver.class.getSimpleName(),
                                processor.getClass().getSimpleName(),
                                FileProcessor.class.getSimpleName());

                        TempFileDownload dl = new TempFileDownload(
                                streamSource, getTempFile(sourceFormat));
                        dl.downloadSync();
                        try {
                            ((FileProcessor) processor).setSourceFile(dl.get());
                        } catch (InterruptedException e) {
                            throw new IOException(e);
                        }
                        return dl;
                    case CACHE:
                        SourceCache sourceCache = CacheFactory.getSourceCache();
                        if (sourceCache != null) {
                            LOGGER.info("Using {} to work around the " +
                                            "incompatibility of {} (a {}) and {} (a {})",
                                    RetrievalStrategy.CACHE,
                                    resolver.getClass().getSimpleName(),
                                    StreamResolver.class.getSimpleName(),
                                    processor.getClass().getSimpleName(),
                                    FileProcessor.class.getSimpleName());

                            Path file = downloadToSourceCache(
                                    streamSource, sourceCache, identifier);
                            connect(sourceCache, file, processor);
                        } else {
                            throw new CacheDisabledException(
                                    "The source cache is not available.");
                        }
                        break;
                    default:
                        throw new IncompatibleResolverException(resolver, processor);
                }
            } else {
                switch (getStreamProcessorRetrievalStrategy()) {
                    case DOWNLOAD:
                        LOGGER.info("Using {} with {} as a {}",
                                RetrievalStrategy.DOWNLOAD,
                                processorName,
                                StreamProcessor.class.getSimpleName());

                        TempFileDownload dl = new TempFileDownload(
                                streamSource,
                                getTempFile(sourceFormat));
                        dl.downloadSync();
                        StreamSource tempStreamSource =
                                new PathStreamSource(dl.get());
                        ((StreamProcessor) processor).setStreamSource(tempStreamSource);
                        return dl;
                    case CACHE:
                        LOGGER.info("Using {} with {} as a {}",
                                RetrievalStrategy.CACHE,
                                processorName,
                                StreamProcessor.class.getSimpleName());

                        SourceCache sourceCache = CacheFactory.getSourceCache();
                        if (sourceCache != null) {
                            Path file = downloadToSourceCache(
                                    streamSource, sourceCache, identifier);
                            connect(sourceCache, file, processor);
                        } else {
                            throw new CacheDisabledException("Source cache is disabled.");
                        }
                        break;
                    default: // stream
                        // All FileResolvers are also StreamResolvers.
                        LOGGER.info("{} -> {} connection between {} and {}",
                                StreamResolver.class.getSimpleName(),
                                StreamProcessor.class.getSimpleName(),
                                resolverName,
                                processorName);
                        ((StreamProcessor) processor).setStreamSource(streamSource);
                        break;
                }
            }
        }
        return null;
    }

    /**
     * Acquires the source image with the given identifier from the given
     * source cache, downloading it if necessary, and configures the given
     * processor to read it.
     *
     * @param streamSource Source of streams from which to read the source
     *                     image,, if necessary.
     * @param sourceCache  Source cache from which to read the source image,
     *                     and to which to download it, if necessary.
     * @param identifier   Identifier of the source image.
     * @return             Path of the downloaded image within the source
     *                     cache.
     */
    private Path downloadToSourceCache(StreamSource streamSource,
                                       SourceCache sourceCache,
                                       Identifier identifier) throws IOException {
        SourceCacheDownload dl = new SourceCacheDownload(
                streamSource, sourceCache, identifier);
        dl.downloadSync();
        try {
            return dl.get();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    /**
     * Acquires the source image with the given identifier from the given
     * source cache, downloading it if necessary, and configures the given
     * processor to read it.
     *
     * @param sourceCache     Source cache from which to read the source image,
     *                        and to which to download it, if necessary.
     * @param sourceCacheFile Source of streams to read from.
     * @param processor       Processor to configure.
     */
    private void connect(SourceCache sourceCache,
                         Path sourceCacheFile,
                         Processor processor) {
        LOGGER.info("{} -> {} connection between {} and {}",
                SourceCache.class.getSimpleName(),
                FileProcessor.class.getSimpleName(),
                sourceCache.getClass().getSimpleName(),
                processor.getClass().getSimpleName());
        if (processor instanceof FileProcessor) {
            ((FileProcessor) processor).setSourceFile(sourceCacheFile);
        } else {
            StreamSource streamSource = new PathStreamSource(sourceCacheFile);
            ((StreamProcessor) processor).setStreamSource(streamSource);
        }
    }

}
