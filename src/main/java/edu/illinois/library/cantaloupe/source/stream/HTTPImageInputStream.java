package edu.illinois.library.cantaloupe.source.stream;

import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.util.ObjectCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.LongAdder;

/**
 * <p>Input stream that supports pseudo-seeking over HTTP.</p>
 *
 * <p>The stream is divided conceptually into fixed-size windows into which
 * chunks of data are fetched as needed using ranged HTTP requests. This
 * technique may improve efficiency when reading small portions of large images
 * that are selectively readable, like JPEG2000 and multiresolution+tiled TIFF,
 * over low-bandwidth connections. Conversely, it may reduce efficiency when
 * reading large portions of images.</p>
 *
 * <p>Downloaded chunks can be cached in memory by passing a positive value to
 * {@link #setMaxChunkCacheSize(long)}. This could help readers that seek
 * around a lot beyond the window size. The cache is per-instance.</p>
 *
 * <p>The HTTP client is abstracted into the exceedingly simple {@link
 * HTTPImageInputStreamClient} interface, so probably any existing client
 * implementation, including many cloud storage clients, can be hooked up and
 * used easily, without this class needing to know about things like SSL/TLS,
 * request signing, etc.</p>
 *
 * <p>This class works only with HTTP servers that support {@literal Range}
 * requests, as advertised by the presence of a {@literal Accept-Ranges: bytes}
 * header in a {@literal HEAD} response.</p>
 *
 * @author Alex Dolski UIUC
 * @since 4.1
 */
