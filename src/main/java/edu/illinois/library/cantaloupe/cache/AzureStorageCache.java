package edu.illinois.library.cantaloupe.cache;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobOutputStream;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @see <a href="https://github.com/azure/azure-storage-java">
 *     Microsoft Azure Storage DSK for Java</a>
 */
class AzureStorageCache implements DerivativeCache {

    private static class AzureStorageOutputStream extends OutputStream {

        private String blobKey;
        private BlobOutputStream blobOutputStream;
        private Set<String> uploadingKeys;

        AzureStorageOutputStream(String blobKey,
                                 BlobOutputStream blobOutputStream,
                                 Set<String> uploadingKeys) {
            this.blobKey = blobKey;
            this.blobOutputStream = blobOutputStream;
            this.uploadingKeys = uploadingKeys;
        }

        @Override
        public void close() throws IOException {
            try {
                blobOutputStream.close();
            } finally {
                try {
                    super.close();
                } finally {
                    uploadingKeys.remove(blobKey);
                }
            }
        }

        @Override
        public void flush() throws IOException {
            blobOutputStream.flush();
        }

        @Override
        public void write(int b) throws IOException {
            blobOutputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            blobOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            blobOutputStream.write(b, off, len);
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AzureStorageCache.class);

    private static CloudBlobClient client;

    /**
     * Blob keys currently being written to Azure Storage from any thread.
     */
    private static final Set<String> uploadingKeys =
            new ConcurrentSkipListSet<>();

