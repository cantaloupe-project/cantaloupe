package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asynchronously downloads content from a {@link StreamResolver} to a
 * temporary file.
 */
final class TempFileDownload implements Future<Path> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TempFileDownload.class);

    private static final int STREAM_BUFFER_SIZE = 16384;

    private final CountDownLatch downloadLatch    = new CountDownLatch(1);
    private final AtomicBoolean isCancelled       = new AtomicBoolean();
    private final AtomicBoolean mayInterrupt      = new AtomicBoolean();
    private final AtomicBoolean downloadAttempted = new AtomicBoolean();
    private StreamSource streamSource;
    private Path tempFile;

    TempFileDownload(StreamSource streamSource, Path tempFile) {
        this.streamSource = streamSource;
        this.tempFile = tempFile;
    }

    void downloadAsync() {
        if (downloadAttempted.get()) {
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
        if (downloadAttempted.get()) {
            return;
        }
        downloadAttempted.set(true);

        try {
            try (InputStream is = new BufferedInputStream(
                    streamSource.newInputStream(),
                    STREAM_BUFFER_SIZE);
                 OutputStream os = new BufferedOutputStream(
                         Files.newOutputStream(tempFile),
                         STREAM_BUFFER_SIZE)) {

                LOGGER.debug("Downloading to {}", tempFile);

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
            }
        } finally {
            downloadLatch.countDown();
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
        return tempFile;
    }

    @Override
    public Path get(long timeout,
                    TimeUnit unit) throws InterruptedException {
        downloadLatch.await(timeout, unit);
        return tempFile;
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