public class HTTPImageInputStream extends ImageInputStreamImpl
        implements ImageInputStream {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HTTPImageInputStream.class);

    /**
     * Enables debug logging, which may be very expensive.
     */
    private static final boolean DEBUG = false;

    /**
     * Can be overridden by {@link #setWindowSize(int)}.
     */
    private static final int DEFAULT_WINDOW_SIZE = 1024 * 512;

    private HTTPImageInputStreamClient client;
    private ObjectCache<Range,byte[]> chunkCache;
    private long streamLength   = -1;
    private int windowPos;
    private int windowSize      = DEFAULT_WINDOW_SIZE;
    private int windowIndex     = -1;
    private byte[] windowBuffer = new byte[windowSize];

    private int prefetchCount;
    private final Map<Range,CompletableFuture<Response>> inFlight =
            new ConcurrentHashMap<>();

    private int numChunkDownloads, numChunkCacheHits, numChunkCacheMisses,
            numPrefetchHits;
    private long numBytesRead;
    private final LongAdder bytesDownloadedAdder = new LongAdder();

    private static void debug(String message, Object... vars) {
        if (DEBUG) {
            LOGGER.trace(message, vars);
        }
    }

    /**
     * Variant that sends a preliminary {@literal HEAD} request to retrieve
     * some needed information. Use {@link #HTTPImageInputStream(
     * HTTPImageInputStreamClient, long)} instead if you already know the
     * resource length and that the server supports ranged requests.
     *
     * @param client Client to use to handle requests.
     * @throws RangesNotSupportedException if the server does not advertise
     *                                     support for ranges.
     * @throws IOException if the response does not include a valid {@literal
     *                     Content-Length} header or some other communication
     *                     error occurs.
     */
    public HTTPImageInputStream(HTTPImageInputStreamClient client)
            throws IOException {
        this.client = client;
        sendHEADRequest();
    }

    /**
     * @param client         Client to use to handle requests.
     * @param resourceLength Resource length/size.
     */
    public HTTPImageInputStream(HTTPImageInputStreamClient client,
                                long resourceLength) {
        this.client       = client;
        this.streamLength = resourceLength;
    }

    public long getMaxChunkCacheSize() {
        if (chunkCache != null) {
            return chunkCache.maxSize();
        }
        return 0;
    }

    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Must be called before any reading or seeking occurs, but
     * <strong>after</strong> {@link #setWindowSize(int)}.
     *
     * @param maxChunkCacheSize Maximum byte size of the shared chunk cache.
     *                          Supply {@literal 0} to disable the chunk cache.
     */
    public void setMaxChunkCacheSize(long maxChunkCacheSize) {
        long count = Math.round(maxChunkCacheSize / (double) getWindowSize());
        if (count > 0) {
            chunkCache = new ObjectCache<>(count);
        }
    }

    public int getPrefetchCount() {
        return prefetchCount;
    }

    /**
     * <p>Sets the number of windows to prefetch asynchronously after each
     * fetched chunk. Must be called before any reading or seeking occurs.</p>
     *
     * <p>Set to {@code 0} (the default) to disable prefetching. Each
     * outstanding prefetch holds {@link #getWindowSize() windowSize} bytes
     * once it completes, so memory usage per stream is bounded by
     * {@code (prefetchCount + 1) * windowSize}.</p>
     */
    public void setPrefetchCount(int prefetchCount) {
        this.prefetchCount = Math.max(0, prefetchCount);
    }

    /**
     * <p>Sets the window size. Must be called before any reading or seeking
     * occurs.</p>
     *
     * <p>In general, a smaller size means more requests may be needed, and a
     * larger size means more irrelevant data will have to be read and
     * discarded. The optimal size will vary depending on the source image, the
     * amount of data needed from it, and network transfer rate vs.
     * latency. The size should probably always be at least a few KB so as to
     * be able to read the image header in one request.</p>
     *
     * @param windowSize Window/chunk size.
     */
    public void setWindowSize(int windowSize) {
        this.windowSize   = windowSize;
        this.windowBuffer = new byte[windowSize];
    }

    /**
     * Checks whether the server supports the {@literal Range} header and reads
     * the resource length from the {@literal Content-Length} header.
     */
    private void sendHEADRequest() throws IOException {
        Response response = client.sendHEADRequest();

        if (!"bytes".equals(response.getHeaders().getFirstValue("Accept-Ranges"))) {
            throw new RangesNotSupportedException();
        }
        try {
            streamLength = Long.parseLong(
                    response.getHeaders().getFirstValue("Content-Length"));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid or missing Content-Length header");
        }
    }

    @Override
    public void close() throws IOException {
        for (CompletableFuture<Response> f : inFlight.values()) {
            f.cancel(true);
        }
        inFlight.clear();
        logStatistics();
        try {
            super.close();
        } finally {
            client       = null;
            windowBuffer = null;
            chunkCache   = null;
        }
    }

    private void logStatistics() {
        final long numBytesDownloaded = bytesDownloadedAdder.sum();
        LOGGER.debug("Downloaded {} chunks ({} ({}%) of {} bytes); " +
                        "read {}% of chunk data; {} cache hits; " +
                        "{} prefetch hits; {} cache misses",
                numChunkDownloads,
                numBytesDownloaded,
                String.format("%.2f", numBytesDownloaded * 100 / (double) streamLength),
                streamLength,
                String.format("%.2f", numBytesRead * 100 / (double) Math.max(1, numBytesDownloaded)),
                numChunkCacheHits,
                numPrefetchHits,
                numChunkCacheMisses);
    }

    /**
     * Invalidates any cached data lying entirely before the stream position.
     */
    @Override
    public void flushBefore(long pos) throws IOException {
        super.flushBefore(pos);
        if (chunkCache != null) {
            chunkCache.asMap().keySet()
                    .stream()
                    .filter(range -> range.end < streamPos)
                    .forEach(range -> chunkCache.remove(range));
        }
    }

    @Override
    public long length() {
        return streamLength;
    }

    @Override
    public int read() throws IOException {
        if (streamPos >= streamLength) {
            return -1;
        }

        debug("read(): begin [pos: {}] [windowPos: {}]",
                streamPos, windowPos);

        prepareWindowBuffer();
        bitOffset = 0;
        int b = windowBuffer[windowPos] & 0xff;
        numBytesRead++;
        windowPos++;
        streamPos++;

        debug("read(): end [pos: {}] [windowPos: {}]",
                streamPos, windowPos);
        return b;
    }

    @Override
    public int read(byte[] b,
                    int offset,
                    int requestedLength) throws IOException {
        // Boilerplate checks required by the method contract.
        if (streamPos >= streamLength) {
            return -1;
        } else if (offset < 0) {
            throw new IndexOutOfBoundsException("Negative offset");
        } else if (requestedLength < 0) {
            throw new IndexOutOfBoundsException("Negative length");
        } else if (offset + requestedLength > b.length) {
            throw new IndexOutOfBoundsException("offset + length > buffer length");
        }

        // Also required by the method contract.
        bitOffset = 0;

        debug("read(byte[],int,int): begin [pos: {}] [windowPos: {}] [requested: {}]",
                streamPos, windowPos, requestedLength);

        // N.B.: although the method contract says we don't have to read all of
        // requestedLength as long as we return a count of what we did read,
        // in practice, readers won't' always check the return value, so we
        // will try to read as much of requestedLength as possible.
        int fulfilledLength = 0, incrementLength = 0;
        while (fulfilledLength < requestedLength && incrementLength != -1) {
            int remainingLength = requestedLength - fulfilledLength;
            incrementLength = readIncrement(b, offset, remainingLength);
            fulfilledLength += incrementLength;
            offset          += incrementLength;
        }

        debug("read(byte[],int,int): end [pos: {}] [windowPos: {}] [fulfilled: {}]",
                streamPos, windowPos, fulfilledLength);

        return fulfilledLength;
    }

    /**
     * Reads the smaller of {@literal requestedLength} or the remaining window
     * size into a byte array.
     *
     * @return Number of bytes read, or {@literal -1} when there are no more
     *         bytes to read.
     */
    private int readIncrement(byte[] b,
                              int offset,
                              int requestedLength) throws IOException {
        prepareWindowBuffer();

        final int fulfilledLength = Math.min(
                requestedLength,
                windowBuffer.length - windowPos);
        System.arraycopy(
                windowBuffer, windowPos, // from, from index
                b, offset,               // to, to index
                fulfilledLength);        // length

        numBytesRead += fulfilledLength;
        windowPos    += fulfilledLength;
        streamPos    += fulfilledLength;

        return (fulfilledLength > 0) ? fulfilledLength : -1;
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos);
        windowPos = getIndexWithinWindow();

        debug("seek(): [pos: {}] [windowPos: {}]",
                streamPos, windowPos);
    }

    /**
     * Checks that {@link #windowBuffer} has some readable bytes remaining,
     * and fills it with more if not.
     */
    private void prepareWindowBuffer() throws IOException {
        final int neededWindowIndex = getStreamWindowIndex();
        if (neededWindowIndex != windowIndex) {
            Range range  = getRange(neededWindowIndex);
            windowBuffer = fetchChunk(range);
            windowIndex  = neededWindowIndex;
            windowPos    = getIndexWithinWindow();
        }
    }

    /**
     * Fetches a chunk for the given range. Lookup order: chunk cache,
     * in-flight prefetch, synchronous download. After serving the chunk,
     * triggers asynchronous prefetches for the next {@link #prefetchCount}
     * windows.
     */
    private byte[] fetchChunk(Range range) throws IOException {
        if (chunkCache != null) {
            byte[] cached = chunkCache.get(range);
            if (cached != null) {
                LOGGER.trace("Chunk cache hit for range: {}", range);
                numChunkCacheHits++;
                triggerPrefetch(windowIndexOf(range));
                return cached;
            }
        }

        CompletableFuture<Response> pending = inFlight.get(range);
        if (pending != null) {
            try {
                byte[] data = pending.get().getBody();
                numPrefetchHits++;
                triggerPrefetch(windowIndexOf(range));
                return data;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted awaiting prefetch of " + range, e);
            } catch (ExecutionException e) {
                inFlight.remove(range);
                LOGGER.debug("Prefetch failed for {}; falling back to sync",
                        range, e);
                // Fall through to the synchronous path below.
            }
        }

        numChunkCacheMisses++;
        byte[] data = downloadChunk(range);
        if (chunkCache != null) chunkCache.put(range, data);
        triggerPrefetch(windowIndexOf(range));
        return data;
    }

    private byte[] downloadChunk(Range range) throws IOException {
        debug("Downloading range: {}", range);
        Response response = client.sendGETRequest(range);
        byte[] entity     = response.getBody();
        bytesDownloadedAdder.add(entity.length);
        numChunkDownloads++;
        return entity;
    }

    private void triggerPrefetch(int currentWindowIndex) {
        if (prefetchCount <= 0 || client == null) {
            return;
        }
        for (int i = 1; i <= prefetchCount; i++) {
            final int targetWindow = currentWindowIndex + i;
            final long start = (long) targetWindow * windowSize;
            if (start >= streamLength) {
                break;
            }
            final Range r = getRange(targetWindow);
            if (inFlight.containsKey(r)) {
                continue;
            }
            if (chunkCache != null && chunkCache.get(r) != null) {
                continue;
            }
            CompletableFuture<Response> future = client.sendGETRequestAsync(r);
            future.whenComplete((response, err) -> {
                inFlight.remove(r);
                if (err == null && response != null) {
                    byte[] data = response.getBody();
                    if (data != null) {
                        bytesDownloadedAdder.add(data.length);
                        if (chunkCache != null) {
                            chunkCache.put(r, data);
                        }
                    }
                }
            });
            inFlight.put(r, future);
            numChunkDownloads++;
            LOGGER.trace("Prefetching range: {}", r);
        }
    }

    private int windowIndexOf(Range range) {
        return (int) (range.start / windowSize);
    }

    private Range getRange(int windowIndex) {
        final Range range = new Range();
        range.start       = (long) windowIndex * windowSize;
        range.end         = Math.min(range.start + windowSize, streamLength) - 1;
        range.length      = streamLength;
        return range;
    }

    private int getIndexWithinWindow() {
        return (int) (streamPos % (long) windowSize);
    }

    /**
     * Calculates the current window index based on {@link #streamPos}, but
     * does not update {@link #windowIndex}.
     */
    private int getStreamWindowIndex() {
        return (int) Math.floor(streamPos / (double) windowSize);
    }

}
