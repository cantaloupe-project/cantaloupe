package edu.illinois.library.cantaloupe.source;

import com.amazonaws.services.s3.model.S3Object;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Source of streams for {@link S3Source}, returned from {@link
 * S3Source#newStreamFactory()}.
 */
class S3StreamFactory implements StreamFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(S3StreamFactory.class);

    private static final int DEFAULT_CHUNK_SIZE       = (int) Math.pow(2, 19);
    private static final int DEFAULT_CHUNK_CACHE_SIZE = (int) Math.pow(1024, 2);

    private S3ObjectInfo objectInfo;
    private S3Object object;

    S3StreamFactory(S3ObjectInfo objectInfo, S3Object object) {
        this.objectInfo = objectInfo;
        this.object     = object;
    }

    @Override
    public InputStream newInputStream() {
        final InputStream responseStream = object.getObjectContent();
        final AtomicBoolean isClosed     = new AtomicBoolean();

        // Ideally we would just return responseStream. However, if
        // responseStream is close()d before all of its data has been read,
        // its underlying TCP connection will also be closed, thus negating
        // the advantage of the connection pool, and triggering a warning
        // log message from the S3 client.
        //
        // This wrapper stream's close() method will drain the stream
        // before closing it. Because draining the stream may be expensive,
        // it will happen in another thread.
        return new InputStream() {
            @Override
            public void close() {
                if (!isClosed.get()) {
                    isClosed.set(true);

                    ThreadPool.getInstance().submit(() -> {
                        try {
                            try {
                                while (responseStream.read() != -1) {
                                    // drain the stream
                                }
                            } finally {
                                try {
                                    responseStream.close();
                                } finally {
                                    super.close();
                                }
                            }
                        } catch (IOException e) {
                            LOGGER.warn(e.getMessage(), e);
                        }
                    });
                }
            }

            @Override
            public int read() throws IOException {
                return responseStream.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return responseStream.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return responseStream.read(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return responseStream.skip(n);
            }
        };
    }

    @Override
    public ImageInputStream newSeekableStream() throws IOException {
        if (isChunkingEnabled()) {
            final int chunkSize = getChunkSize();
            LOGGER.debug("newSeekableStream(): using {}-byte chunks",
                    chunkSize);

            final S3HTTPImageInputStreamClient client =
                    new S3HTTPImageInputStreamClient(objectInfo);

            HTTPImageInputStream stream = new HTTPImageInputStream(
                    client, objectInfo.getLength());
            stream.setWindowSize(chunkSize);
            if (isChunkCacheEnabled()) {
                stream.setMaxChunkCacheSize(getMaxChunkCacheSize());
            }
            return stream;
        } else {
            LOGGER.debug("newSeekableStream(): chunking is disabled");
            return StreamFactory.super.newSeekableStream();
        }
    }

    private boolean isChunkingEnabled() {
        return Configuration.getInstance().getBoolean(
                Key.S3SOURCE_CHUNKING_ENABLED, true);
    }

    private int getChunkSize() {
        return (int) Configuration.getInstance().getLongBytes(
                Key.S3SOURCE_CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
    }

    private boolean isChunkCacheEnabled() {
        return Configuration.getInstance().getBoolean(
                Key.S3SOURCE_CHUNK_CACHE_ENABLED, true);
    }

    private int getMaxChunkCacheSize() {
        return (int) Configuration.getInstance().getLongBytes(
                Key.S3SOURCE_CHUNK_CACHE_MAX_SIZE, DEFAULT_CHUNK_CACHE_SIZE);
    }

}
