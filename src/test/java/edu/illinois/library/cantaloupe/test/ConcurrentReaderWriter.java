package edu.illinois.library.cantaloupe.test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrently pits a reader against a writer.
 */
public class ConcurrentReaderWriter {

    private static final short NUM_THREADS = 500;

    private final Runnable reader;
    private final Runnable writer;
    private final AtomicInteger readCount = new AtomicInteger(0);
    private final AtomicInteger writeCount = new AtomicInteger(0);

    /**
     * N.B.: Reader and writer exception handlers should call
     * {@link org.junit.Assert#fail}.
     */
    public ConcurrentReaderWriter(Runnable writer, Runnable reader) {
        this.writer = writer;
        this.reader = reader;
    }

    public void run() {
        for (int i = 0; i < NUM_THREADS / 2f; i++) {
            new Thread(() -> { // writer thread
                try {
                    writer.run();
                } finally {
                    writeCount.incrementAndGet();
                }
            }).start();

            new Thread(() -> {
                while (true) {
                    // Spin until we have something to read.
                    if (writeCount.get() > 0) {
                        try {
                            reader.run();
                        } finally {
                            readCount.incrementAndGet();
                        }
                        break;
                    } else {
                        sleep(1);
                    }
                }
            }).start();
        }

        while (readCount.get() < NUM_THREADS / 2f ||
                writeCount.get() < NUM_THREADS / 2f) {
            sleep(1);
        }
    }

    private void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            // whatever
        }
    }

}
