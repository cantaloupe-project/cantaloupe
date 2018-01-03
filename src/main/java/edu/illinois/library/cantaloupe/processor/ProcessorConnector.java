package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.cache.CacheDisabledException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Establishes the best connection between a processor and a resolver.
 */
public class ProcessorConnector {

    enum StreamProcessorRetrievalStrategy {
        STREAM, CACHE
    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ProcessorConnector.class);

    public static StreamProcessorRetrievalStrategy
    getStreamProcessorRetrievalStrategy() {
        return Configuration.getInstance().getString(
                Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                "StreamStrategy").equals("StreamStrategy") ?
                StreamProcessorRetrievalStrategy.STREAM :
                StreamProcessorRetrievalStrategy.CACHE;
    }

    /**
     * <p>Establishes the best (most efficient & compatible) connection between
     * a processor and resolver.</p>
     *
     * <ul>
     *     <li>If the resolver is a {@link FileResolver}, the processor will
     *     read from either a {@link Path} or a {@link StreamSource}.</li>
     *     <li>If the resolver is <em>only</em> a {@link StreamResolver} and
     *     the processor is <em>only</em> a {@link StreamProcessor}:
     *         <ul>
     *             <li>If {@link Key#STREAMPROCESSOR_RETRIEVAL_STRATEGY}
     *             is set to {@literal StreamStrategy}, the processor will
     *             read from the {@link StreamSource} provided by the
     *             resolver.</li>
     *             <li>If {@link Key#STREAMPROCESSOR_RETRIEVAL_STRATEGY}
     *             is set to {@literal CacheStrategy}, the source image will
     *             be downloaded to the source cache, and the processor will
     *             read the file returned by
     *             {@link SourceCache#getSourceImageFile(Identifier)}. This will
     *             block, so other threads trying to access the same source
     *             image will have to wait for it to download.</li>
     *         </ul>
     *     </li>
     *     <li>If the resolver is <em>only</em> a {@link StreamResolver} and
     *     the processor is <em>only</em> a {@link FileProcessor}:
     *         <ul>
     *             <li>If {@link Key#SOURCE_CACHE_ENABLED} is {@literal true},
     *             the source image will be downloaded to the source cache, and
     *             the processor will read the file returned by
     *             {@link SourceCache#getSourceImageFile(Identifier)}. This will
     *             block, so other threads trying to access the same source
     *             image will have to wait for it to download.</li>
     *             <li>Otherwise, an {@link IncompatibleResolverException}
     *             will be thrown.</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * <p>The processor is guaranteed to have its source set.</p>
     */
    public void connect(Resolver resolver,
                        Processor processor,
                        Identifier identifier) throws IOException,
            CacheDisabledException, IncompatibleResolverException {
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
        } else { // resolver is a StreamResolver
            // StreamResolvers and FileProcessors can't work together.
            if (!(processor instanceof StreamProcessor)) {
                LOGGER.info("Resolver and processor are incompatible ({} -> {})",
                        StreamResolver.class.getSimpleName(),
                        FileProcessor.class.getSimpleName());
                SourceCache sourceCache = CacheFactory.getSourceCache();
                if (sourceCache != null) {
                    LOGGER.debug("Source cache available.");
                    setSourceCacheAsSource(resolver, processor, sourceCache,
                            identifier);
                } else {
                    throw new IncompatibleResolverException(resolver, processor);
                }
            } else {
                switch (getStreamProcessorRetrievalStrategy()) {
                    case CACHE:
                        LOGGER.info("Using CacheStrategy with {} as a {}",
                                processorName,
                                StreamProcessor.class.getSimpleName());
                        SourceCache sourceCache = CacheFactory.getSourceCache();
                        if (sourceCache != null) {
                            LOGGER.info("Source cache available.");
                            setSourceCacheAsSource(resolver, processor,
                                    sourceCache, identifier);
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
                        ((StreamProcessor) processor).setStreamSource(
                                ((StreamResolver) resolver).newStreamSource());
                        break;
                }
            }
        }
    }

    private void setSourceCacheAsSource(Resolver resolver,
                                        Processor processor,
                                        SourceCache sourceCache,
                                        Identifier identifier) throws IOException {
        // This will block while a file is being written in another thread,
        // which will prevent the image from being downloaded multiple times.
        Path sourceFile = sourceCache.getSourceImageFile(identifier);
        if (sourceFile == null) {
            downloadToSourceCache(resolver, sourceCache, identifier);
            sourceFile = sourceCache.getSourceImageFile(identifier);
        }

        LOGGER.info("{} -> {} connection between {} and {}",
                SourceCache.class.getSimpleName(),
                FileProcessor.class.getSimpleName(),
                sourceCache.getClass().getSimpleName(),
                processor.getClass().getSimpleName());
        if (processor instanceof FileProcessor) {
            ((FileProcessor) processor).setSourceFile(sourceFile);
        } else {
            InputStream inputStream = Files.newInputStream(sourceFile);
            StreamSource streamSource = new InputStreamStreamSource(inputStream);
            ((StreamProcessor) processor).setStreamSource(streamSource);
        }
    }

    private void downloadToSourceCache(Resolver resolver,
                                       SourceCache sourceCache,
                                       Identifier identifier) throws IOException {
        // Download to the SourceCache and then read from it.
        try (InputStream inputStream =
                     ((StreamResolver) resolver).newStreamSource().newInputStream();
             OutputStream outputStream =
                     sourceCache.newSourceImageOutputStream(identifier)) {
            LOGGER.info("Downloading {} to {}",
                    identifier,
                    SourceCache.class.getSimpleName());
            IOUtils.copy(inputStream, outputStream);
        }
    }

}
