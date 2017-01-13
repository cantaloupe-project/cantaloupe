package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.cache.SourceCacheDisabledException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.IncompatibleResolverException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Establishes the best connection between a processor and a resolver.
 */
public class SourceImageWrangler {

    enum StreamProcessorRetrievalStrategy {
        STREAM, CACHE
    }

    private static final Logger logger = LoggerFactory.
            getLogger(SourceImageWrangler.class);

    static final String STREAMPROCESSOR_RETRIEVAL_STRATEGY_CONFIG_KEY =
            "StreamProcessor.retrieval_strategy";

    private Identifier identifier;
    private Processor processor;
    private Resolver resolver;

    public static StreamProcessorRetrievalStrategy
    getStreamProcessorRetrievalStrategy() {
        return ConfigurationFactory.getInstance().getString(
                STREAMPROCESSOR_RETRIEVAL_STRATEGY_CONFIG_KEY,
                "StreamStrategy").equals("StreamStrategy") ?
                StreamProcessorRetrievalStrategy.STREAM :
                StreamProcessorRetrievalStrategy.CACHE;
    }

    public SourceImageWrangler(Resolver resolver,
                               Processor processor,
                               Identifier identifier) {
        this.resolver = resolver;
        this.processor = processor;
        this.identifier = identifier;
    }

    /**
     * <p>Establishes the best connection between the processor and the
     * resolver.</p>
     *
     * <ul>
     *     <li>If the resolver is a {@link FileResolver}, the processor will
     *     read from either a {@link File} or a {@link StreamSource}.</li>
     *     <li>If the resolver is a {@link StreamResolver} (only) and the
     *     processor is a {@link StreamProcessor} (only):
     *         <ul>
     *             <li>If {@link #STREAMPROCESSOR_RETRIEVAL_STRATEGY_CONFIG_KEY}
     *             is set to <code>StreamStrategy</code>, the processor will
     *             read from the {@link StreamSource} provided by the
     *             resolver.</li>
     *             <li>If {@link #STREAMPROCESSOR_RETRIEVAL_STRATEGY_CONFIG_KEY}
     *             is set to <code>CacheStrategy</code>, the source image will
     *             be downloaded to the source cache, and the processor will
     *             read the file returned by
     *             {@link SourceCache#getSourceImageFile(Identifier)}. This will
     *             block, and other threads trying to access the same source
     *             image will wait for it to download.</li>
     *         </ul>
     *     </li>
     *     <li>If the resolver is a {@link StreamResolver} (only) and the
     *     processor is a {@link FileProcessor} (only):
     *         <ul>
     *             <li>If {@link CacheFactory#SOURCE_CACHE_CONFIG_KEY} is set,
     *             the source image will be downloaded to the source cache,
     *             and the processor will read the file returned by
     *             {@link SourceCache#getSourceImageFile(Identifier)}. This will
     *             block, and other threads trying to access the same source
     *             image will wait for it to download.</li>
     *             <li>Otherwise, an {@link IncompatibleResolverException}
     *             will be thrown.</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * <p>When this method completes, the processor is guaranteed to have its
     * source set.</p>
     *
     * @throws IOException
     * @throws CacheException
     * @throws SourceCacheDisabledException
     * @throws IncompatibleResolverException
     */
    public void wrangle() throws IOException, CacheException,
            IncompatibleResolverException {
        final String resolverName = resolver.getClass().getSimpleName();
        final String processorName = processor.getClass().getSimpleName();

        if (resolver instanceof FileResolver) {
            if (processor instanceof FileProcessor) {
                logger.info("FileResolver -> FileProcessor connection " +
                        "between {} and {}",
                        resolverName, processorName);
                ((FileProcessor) processor).setSourceFile(
                        ((FileResolver) resolver).getFile());
            } else {
                // All FileResolvers are also StreamResolvers.
                logger.info("FileResolver -> StreamProcessor connection " +
                        "between {} and {}",
                        resolverName, processorName);
                ((StreamProcessor) processor).setStreamSource(
                        ((StreamResolver) resolver).newStreamSource());
            }
        } else { // resolver is a StreamResolver
            // StreamResolvers and FileProcessors can't work together.
            if (!(processor instanceof StreamProcessor)) {
                logger.info("Incompatible resolver and processor " +
                        "(StreamResolver -> FileProcessor).");
                SourceCache sourceCache = CacheFactory.getSourceCache();
                if (sourceCache != null) {
                    logger.debug("Source cache available.");
                    setSourceCacheAsSource(sourceCache);
                } else {
                    throw new IncompatibleResolverException(resolver, processor);
                }
            } else {
                switch (getStreamProcessorRetrievalStrategy()) {
                    case CACHE:
                        logger.info("Using CacheStrategy with {} as a StreamProcessor",
                                processorName);
                        SourceCache sourceCache = CacheFactory.getSourceCache();
                        if (sourceCache != null) {
                            logger.info("Source cache available.");
                            setSourceCacheAsSource(sourceCache);
                        } else {
                            throw new SourceCacheDisabledException();
                        }
                        break;
                    default: // stream
                        // All FileResolvers are also StreamResolvers.
                        logger.info("StreamResolver -> StreamProcessor " +
                                "connection between {} and {}",
                                resolverName, processorName);
                        ((StreamProcessor) processor).setStreamSource(
                                ((StreamResolver) resolver).newStreamSource());
                        break;
                }
            }
        }
    }

    private void setSourceCacheAsSource(SourceCache sourceCache)
            throws IOException, CacheException {
        // This will block while a file is being written in another thread,
        // which will prevent the image from being downloaded multiple times.
        File sourceFile = sourceCache.getSourceImageFile(identifier);
        if (sourceFile == null) {
            downloadToSourceCache(sourceCache);
            sourceFile = sourceCache.getSourceImageFile(identifier);
        }
        logger.info("SourceCache -> FileProcessor connection between {} and {}",
                sourceCache.getClass().getSimpleName(),
                processor.getClass().getSimpleName());
        if (processor instanceof FileProcessor) {
            ((FileProcessor) processor).setSourceFile(sourceFile);
        } else {
            InputStream inputStream = new FileInputStream(sourceFile);
            StreamSource streamSource = new InputStreamStreamSource(inputStream);
            ((StreamProcessor) processor).setStreamSource(streamSource);
        }
    }

    private void downloadToSourceCache(SourceCache sourceCache)
            throws IOException, CacheException {
        // Download to the SourceCache and then read from it.
        try (InputStream inputStream = ((StreamResolver) resolver).
                newStreamSource().newInputStream();
             OutputStream outputStream =
                     sourceCache.getImageOutputStream(identifier)) {
            logger.info("Downloading {} to the source cache", identifier);
            IOUtils.copy(inputStream, outputStream);
        }
    }

}
