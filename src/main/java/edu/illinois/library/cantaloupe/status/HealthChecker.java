package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.source.FileSource;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.StreamSource;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
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
 * <p>Checks various aspects of the application to verify that they are
 * functioning correctly:</p>
 *
 * <dl>
 *     <dt>Source I/O</dt>
 *     <dd>When an image endpoint successfully completes a request, it calls
 *     {@link #addSourceUsage(Source)} to register the various objects it used
 *     to do so. This class keeps track of every unique source usage and tests
 *     each one against the last known image it worked with.</dd>
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
     * Can be set during testing to cause {@link #check()} to return a custom
     * instance.
     */
    private static Health overriddenHealth;

    /**
     * <p>Informs the class of a {@link Source} that has been used
     * successfully, and could be used again in the course of a health check.
     * Should be called by image processing endpoints after processing has
     * completed successfully.</p>
     *
     * <p>This method is thread-safe.</p>
     */
    public static void addSourceUsage(Source source) {
        final SourceUsage usage = new SourceUsage(source);
        // The source is configured to read a specific image. We want to remove
        // any older "equal" (see SourceUsage.equals()) instance before adding
        // the current one because the older one is more likely to be stale
        // (no longer accessible), in light of the possibility that the
        // application has been running for a while.
        SOURCE_USAGES.remove(usage);
        SOURCE_USAGES.add(usage);
    }

    /**
     * For testing only!
     */
    static Set<SourceUsage> getSourceUsages() {
        return SOURCE_USAGES;
    }

    /**
     * For testing only!
     *
     * @param health Custom instance that will be returned by {@link #check()}.
     *               Supply {@code null} to clear the override.
     */
    public static synchronized void setOverriddenHealth(Health health) {
        overriddenHealth = health;
    }

    /**
     * <p>Checks the functionality of every {@link #addSourceUsage(Source)
     * known} source usage.</p>
     *
     * <p>The individual checks are done concurrently in as many threads as
     * there are unique pairs. This is intended to improve responsiveness,
     * assuming there aren't a whole lot more pairs than there are CPU
     * threads.</p>
     */
    private static synchronized void checkSources(Health health) {
        // Make a local copy to ensure that another thread doesn't change it
        // underneath us.
        final Set<SourceUsage> localUsages =
                new HashSet<>(SOURCE_USAGES);
        final int numUsages = localUsages.size();

        LOGGER.trace("{} unique sources.", numUsages);

        final CountDownLatch latch = new CountDownLatch(numUsages);

        localUsages.forEach(usage -> {
            ThreadPool.getInstance().submit(() -> {
                LOGGER.debug("Exercising source I/O: {}", usage);
                ImageInputStream is = null;
                try {
                    if (usage.getSource() instanceof FileSource) {
                        FileSource source = (FileSource) usage.getSource();
                        is = new FileImageInputStream(source.getPath().toFile());
                    } else {
                        StreamSource source = (StreamSource) usage.getSource();
                        is = source.newStreamFactory().newSeekableStream();
                    }
                    is.length();
                } catch (Throwable t) {
                    health.setMinColor(Health.Color.RED);
                    health.setMessage(String.format("%s (%s)",
                            t.getMessage(), usage));
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            LOGGER.warn("checkSources(): {}", e.getMessage());
                        }
                    }
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
            LOGGER.debug("Exercising the source cache: {}", sCache);
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
            LOGGER.debug("Exercising the derivative cache: {}", dCache);
            final Identifier identifier =
                    new Identifier("HealthCheck-" + UUID.randomUUID());
            try {
                // Exercise the cache. Errors will throw exceptions.
                dCache.purge(identifier);
                // Write to the cache.
                dCache.put(identifier, new Info());
                // Read it back.
                dCache.getImageInfo(identifier);
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
     * Each group of checks (derivative cache, source I/O, etc.) is
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

        if (overriddenHealth != null) {
            return overriddenHealth;
        }

        final Stopwatch watch = new Stopwatch();
        final Health health   = new Health();

        // Check source I/O.
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
            LOGGER.debug("Health check completed in {}: {}", watch, health);
        } else {
            LOGGER.warn("Health check completed in {}: {}", watch, health);
        }
        return health;
    }

}
