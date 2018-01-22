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
 * Establishes the best connection between a {@link Resolver} and a
 * {@link Processor}.
 */
public final class ProcessorConnector {

    /**
     * Strategy used by {@link StreamProcessor}s to read content.
     */
    enum StreamProcessorRetrievalStrategy {

        /**
         * Content is streamed into a reader from its source. When multiple
         * threads need to read the same image simultaneously, all will stream
         * it from the source simultaneously.
         */
        STREAM("StreamStrategy"),

        /**
         * Source content is downloaded into a {@link SourceCache} before
         * being read. When multiple threads need to read the same image and it
         * has not yet been cached, they will wait (on a monitor) for the image
         * to download.
         */
        CACHE("CacheStrategy");

        private final String configValue;

        StreamProcessorRetrievalStrategy(String configValue) {
            this.configValue = configValue;
        }

        /**
         * @return Representative string value of the strategy in the
         *         application configuration.
         */
        public String getConfigValue() {
            return configValue;
        }

        @Override
        public String toString() {
            return getConfigValue();
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProcessorConnector.class);

    /**
     * Maximum number of a times a source image download will be attempted.
     */
    private static final short MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS = 2;

    /**
     * @return Strategy from the application configuration, or a default.
     */
    public static StreamProcessorRetrievalStrategy
    getStreamProcessorRetrievalStrategy() {
        final String configValue = Configuration.getInstance().getString(
                Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY);
        return StreamProcessorRetrievalStrategy.CACHE.getConfigValue().equals(configValue) ?
                StreamProcessorRetrievalStrategy.CACHE :
                StreamProcessorRetrievalStrategy.STREAM;
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
     *             <li>If using {@link
     *             StreamProcessorRetrievalStrategy#STREAM}, the processor will
     *             read from the {@link StreamSource} provided by the
     *             resolver.</li>
     *             <li>If using {@link
     *             StreamProcessorRetrievalStrategy#CACHE}, the source image
     *             will be downloaded to the source cache, and the processor
     *             will read the file returned by {@link
     *             SourceCache#getSourceImageFile(Identifier)}. This will
     *             block all threads that are calling with the same argument,
     *             forcing them to wait for it to download.</li>
     *         </ul>
     *     </li>
     *     <li>If the resolver is <em>only</em> a {@link StreamResolver} and
     *     the processor is <em>only</em> a {@link FileProcessor}:
     *         <ul>
     *             <li>If {@link Key#SOURCE_CACHE_ENABLED} is set to {@literal
     *             true}, the source image will be downloaded to the source
     *             cache, and the processor will read the file returned by
     *             {@link SourceCache#getSourceImageFile(Identifier)}. This
     *             will block all threads that are calling with the same
     *             argument, forcing them to wait for it to download.</li>
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
                        LOGGER.info("Using {} with {} as a {}",
                                StreamProcessorRetrievalStrategy.CACHE,
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

    /**
     * Acquires the source image with the given identifier from the given
     * source cache, downloading it if necessary, and configures the given
     * processor to read it. Up to
     * {@link #MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS} attempts are made.
     *
     * @param resolver     Resolver to read the source image from, if necessary.
     * @param processor    Processor to configure.
     * @param sourceCache  Source cache from which to read the source image,
     *                     and to which to download it, if necessary.
     * @param identifier   Identifier of the source image.
     * @throws IOException if anything goes wrong on the final attempt.
     */
    private void setSourceCacheAsSource(Resolver resolver,
                                        Processor processor,
                                        SourceCache sourceCache,
                                        Identifier identifier) throws IOException {
        boolean succeeded = false;
        short numAttempts = 0;
        do {
            numAttempts++;
            try {
                // This will block while a file is being written in another
                // thread, which will prevent the image from being downloaded
                // multiple times, and maybe enable other threads to get the
                // image sooner.
                // If it throws an exception, we will log it and retry a few
                // times, and only rethrow it on the last try.
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
                succeeded = true;
            } catch (IOException e) {
                LOGGER.error("setSourceCacheAsSource(): {} (attempt {} of {})",
                        e.getMessage(),
                        numAttempts,
                        MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS);
                if (numAttempts == MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS) {
                    throw e;
                }
            }
        } while (!succeeded &&
                numAttempts < MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS);
    }

    /**
     * Downloads the source image with the given identifier from the given
     * resolver to the given source cache.
     *
     * @param resolver     Resolver to read from.
     * @param sourceCache  Source cache to write to.
     * @param identifier   Identifier of the source image.
     * @throws IOException if anything goes wrong.
     */
    private void downloadToSourceCache(Resolver resolver,
                                       SourceCache sourceCache,
                                       Identifier identifier) throws IOException {
        final StreamSource source = ((StreamResolver) resolver).newStreamSource();
        try (InputStream is = source.newInputStream();
             OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            LOGGER.info("Downloading {} to {}",
                    identifier,
                    SourceCache.class.getSimpleName());
            IOUtils.copy(is, os);
        }
    }

}
