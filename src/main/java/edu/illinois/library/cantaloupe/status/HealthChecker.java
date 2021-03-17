package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>Checks various aspects of the application to verify that they are
 * functioning correctly:</p>
 *
 * <dl>
 *     <dt>Source I/O</dt>
 *     <dd>When an image endpoint successfully completes a request, it calls
 *     {@link #addSourceUsage(Source)} to register the source it used to do so.
 *     This class tests the I/O of each unique source.</dd>
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

    private static final Set<SourceUsage> SOURCE_USAGES =
            ConcurrentHashMap.newKeySet();

    /**
     * Can be set during testing to cause {@link #checkSerially()} to return a
     * custom instance.
     */
    private static Health overriddenHealth;

    /**
     * <p>Informs the class of a {@link Source} that has just been used
     * successfully, and could be used again in the course of a health check.
     * Should be called by image processing endpoints after processing has
     * completed successfully.</p>
     *
     * <p>This method is thread-safe.</p>
     */
    public static void addSourceUsage(Source source) {
        final SourceUsage usage = new SourceUsage(source);
        // The pair is configured to read a specific image. We want to remove
        // any older "equal" (see this ivar's equals() method!) instance before
        // adding the current one because the older one is more likely to be
        // stale (no longer accessible), in light of the possibility that the
        // application has been running for a while.
        SOURCE_USAGES.remove(usage);
        SOURCE_USAGES.add(usage);
    }

    /**
     * For testing only!
     */
    public static Set<SourceUsage> getSourceUsages() {
        return SOURCE_USAGES;
    }

    /**
     * For testing only!
     *
     * @param health Custom instance that will be returned by {@link
     *               #checkSerially()}. Supply {@code null} to clear the
     *               override.
     */
    public static synchronized void overrideHealth(Health health) {
        overriddenHealth = health;
    }

    /**
     * <p>Checks the functionality of every {@link #addSourceUsage(Source)
     * known source}.</p>
     *
     * <p>The individual checks are done concurrently in as many threads as
     * there are unique pairs.</p>
     */
    private static synchronized void checkSources(Health health) {
        // Make a local copy to ensure that another thread doesn't change it
        // underneath us.
        final Set<SourceUsage> localUsages = new HashSet<>(SOURCE_USAGES);
        final int numUsages                = localUsages.size();

        LOGGER.trace("{} unique sources.", numUsages);

        final CountDownLatch latch = new CountDownLatch(numUsages);
        localUsages.forEach(usage -> {
            ThreadPool.getInstance().submit(() -> {
                final Source source = usage.getSource();
                LOGGER.trace("Exercising source I/O for {}", usage);
                // TODO: it would be nice to have a Source.exists() method here
                try (ImageInputStream is = source.newStreamFactory().newSeekableStream()) {
                    is.length();
                } catch (IOException e) {
                    health.setMinColor(Health.Color.RED);
                    health.setMessage(String.format("%s (%s)",
                            e.getMessage(), usage));
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
        final Optional<SourceCache> optSrcCache = cacheFacade.getSourceCache();
        if (optSrcCache.isPresent()) {
            final SourceCache srcCache = optSrcCache.get();
            LOGGER.trace("Exercising the source cache: {}", srcCache);
            final Identifier identifier =
                    new Identifier("HealthCheck-" + UUID.randomUUID());
            try {
                // Exercise the cache. Errors will throw exceptions.
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
                // Delete it.
                srcCache.purge(identifier);
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
            LOGGER.trace("Exercising the derivative cache: {}", dCache);
            final Identifier identifier =
                    new Identifier("HealthCheck-" + UUID.randomUUID());
            try {
                // Exercise the cache. Errors will throw exceptions.
                // Write to the cache.
                dCache.put(identifier, new Info());
                // Read it back.
                dCache.getInfo(identifier);
                // Delete it.
                dCache.purge(identifier);
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
     * Each group of checks (derivative cache, source I/O, etc.) is performed
     * sequentially. If any check fails, all remaining checks are skipped.</p>
     *
     * <p>N.B.: it may be faster to {@link #checkConcurrently() do all of the
     * checks concurrently}; but this could also result in having to do more
     * checks than necessary, since, if a single check fails, none of the
     * others matter.</p>
     *
     * @return Instance reflecting the application health.
     * @see #checkConcurrently()
     */
    public Health checkSerially() {
        LOGGER.debug("Initiating a health check");

        if (overriddenHealth != null) {
            return overriddenHealth;
        }

        final Stopwatch watch = new Stopwatch();
        final Health health   = new Health();

        // Check source input.
        if (!Health.Color.RED.equals(health.getColor())) {
            checkSources(health);
            LOGGER.trace("Source I/O check completed in {}; health so far is {}",
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
            LOGGER.debug("Sequential health check completed in {}: {}",
                    watch, health);
        } else {
            LOGGER.warn("Sequential health check completed in {}: {}",
                    watch, health);
        }
        return health;
    }

    /**
     * <p>Performs a health check as explained in the class documentation.
     * Each group of checks (derivative cache, source I/O, etc.) is performed
     * concurrently. This may cause the check to complete faster than the one
     * via {@link #checkSerially()} but it also may result in performing more
     * checks than necessary.</p>
     *
     * @return Instance reflecting the application health.
     * @see #checkSerially()
     */
    public Health checkConcurrently() {
        LOGGER.debug("Initiating a health check");

        if (overriddenHealth != null) {
            return overriddenHealth;
        }

        final Stopwatch watch      = new Stopwatch();
        final Health health        = new Health();
        final ThreadPool pool      = ThreadPool.getInstance();
        final CountDownLatch latch = new CountDownLatch(3);

        // Check source I/O.
        pool.submit(() -> {
            try {
                checkSources(health);
                LOGGER.trace("Source I/O check completed in {}; health so far is {}",
                        watch, health.getColor());
            } finally {
                latch.countDown();
            }
        });

        // Check the source cache.
        pool.submit(() -> {
            try {
                checkSourceCache(health);
                LOGGER.trace("Source cache check completed in {}; health so far is {}",
                        watch, health.getColor());
            } finally {
                latch.countDown();
            }
        });

        // Check the derivative cache.
        pool.submit(() -> {
            try {
                checkDerivativeCache(health);
                LOGGER.trace("Derivative cache check completed in {}; health so far is {}",
                        watch, health.getColor());
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }

        // Log the final status.
        if (Health.Color.GREEN.equals(health.getColor())) {
            LOGGER.debug("Concurrent health check completed in {}: {}",
                    watch, health);
        } else {
            LOGGER.warn("Concurrent health check completed in {}: {}",
                    watch, health);
        }
        return health;
    }

}
