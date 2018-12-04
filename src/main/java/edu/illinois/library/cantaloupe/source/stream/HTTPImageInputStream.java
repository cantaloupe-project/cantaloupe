package edu.illinois.library.cantaloupe.source.stream;

import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

/**
 * <p>Input stream that supports crude seeking over HTTP.</p>
 *
 * <p>The stream is divided conceptually into fixed-size windows a.k.a. chunks
 * which are fetched as needed using ranged HTTP requests. This may improve
 * efficiency when reading small portions of large images that are selectively
 * readable, like JPEG2000 or multiresolution/tiled TIFF. Conversely, it may
 * reduce efficiency when reading whole images.</p>
 *
 * <p>The HTTP client is abstracted into the exceedingly simple {@link
 * HTTPImageInputStreamClient} interface, so probably any HTTP client,
 * including many cloud storage clients, can be hooked up and used easily,
 * without this class needing to know about things like request signing
 * etc.</p>
 *
 * <p>This class works only with HTTP servers that support {@literal Range}
 * requests, as indicated by the presence of a {@literal Accept-Ranges: bytes}
 * header in a {@literal HEAD} response.</p>
 *
 * <p>Currently this class is simple and only demonstrates the windowing
 * technique. To improve performance, it could be enhanced to e.g. retain
 * multiple windows, cache them to disk, etc.</p>
 *
 * @author Alex Dolski UIUC
 * @since 4.1
 */
public class HTTPImageInputStream extends ImageInputStreamImpl
        implements ImageInputStream {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HTTPImageInputStream.class);

    private HTTPImageInputStreamClient client;
    private final int windowSize;
    private long streamLength;
    private int windowIndex = -1, indexWithinBuffer;
    private byte[] windowBuffer;

    /**
     * Variant that sends a preliminary {@literal HEAD} request to retrieve
     * some needed information. Use {@link #HTTPImageInputStream(
     * HTTPImageInputStreamClient, int, long)} instead if you already know the
     * resource length and that the server supports {@literal HEAD} requests.
     *
     * @param client         Client to use to handle requests.
     * @param windowSize     Window/chunk size. In general, a smaller size means
     *                       more requests will be needed, and a larger size
     *                       means more irrelevant data will have to be read and
     *                       discarded. The optimal size will vary depending on
     *                       the source image, the amount of data needed from
     *                       it, and network transfer rate vs. latency.
     * @throws RangesNotSupportedException if the server does not support
     *         ranged requests.
     * @throws IOException if something goes wrong when checking for range
     *         support.
     */
    public HTTPImageInputStream(HTTPImageInputStreamClient client,
                                int windowSize) throws IOException {
        this.client       = client;
        this.windowSize   = windowSize;
        this.windowBuffer = new byte[windowSize];

        sendHEADRequest();
    }

    /**
     * @param client         Client to use to handle requests.
     * @param windowSize     Window/chunk size. In general, a smaller size means
     *                       more requests will be needed, and a larger size
     *                       means more irrelevant data will have to be read and
     *                       discarded. The optimal size will vary depending on
     *                       the source image, the amount of data needed from
     *                       it, and network transfer rate vs. latency.
     * @param resourceLength Resource length/size.
     */
    public HTTPImageInputStream(HTTPImageInputStreamClient client,
                                int windowSize,
                                long resourceLength) {
        this.client       = client;
        this.windowSize   = windowSize;
        this.windowBuffer = new byte[windowSize];
        this.streamLength = resourceLength;
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
        streamLength = Long.parseLong(response.getHeaders().getFirstValue("Content-Length"));
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            client       = null;
            windowBuffer = null;
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
        prepareWindowBuffer();
        bitOffset = 0;
        int b = windowBuffer[indexWithinBuffer] & 0xff;
        indexWithinBuffer++;
        streamPos++;
        return b;
    }

    @Override
    public int read(byte[] b,
                    int offset,
                    int requestedLength) throws IOException {
        if (streamPos >= streamLength) {
            return -1;
        } else if (offset < 0) {
            throw new IndexOutOfBoundsException("Negative offset");
        } else if (requestedLength < 0) {
            throw new IndexOutOfBoundsException("Negative length");
        } else if (offset + requestedLength > b.length) {
            throw new IndexOutOfBoundsException("offset + length > buffer length");
        }

        if (streamPos + requestedLength >= streamLength) {
            requestedLength = (int) (streamLength - streamPos);
        }

        prepareWindowBuffer();
        bitOffset = 0;

        final int fulfilledLength = Math.min(
                requestedLength,
                windowBuffer.length - indexWithinBuffer);
        System.arraycopy(
                windowBuffer, indexWithinBuffer, // from, from index
                b, offset,                       // to, to index
                fulfilledLength);                // length

        indexWithinBuffer += fulfilledLength;
        streamPos += fulfilledLength;
        return fulfilledLength;
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos);
        indexWithinBuffer = (int) streamPos % windowSize;
    }

    /**
     * Checks that the {@link #windowBuffer window buffer} has some readable
     * bytes remaining, and fills it with more if not.
     */
    private void prepareWindowBuffer() throws IOException {
        final int streamWindowIndex = getStreamWindowIndex();
        if (streamWindowIndex != windowIndex) {
            Range range = getRange(streamWindowIndex);
            LOGGER.trace("Filling window buffer with range: {}", range);
            Response response = client.sendGETRequest(range);
            windowBuffer      = response.getBody();
            windowIndex       = streamWindowIndex;
            indexWithinBuffer = (int) streamPos % windowSize;
        }
    }

    private Range getRange(int windowIndex) {
        final Range range = new Range();
        range.start       = windowIndex * windowSize;
        range.end         = Math.min(range.start + windowSize, streamLength) - 1;
        range.length      = streamLength;
        return range;
    }

    /**
     * Finds the current window index based on the {@link #streamPos stream
     * position}.
     */
    private int getStreamWindowIndex() {
        return (int) Math.floor(streamPos / (double) windowSize);
    }

}