    static synchronized CloudBlobClient getClientInstance() {
        if (client == null) {
            try {
                final Configuration config = Configuration.getInstance();
                final String accountName =
                        config.getString(Key.AZURESTORAGECACHE_ACCOUNT_NAME);
                final String accountKey =
                        config.getString(Key.AZURESTORAGECACHE_ACCOUNT_KEY);

                final String connectionString = String.format(
                        "DefaultEndpointsProtocol=https;" +
                                "AccountName=%s;" +
                                "AccountKey=%s", accountName, accountKey);
                final CloudStorageAccount account =
                        CloudStorageAccount.parse(connectionString);

                LOGGER.info("Using account: {}", accountName);

                client = account.createCloudBlobClient();

                client.getContainerReference(getContainerName()).
                        createIfNotExists();
            } catch (StorageException | URISyntaxException | InvalidKeyException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return client;
    }

    static String getContainerName() {
        // All letters in a container name must be lowercase.
        return Configuration.getInstance().
                getString(Key.AZURESTORAGECACHE_CONTAINER_NAME).toLowerCase();
    }

    private static Instant getEarliestValidInstant() {
        final Configuration config = Configuration.getInstance();
        final long ttl = config.getLong(Key.DERIVATIVE_CACHE_TTL);
        return (ttl > 0) ?
                Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(ttl) :
                Instant.MIN;
    }

    @Override
    public Info getImageInfo(Identifier identifier) throws IOException {
        final String containerName = getContainerName();
        final CloudBlobClient client = getClientInstance();

        try {
            final Stopwatch watch = new Stopwatch();
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final String objectKey = getObjectKey(identifier);

            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            if (blob.exists()) {
                if (isValid(blob)) {
                    try (InputStream is = blob.openInputStream()) {
                        Info info = Info.fromJSON(is);
                        LOGGER.info("getImageInfo(): read {} from container {} in {}",
                                objectKey, containerName, watch);
                        return info;
                    }
                } else {
                    LOGGER.debug("getImageInfo(): deleting invalid item " +
                                    "asynchronously: {} in container {}",
                            objectKey, containerName);
                    purgeAsync(blob);
                }
            }
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final String objectKey = getObjectKey(opList);

            LOGGER.info("newDerivativeImageInputStream(): bucket: {}; key: {}",
                    containerName, objectKey);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            if (blob.exists()) {
                if (isValid(blob)) {
                    return blob.openInputStream();
                } else {
                    LOGGER.debug("newDerivativeImageInputStream(): " +
                                    "deleting invalid item asynchronously: " +
                                    "{} in container {}",
                            objectKey, containerName);
                    purgeAsync(blob);
                }
            }
            return null;
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList opList)
            throws IOException {
        final String objectKey = getObjectKey(opList);
        if (!uploadingKeys.contains(objectKey)) {
            uploadingKeys.add(objectKey);
            final String containerName = getContainerName();
            final CloudBlobClient client = getClientInstance();
            try {
                final CloudBlobContainer container =
                        client.getContainerReference(containerName);
                final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
                blob.getProperties().setContentType(opList.getOutputFormat().
                        getPreferredMediaType().toString());

                return new AzureStorageOutputStream(
                        objectKey, blob.openOutputStream(), uploadingKeys);
            } catch (URISyntaxException | StorageException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return new NullOutputStream();
    }

    /**
     * @param identifier
     * @return Object key of the serialized Info associated with the given
     *         identifier.
     */
    String getObjectKey(Identifier identifier) {
        try {
            return getObjectKeyPrefix() + "info/" +
                    URLEncoder.encode(identifier.toString(), "UTF-8") + ".json";
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param opList
     * @return Object key of the image associated with the given operation list.
     */
    String getObjectKey(OperationList opList) {
        try {
            return getObjectKeyPrefix() + "image/" +
                    URLEncoder.encode(opList.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @return Value of {@link Key#AZURESTORAGECACHE_OBJECT_KEY_PREFIX}
     *         with trailing slash.
     */
    String getObjectKeyPrefix() {
        String prefix = Configuration.getInstance().
                getString(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX);
        if (prefix.length() < 1 || prefix.equals("/")) {
            return "";
        }
        return StringUtils.stripEnd(prefix, "/") + "/";
    }

    private boolean isValid(CloudBlob blob) {
        return blob.getProperties().getLastModified().toInstant().
                isAfter(getEarliestValidInstant());
    }

    @Override
    public void purge() throws IOException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            int count = 0;
            for (ListBlobItem item : container.listBlobs(getObjectKeyPrefix(), true)) {
                if (item instanceof CloudBlob) {
                    CloudBlob blob = (CloudBlob) item;
                    if (blob.deleteIfExists()) {
                        count++;
                    }
                }
            }
            LOGGER.info("purge(): deleted {} items", count);
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(OperationList opList) throws IOException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        final String objectKey = getObjectKey(opList);

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            blob.deleteIfExists();
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void purgeAsync(CloudBlob blob) {
        TaskQueue.getInstance().submit(() -> {
            LOGGER.debug("purgeAsync(): {}", blob);
            try {
                blob.deleteIfExists();
            } catch (StorageException e) {
                LOGGER.warn("purgeAsync(): failed to delete {}: {}",
                        blob, e.getMessage());
            }
        });
    }

    @Override
    public void purgeInvalid() throws IOException {
        final String containerName = getContainerName();
        final CloudBlobClient client = getClientInstance();

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            int count = 0, deletedCount = 0;
            for (ListBlobItem item : container.listBlobs(getObjectKeyPrefix(), true)) {
                if (item instanceof CloudBlob) {
                    CloudBlob blob = (CloudBlob) item;
                    count++;
                    if (!isValid(blob)) {
                        if (blob.deleteIfExists()) {
                            deletedCount++;
                        }
                    }
                }
            }
            LOGGER.info("purgeInvalid(): deleted {} of {} items",
                    deletedCount, count);
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(Identifier identifier) throws IOException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        final String objectKey = getObjectKey(identifier);

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            blob.deleteIfExists();
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void put(Identifier identifier, Info imageInfo) throws IOException {
        final String objectKey = getObjectKey(identifier);
        if (!uploadingKeys.contains(objectKey)) {
            uploadingKeys.add(objectKey);
            try {
                final String containerName = getContainerName();
                final CloudBlobClient client = getClientInstance();
                final CloudBlobContainer container =
                        client.getContainerReference(containerName);
                final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
                blob.getProperties().setContentType("application/json");
                blob.getProperties().setContentEncoding("UTF-8");

                // writeAsJSON() will close this.
                OutputStream os = new AzureStorageOutputStream(
                        objectKey, blob.openOutputStream(), uploadingKeys);
                imageInfo.writeAsJSON(os);
            } catch (URISyntaxException | StorageException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }
}
