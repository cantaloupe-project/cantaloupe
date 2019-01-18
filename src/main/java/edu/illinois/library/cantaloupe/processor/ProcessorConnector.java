package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.cache.CacheDisabledException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.source.FileSource;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.source.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Establishes an optimal connection between a {@link Source} and a
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
     * <p>Establishes the best (most efficient and compatible) connection
     * between a source and a processor.</p>
     *
     * <ul>
     *     <li>If the source is a {@link FileSource}, the processor will read
     *     from either a {@link Path} or a {@link StreamFactory}.</li>
     *     <li>If the source is <em>only</em> a {@link StreamSource}:
     *         <ul>
     *             <li>If the processor is a {@link StreamProcessor}:
     *                 <ul>
     *                     <li>If the source is configured to support seeking,
     *                     and its seeking support is {@link
     *                     StreamFactory#isSeekingDirect() direct}, the
     *                     processor will read from a {@link
     *                     StreamFactory#newSeekableStream() seekable stream
     *                     provided by the source}.</li>
     *                     <li>If {@link
     *                     Key#PROCESSOR_STREAM_RETRIEVAL_STRATEGY} is set to
     *                     {@link RetrievalStrategy#STREAM}, the processor will
     *                     read from {@link StreamFactory one of the streams
     *                     provided by the source}.</li>
     *                     <li>If it is set to {@link
     *                     RetrievalStrategy#DOWNLOAD}, the source image will
     *                     be downloaded to a temp file, and the processor will
     *                     read that.</li>
     *                     <li>If it is set to {@link RetrievalStrategy#CACHE},
     *                     the source image will be downloaded to the source
     *                     cache, and the processor will read the file returned
     *                     by {@link
     *                     SourceCache#getSourceImageFile(Identifier)}. This
     *                     will block all threads that are calling with the
     *                     same argument, forcing them to wait for it to
     *                     download.</li>
     *                 </ul>
     *             </li>
     *             <li>If the processor is <em>only</em> a {@link
     *             FileProcessor}:
     *                 <ul>
     *                     <li>If {@link
     *                     Key#PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY} is set to
     *                     {@link RetrievalStrategy#CACHE}:
     *                         <ul>
     *                             <li>If the source cache is enabled, the
     *                             source image will be downloaded to it, and
     *                             the processor will read the file returned by
     *                             {@link
     *                             SourceCache#getSourceImageFile(Identifier)}.
     *                             This will block all threads that are calling
     *                             with the same argument, forcing them to wait
     *                             for it to download.</li>
     *                             <li>Otherwise, a {@link
     *                             CacheDisabledException} will be thrown.</li>
     *                         </ul>
     *                     </li>
     *                     <li>If it is set to {@link
     *                     RetrievalStrategy#DOWNLOAD}, the source image will
     *                     be downloaded to a temp file, and the processor will
     *                     read that.</li>
     *                     <li>Otherwise, an {@link IncompatibleSourceException}
     *                     will be thrown.</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * <p>The processor is guaranteed to have its source set.</p>
     *
     * @param source     Source to connect to the processor.
     * @param processor  Processor to connect to the source.
     * @param identifier Identifier of the source image.
     * @return           Instance representing a download in progress. The
     *                   client should delete the corresponding file when it is
     *                   no longer needed. Will be non-{@literal null} only if
     *                   the current retrieval strategy is {@link
     *                   RetrievalStrategy#DOWNLOAD}.
     */
    public Future<Path> connect(Source source,
                                Processor processor,
                                Identifier identifier,
                                Format sourceFormat) throws IOException,
            CacheDisabledException, IncompatibleSourceException,
            InterruptedException {
        final String sourceName    = source.getClass().getSimpleName();
        final String processorName = processor.getClass().getSimpleName();

        if (source instanceof FileSource) {
            if (processor instanceof FileProcessor) {
                LOGGER.debug("{} -> {} connection between {} and {}",
                        FileSource.class.getSimpleName(),
                        FileProcessor.class.getSimpleName(),
                        sourceName,
                        processorName);
                ((FileProcessor) processor).setSourceFile(
                        ((FileSource) source).getPath());
            } else {
                // All FileSources are also StreamSources.
                LOGGER.debug("{} -> {} connection between {} and {}",
                        FileSource.class.getSimpleName(),
                        StreamProcessor.class.getSimpleName(),
                        sourceName,
                        processorName);
                ((StreamProcessor) processor).setStreamFactory(
                        ((StreamSource) source).newStreamFactory());
            }
        } else {
            // The source is a StreamSource.
            StreamFactory streamFactory =
                    ((StreamSource) source).newStreamFactory();

            // StreamSources and FileProcessors can't work together using
            // StreamStrategy, but they can using one of the other strategies.
            if (!(processor instanceof StreamProcessor)) {
                switch (getFallbackRetrievalStrategy()) {
                    case DOWNLOAD:
                        LOGGER.info("Using {} to work around the " +
                                        "incompatibility of {} (a {}) and {} (a {})",
                                RetrievalStrategy.DOWNLOAD,
                                source.getClass().getSimpleName(),
                                StreamSource.class.getSimpleName(),
                                processor.getClass().getSimpleName(),
                                FileProcessor.class.getSimpleName());

                        TempFileDownload dl = new TempFileDownload(
                                streamFactory, getTempFile(sourceFormat));
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
                                    source.getClass().getSimpleName(),
                                    StreamSource.class.getSimpleName(),
                                    processor.getClass().getSimpleName(),
                                    FileProcessor.class.getSimpleName());

                            Path file = downloadToSourceCache(
                                    streamFactory, sourceCache, identifier);
                            connect(sourceCache, file, processor);
                        } else {
                            throw new CacheDisabledException(
                                    "The source cache is not available.");
                        }
                        break;
                    default:
                        throw new IncompatibleSourceException(source, processor);
                }
            } else {
                final RetrievalStrategy strategy =
                        getStreamProcessorRetrievalStrategy();
                if (RetrievalStrategy.STREAM.equals(strategy) ||
                        streamFactory.isSeekingDirect()) {
                    LOGGER.debug("{} -> {} connection between {} and {}",
                            StreamSource.class.getSimpleName(),
                            StreamProcessor.class.getSimpleName(),
                            sourceName,
                            processorName);
                    ((StreamProcessor) processor).setStreamFactory(streamFactory);
                } else if (RetrievalStrategy.DOWNLOAD.equals(strategy)) {
                    LOGGER.debug("Using {} with {} as a {}",
                            RetrievalStrategy.DOWNLOAD,
                            processorName,
                            StreamProcessor.class.getSimpleName());
                    TempFileDownload dl = new TempFileDownload(
                            streamFactory,
                            getTempFile(sourceFormat));
                    dl.downloadSync();
                    StreamFactory tempStreamFactory =
                            new PathStreamFactory(dl.get());
                    ((StreamProcessor) processor).setStreamFactory(tempStreamFactory);
                    return dl;
                } else if (RetrievalStrategy.CACHE.equals(strategy)) {
                    LOGGER.debug("Using {} with {} as a {}",
                            RetrievalStrategy.CACHE,
                            processorName,
                            StreamProcessor.class.getSimpleName());
                    SourceCache sourceCache = CacheFactory.getSourceCache();
                    if (sourceCache != null) {
                        Path file = downloadToSourceCache(
                                streamFactory,
                                sourceCache,
                                identifier);
                        connect(sourceCache, file, processor);
                    } else {
                        throw new CacheDisabledException("Source cache is disabled.");
                    }
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
     * @param streamFactory Source of streams from which to read the source
     *                     image,, if necessary.
     * @param sourceCache  Source cache from which to read the source image,
     *                     and to which to download it, if necessary.
     * @param identifier   Identifier of the source image.
     * @return             Path of the downloaded image within the source
     *                     cache.
     */
    private Path downloadToSourceCache(StreamFactory streamFactory,
                                       SourceCache sourceCache,
                                       Identifier identifier) throws IOException {
        SourceCacheDownload dl = new SourceCacheDownload(
                streamFactory, sourceCache, identifier);
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
        LOGGER.debug("{} -> {} connection between {} and {}",
                SourceCache.class.getSimpleName(),
                FileProcessor.class.getSimpleName(),
                sourceCache.getClass().getSimpleName(),
                processor.getClass().getSimpleName());
        if (processor instanceof FileProcessor) {
            ((FileProcessor) processor).setSourceFile(sourceCacheFile);
        } else {
            StreamFactory streamFactory = new PathStreamFactory(sourceCacheFile);
            ((StreamProcessor) processor).setStreamFactory(streamFactory);
        }
    }

}
