package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads content from a {@link StreamResolver} to a source cache.
 */
final class SourceCacheDownload implements Future<Path> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SourceCacheDownload.class);

    /**
     * Set of identifiers of images that are currently being downloaded in any
     * thread. Used to avoid concurrent downloads.
     */
    private static final Set<Identifier> DOWNLOADING_IMAGES =
            ConcurrentHashMap.newKeySet();

    private static final int STREAM_BUFFER_SIZE = 32768;

    /**
     * Maximum number of a times a source image download will be attempted.
     */
    private static final short MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS = 2;

    private final CountDownLatch downloadLatch      = new CountDownLatch(1);
    private final AtomicBoolean isCancelled         = new AtomicBoolean();
    private final AtomicBoolean isDownloadAttempted = new AtomicBoolean();
    private final AtomicBoolean mayInterrupt        = new AtomicBoolean();
    private StreamSource streamSource;
    private SourceCache sourceCache;
    private Identifier identifier;

    SourceCacheDownload(StreamSource streamSource,
                        SourceCache sourceCache,
                        Identifier identifier) {
        this.streamSource = streamSource;
        this.sourceCache = sourceCache;
        this.identifier = identifier;
    }

    void downloadAsync() {
        if (isDownloadAttempted.get()) {
            return;
        }
        ThreadPool.getInstance().submit(() -> {
            try {
                downloadSync();
            } catch (IOException e) {
                LOGGER.error("downloadAsync(): {}", e.getMessage());
            }
            return null;
        });
    }

    void downloadSync() throws IOException {
        if (isDownloadAttempted.get()) {
            return;
        }
        isDownloadAttempted.set(true);

        boolean succeeded = false;
        short numAttempts = 0;
        try {
            do {
                numAttempts++;
                try {
                    // This will block while a file is being written in another
                    // thread, which will prevent the image from being
                    // downloaded multiple times, and maybe enable other
                    // threads to get the image sooner.
                    // If it throws an exception, we will log it and retry a
                    // few times, and only rethrow it on the last try.
                    Path sourceFile = sourceCache.getSourceImageFile(identifier);
                    if (sourceFile == null) {
                        downloadToSourceCache(streamSource, sourceCache, identifier);
                    }
                    succeeded = true;
                } catch (IOException e) {
                    LOGGER.error("downloadSync(): {} (attempt {} of {})",
                            e.getMessage(),
                            numAttempts,
                            MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS);
                    if (numAttempts == MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS) {
                        throw e;
                    }
                }
            } while (!succeeded && numAttempts < MAX_NUM_SOURCE_CACHE_RETRIEVAL_ATTEMPTS);
        } finally {
            downloadLatch.countDown();
        }
    }

    /**
     * Downloads the source image with the given identifier from the given
     * resolver to the given source cache.
     *
     * @param streamSource Source of streams to read from.
     * @param sourceCache  Source cache to write to.
     * @param identifier   Identifier of the source image.
     * @throws IOException if anything goes wrong.
     */
    private void downloadToSourceCache(StreamSource streamSource,
                                       SourceCache sourceCache,
                                       Identifier identifier) throws IOException {
        synchronized (DOWNLOADING_IMAGES) {
            while (DOWNLOADING_IMAGES.contains(identifier)) {
                try {
                    LOGGER.debug("downloadToSourceCache(): waiting on {}...",
                            identifier);
                    DOWNLOADING_IMAGES.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }

            if (sourceCache.getSourceImageFile(identifier) != null) {
                return;
            }

            DOWNLOADING_IMAGES.add(identifier);
        }

        final Stopwatch watch = new Stopwatch();

        try (InputStream is = new BufferedInputStream(
                streamSource.newInputStream(),
                STREAM_BUFFER_SIZE);
             OutputStream os = new BufferedOutputStream(
                     sourceCache.newSourceImageOutputStream(identifier),
                     STREAM_BUFFER_SIZE)) {

            LOGGER.debug("Downloading {} to {}",
                    identifier,
                    SourceCache.class.getSimpleName());
            final byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            int n;
            while ((n = is.read(buffer)) > 0) {
                os.write(buffer, 0, n);

                if (isCancelled.get()) {
                    if (mayInterrupt.get()) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
            }

            LOGGER.debug("Downloaded {} to {} in {}",
                    identifier,
                    SourceCache.class.getSimpleName(),
                    watch);
        } finally {
            DOWNLOADING_IMAGES.remove(identifier);
            synchronized (DOWNLOADING_IMAGES) {
                DOWNLOADING_IMAGES.notifyAll();
            }
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        mayInterrupt.set(mayInterruptIfRunning);
        isCancelled.set(true);
        return true;
    }

    @Override
    public Path get() throws InterruptedException {
        downloadLatch.await();
        try {
            return sourceCache.getSourceImageFile(identifier);
        } catch (IOException e) {
            LOGGER.error("get(): {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Path get(long timeout,
                    TimeUnit unit) throws InterruptedException {
        downloadLatch.await(timeout, unit);
        try {
            return sourceCache.getSourceImageFile(identifier);
        } catch (IOException e) {
            LOGGER.error("get(): {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled.get();
    }

    @Override
    public boolean isDone() {
        return downloadLatch.getCount() == 0;
    }

}