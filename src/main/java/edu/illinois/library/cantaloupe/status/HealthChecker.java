package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * <p>Checks various aspects of the application to verify that they are
 * functioning correctly:</p>
 *
 * <dl>
 *     <dt>Processing I/O</dt>
 *     <dd>When an image endpoint successfully completes a request, it calls
 *     {@link #addSourceProcessorPair(Source, Processor, OperationList)} to
 *     register the various objects it used to do so. This class keeps track of
 *     every unique source-processor pair and tests each one against the last
 *     known image it worked with.</dd>
 *     <dt>The source cache</dt>
 *     <dd>An image is written to the source cache (if available) and read
 *     back.</dd>
 *     <dt>The derivative cache</dt>
 *     <dd>An image is written to the derivative cache (if available) and read
 *     back.</dd>
 * </dl>
 *
 * @author Alex Dolski UIUC
 * @since 4.1
 */
public final class HealthChecker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HealthChecker.class);

    private static final Set<SourceProcessorPair> SOURCE_PROCESSOR_PAIRS =
            ConcurrentHashMap.newKeySet();

    /**
     * <p>Informs the class of a {@link Source}-{@link Processor} pair that has
     * been used successfully, and could be used again in the course of a
     * health check. Should be called by image processing endpoints after
     * processing has completed successfully.</p>
     *
     * <p>This method is thread-safe.</p>
     */
    public static void addSourceProcessorPair(Source source,
                                              Processor processor,
                                              OperationList opList) {
        final SourceProcessorPair pair = new SourceProcessorPair(
                source, processor.getClass().getName(), opList);
        // The pair is configured to read a specific image. We want to remove
        // any older "equal" (see this ivar's equals() method!) instance before
        // adding the current one because the older one is more likely to be
        // stale (no longer accessible), in light of the possibility that the
        // application has been running for a while.
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
     * <p>Checks the functionality of every {@link #addSourceProcessorPair(
     * Source, Processor, OperationList) known} source-processor pair. The
     * checks exercise the full length of the processing pipeline, reading an
     * image from a {@link Source}, running it through a {@link Processor}, and
     * writing it to an output stream.</p>
     *
     * <p>The individual checks are done concurrently in as many threads as
     * there are unique pairs. This is intended to improve responsiveness,
     * assuming there aren't a whole lot more pairs than there are CPU
     * threads.</p>
     */
    private static synchronized void checkProcessing(Health health) {
        // Make a local copy to ensure that another thread doesn't change it
        // underneath us.
        final Set<SourceProcessorPair> localPairs =
                new HashSet<>(SOURCE_PROCESSOR_PAIRS);
        final int numPairs = localPairs.size();

        LOGGER.trace("{} unique source/processor combinations.", numPairs);

        final CountDownLatch latch = new CountDownLatch(numPairs);
        localPairs.forEach(pair -> {
            ThreadPool.getInstance().submit(() -> {
                LOGGER.debug("Exercising processing I/O: {}", pair);

                Future<Path> tempFileFuture = null;
                final ProcessorFactory pf = new ProcessorFactory();
                final Source source = pair.getSource();

                final Iterator<Format> formatIterator = source.getFormatIterator();

                // This should never be the case, but is a basic sanity check.
                if (!formatIterator.hasNext()) {
                    health.setMinColor(Health.Color.RED);
                    health.setMessage("Format iterator returned an empty " +
                            "iterator. This is probably a bug.");
                    latch.countDown();
                }

                while (formatIterator.hasNext()) {
                    final Format format = formatIterator.next();
                    try (Processor processor = pf.newProcessor(pair.getProcessorName());
                         OutputStream os = OutputStream.nullOutputStream()) {
                        processor.setSourceFormat(format);

                        tempFileFuture = new ProcessorConnector().connect(
                                source,
                                processor,
                                source.getIdentifier(),
                                format);
                        Info info = processor.readInfo();
                        processor.process(pair.getOperationList(), info, os);
                    } catch (SourceFormatException e) {
                        LOGGER.debug("checkProcessing(): {}", e.getMessage());
                    } catch (Throwable t) {
                        health.setMinColor(Health.Color.RED);
                        health.setMessage(String.format("%s (%s)",
                                t.getMessage(), pair));
                    } finally {
                        latch.countDown();
                        if (tempFileFuture != null) {
                            try {
                                Path tempFile = tempFileFuture.get();
                                if (tempFile != null) {
                                    Files.deleteIfExists(tempFile);
                                }
                            } catch (Exception e) {
                                LOGGER.error("checkProcessing(): failed to delete temp file: {}",
                                        e.getMessage(), e);
                            }
                        }
                    }
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
        final Optional<SourceCache> optSrcCache = cacheFacade.getSourceCache();
        if (optSrcCache.isPresent()) {
            final SourceCache srcCache = optSrcCache.get();
            LOGGER.debug("Exercising the source cache: {}", srcCache);
            final Identifier identifier =
                    new Identifier("HealthCheck-" + UUID.randomUUID());
            try {
                // Exercise the cache. Errors will throw exceptions.
                srcCache.purge(identifier);
                // Write a file to disk.
                try (OutputStream os = srcCache.newSourceImageOutputStream(identifier)) {
                    String message = String.format("This file written by %s",
                            HealthChecker.class.getName());
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    os.write(data);
                    os.flush();
                }
                // Read it back.
                Optional<Path> path = srcCache.getSourceImageFile(identifier);
                Files.readAllBytes(path.orElseThrow());
            } catch (Throwable t) {
                health.setMinColor(Health.Color.RED);
                String message = String.format("%s: %s",
                        srcCache.getClass().getSimpleName(),
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
        final Optional<DerivativeCache> optDerivativeCache =
                cacheFacade.getDerivativeCache();
        if (optDerivativeCache.isPresent()) {
            DerivativeCache dCache = optDerivativeCache.get();
            LOGGER.debug("Exercising the derivative cache: {}", dCache);
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

    /**
     * <p>Performs a health check as explained in the class documentation.
     * Each group of checks (derivative cache, processor I/O, etc.) is
     * performed sequentially. If any check fails, all remaining checks are
     * skipped.</p>
     *
     * <p>N.B.: there could be benefits in terms of responsiveness to doing all
     * of the checks asynchronously and returning a result when they've all
     * completed; but this could also result in having to do more checks than
     * necessary, since, if a single check fails, none of the others
     * matter.</p>
     *
     * <p>This method is thread-safe and can be called repeatedly to obtain the
     * current health.</p>
     *
     * @return Instance reflecting the application health.
     */
    public Health check() {
        LOGGER.debug("Initiating a health check");

        final Stopwatch watch = new Stopwatch();
        final Health health   = new Health();

        // Check processing I/O.
        if (!Health.Color.RED.equals(health.getColor())) {
            checkProcessing(health);
            LOGGER.trace("Processing I/O check completed in {}; health so far is {}",
                    watch, health.getColor());
        }

        // Check the source cache.
        if (!Health.Color.RED.equals(health.getColor())) {
            checkSourceCache(health);
            LOGGER.trace("Source cache check completed in {}; health so far is {}",
                    watch, health.getColor());
        }

        // Check the derivative cache.
        if (!Health.Color.RED.equals(health.getColor())) {
            checkDerivativeCache(health);
            LOGGER.trace("Derivative cache check completed in {}; health so far is {}",
                    watch, health.getColor());
        }

        // Log the final status.
        if (Health.Color.GREEN.equals(health.getColor())) {
            LOGGER.debug("Health check completed in {}: {}", watch, health);
        } else {
            LOGGER.warn("Health check completed in {}: {}", watch, health);
        }
        return health;
    }

}
