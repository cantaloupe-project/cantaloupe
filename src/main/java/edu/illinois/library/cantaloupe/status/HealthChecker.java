package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Checks various aspects of the application to verify that they are
 * functioning correctly.
 */
public final class HealthChecker {

    /**
     * Combination of a {@link Source} and {@link Processor} with an overridden
     * {@link #equals(Object)} method to support insertion into a {@link Set}.
     */
    private static final class SourceProcessorPair {

        private Source source;
        private Processor processor;

        private SourceProcessorPair(Source source, Processor processor) {
            this.source = source;
            this.processor = processor;
        }

        /**
         * @return {@literal true} if {@link #source} and {@link #processor}
         *         are of the same class.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof SourceProcessorPair) {
                SourceProcessorPair other = (SourceProcessorPair) obj;
                return source.getClass().equals(other.source.getClass()) &&
                        processor.getClass().equals(other.processor.getClass());
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return (source.getClass().getName() +
                    processor.getClass().getName()).hashCode();
        }

        @Override
        public String toString() {
            return String.format("%s -> %s -> %s",
                    source.getIdentifier(),
                    source.getClass().getSimpleName(),
                    processor.getClass().getSimpleName());
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HealthChecker.class);

    private static final Set<SourceProcessorPair> SOURCE_PROCESSOR_PAIRS =
            ConcurrentHashMap.newKeySet();

    /**
     * Makes the class aware of a {@link Source}-{@link Processor} pair that
     * has been used successfully, and could be used again in the course of a
     * health check. Should be called by image processing endpoints after
     * processing has completed successfully.
     */
    public static void addSourceProcessorPair(Source source,
                                              Processor processor) {
        final SourceProcessorPair pair =
                new SourceProcessorPair(source, processor);
        // The pair is configured to read a specific image. We want to remove
        // any older equal pair before adding the current one because the older
        // one is more likely to be stale (no longer accessible), in light of
        // the possibility that the application has been running a while.
        SOURCE_PROCESSOR_PAIRS.remove(pair);
        SOURCE_PROCESSOR_PAIRS.add(pair);
    }

    /**
     * For testing only!
     */
    static Set<SourceProcessorPair> getSourceProcessorPairs() {
        return SOURCE_PROCESSOR_PAIRS;
    }

    /**
     * Checks the functionality of every {@link #addSourceProcessorPair(Source,
     * Processor) known} source-processor pair. This exercises the full length
     * of the processing pipeline, checking whether a single image can be read
     * from a {@link Source} through a {@link Processor} and written to an
     * output stream. {@link
     * edu.illinois.library.cantaloupe.operation.Operation Processing
     * operations} are not applied, as these are expensive and errors there are
     * more likely to be programming- or feature-related, instead of the
     * runtime errors that this class is more concerned with.
     */
    private static synchronized void checkProcessing(Health health) {
        // Make a local copy to ensure that some other thread doesn't change it
        // underneath us.
        final Set<SourceProcessorPair> localPairs =
                new HashSet<>(SOURCE_PROCESSOR_PAIRS);

        // Check in separate threads to improve responsiveness.
        final CountDownLatch latch = new CountDownLatch(localPairs.size());
        localPairs.forEach(pair -> {
            ThreadPool.getInstance().submit(() -> {
                LOGGER.debug("Checking {}", pair);

                try (OutputStream os = OutputStream.nullOutputStream()) {
                    // Encode into the same format as the source so that no
                    // processing is performed.
                    OperationList opList = new OperationList(
                            new Encode(pair.processor.getSourceFormat()));
                    Info info = pair.processor.readInfo();
                    pair.processor.process(opList, info, os);
                } catch (Throwable t) {
                    health.setMinColor(Health.Color.RED);
                    String message = String.format("%s (%s)",
                            t.getMessage(), pair);
                    health.setMessage(message);
                } finally {
                    latch.countDown();
                }
            });
        });

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            health.setMinColor(Health.Color.YELLOW);
            health.setMessage(e.getMessage());
        }
    }

    /**
     * Checks the reading and writing functionality of the source cache.
     */
    private static synchronized void checkSourceCache(Health health) {
        final CacheFacade cacheFacade = new CacheFacade();
        final SourceCache sCache = cacheFacade.getSourceCache();
        if (sCache != null) {
            LOGGER.debug("Checking {}", sCache);
            final Identifier identifier =
                    new Identifier("HealthCheck-" + UUID.randomUUID());
            try {
                // Exercise the cache. Errors will throw exceptions.
                sCache.purge(identifier);
                // Write a file to disk.
                try (OutputStream os = sCache.newSourceImageOutputStream(identifier)) {
                    String message = String.format("This file written by %s",
                            HealthChecker.class.getName());
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    os.write(data);
                    os.flush();
                }
                // Read it back.
                Path path = sCache.getSourceImageFile(identifier);
                Files.readAllBytes(path);
            } catch (Throwable t) {
                health.setMinColor(Health.Color.RED);
                String message = String.format("%s: %s",
                        sCache.getClass().getSimpleName(),
                        t.getMessage());
                health.setMessage(message);
            }
        }
    }

    /**
     * Checks the reading and writing functionality of the source cache.
     */
    private static synchronized void checkDerivativeCache(Health health) {
        final CacheFacade cacheFacade = new CacheFacade();
        final DerivativeCache dCache = cacheFacade.getDerivativeCache();
        if (dCache != null) {
            LOGGER.debug("Checking {}", dCache);

            final Identifier identifier =
                    new Identifier("HealthCheck-" + UUID.randomUUID());
            try {
                // Exercise the cache. Errors will throw exceptions.
                dCache.purge(identifier);
                // Write to the cache.
                dCache.put(identifier, new Info());
                // Read it back.
                dCache.getInfo(identifier);
            } catch (Throwable t) {
                health.setMinColor(Health.Color.RED);
                String message = String.format("%s: %s",
                        dCache.getClass().getSimpleName(),
                        t.getMessage());
                health.setMessage(message);
            }
        }
    }

    public Health check() {
        final Health health = new Health();

        LOGGER.debug("Initiating a health check");
        if (!Health.Color.RED.equals(health.getColor())) {
            checkProcessing(health);
        }
        if (!Health.Color.RED.equals(health.getColor())) {
            checkSourceCache(health);
        }
        if (!Health.Color.RED.equals(health.getColor())) {
            checkDerivativeCache(health);
        }
        LOGGER.debug("Health check complete: {}", health);
        return health;
    }

}
