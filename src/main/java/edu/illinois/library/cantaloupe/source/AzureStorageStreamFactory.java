package edu.illinois.library.cantaloupe.source;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Source of streams for {@link AzureStorageSource}, returned from {@link
 * AzureStorageSource#newStreamFactory()}.
 */
class AzureStorageStreamFactory implements StreamFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AzureStorageStreamFactory.class);

    private static final int DEFAULT_CHUNK_SIZE       = 1024 * 512;
    private static final int DEFAULT_CHUNK_CACHE_SIZE = 1024 * 1024 * 10;

    private final CloudBlockBlob blob;

    AzureStorageStreamFactory(CloudBlockBlob blob) {
        this.blob = blob;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        try {
            return blob.openInputStream();
        } catch (StorageException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public ImageInputStream newSeekableStream() throws IOException {
        if (isChunkingEnabled()) {
            final int chunkSize = getChunkSize();
            LOGGER.debug("newSeekableStream(): using {}-byte chunks",
                    chunkSize);

            final AzureStorageHTTPImageInputStreamClient client =
                    new AzureStorageHTTPImageInputStreamClient(blob);

            try {
                // Populate the blob's properties, if they haven't been already.
                blob.exists();
            } catch (StorageException e) {
                LOGGER.warn("newSeekableStream(): {}", e.getMessage());
            }

            HTTPImageInputStream stream = new HTTPImageInputStream(
                    client, blob.getProperties().getLength());
            try {
                stream.setWindowSize(chunkSize);
                if (isChunkCacheEnabled()) {
                    stream.setMaxChunkCacheSize(getMaxChunkCacheSize());
                }
                return stream;
            } catch (Throwable t) {
                IOUtils.closeQuietly(stream);
                throw t;
            }
        } else {
            LOGGER.debug("newSeekableStream(): chunking is disabled");
            return StreamFactory.super.newSeekableStream();
        }
    }

    @Override
    public boolean isSeekingDirect() {
        return isChunkingEnabled();
    }

    private boolean isChunkingEnabled() {
        return Configuration.getInstance().getBoolean(
                Key.AZURESTORAGESOURCE_CHUNKING_ENABLED, true);
    }

    private int getChunkSize() {
        return (int) Configuration.getInstance().getLongBytes(
                Key.AZURESTORAGESOURCE_CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
    }

    private boolean isChunkCacheEnabled() {
        return Configuration.getInstance().getBoolean(
                Key.AZURESTORAGESOURCE_CHUNK_CACHE_ENABLED, true);
    }

    private int getMaxChunkCacheSize() {
        return (int) Configuration.getInstance().getLongBytes(
                Key.AZURESTORAGESOURCE_CHUNK_CACHE_MAX_SIZE,
                DEFAULT_CHUNK_CACHE_SIZE);
    }

}
