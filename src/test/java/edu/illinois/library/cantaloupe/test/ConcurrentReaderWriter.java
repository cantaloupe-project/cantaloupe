package edu.illinois.library.cantaloupe.test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrently pits a reader against a writer.
 */
public class ConcurrentReaderWriter {

    private final AtomicBoolean anyFailures = new AtomicBoolean(false);
    private final Callable<Void> reader;
    private final Callable<Void> writer;
    private final AtomicInteger readCount = new AtomicInteger(0);
    private final AtomicInteger writeCount = new AtomicInteger(0);

    private int numThreads = 500;

    /**
     * N.B.: Reader and writer exception handlers should call
     * {@link org.junit.Assert#fail}.
     */
    public ConcurrentReaderWriter(Callable<Void> writer, Callable<Void> reader) {
        this.writer = writer;
        this.reader = reader;
    }

    public void run() throws Exception {
        for (int i = 0; i < numThreads / 2f; i++) {
            new Thread(() -> { // writer thread
                try {
                    writer.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    anyFailures.set(true);
                } finally {
                    writeCount.incrementAndGet();
                }
            }).start();

            new Thread(() -> {
                while (true) {
                    // Spin until we have something to read.
                    if (writeCount.get() > 0) {
                        try {
                            reader.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                            anyFailures.set(true);
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

        while (readCount.get() < numThreads / 2f ||
                writeCount.get() < numThreads / 2f) {
            sleep(1);
        }

        if (anyFailures.get()) {
            throw new Exception();
        }
    }

    public ConcurrentReaderWriter numThreads(int count) {
        this.numThreads = count;
        return this;
    }

    private void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            // whatever
        }
    }

}
