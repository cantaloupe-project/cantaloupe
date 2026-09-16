package edu.illinois.library.cantaloupe.cache;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlockBlobOutputStreamOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlobOutputStream;
import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @see <a href="https://github.com/Azure/azure-sdk-for-java">
 *     Microsoft Azure SDK for Java</a>
 */
class AzureStorageCache implements DerivativeCache {

    private static class CustomBlobOutputStream
            extends CompletableOutputStream {

        private final BlobContainerClient container;
        private final BlobClient blob;
        private final String blobKey;
        private final Set<String> uploadingKeys;
        private final BlobOutputStream blobOutputStream;

        /**
         * Constructor for an instance that writes directly into the given
         * blob.
         *
         * @param blob          Blob to write to.
         * @param uploadingKeys All keys that are currently being uploaded in
         *                      in any thread, including {@code
         *                      permanentBlobKey}, which {@link #close()} will
         *                      remove.
         */
        CustomBlobOutputStream(BlobClient blob,
                               BlobHttpHeaders headers,
                               Set<String> uploadingKeys) {
            this.container        = null;
            this.blob             = blob;
            this.blobKey          = blob.getBlobName();
            this.uploadingKeys    = uploadingKeys;
            this.blobOutputStream = blob.getBlockBlobClient().
                    getBlobOutputStream(new BlockBlobOutputStreamOptions().
                            setHeaders(headers));
        }

        /**
         * Constructor for an instance that writes into the given temporary
         * blob. Upon closure, if the stream is {@link #isComplete()
         * completely written}, the temporary blob is copied into place and
         * deleted. Otherwise, the temporary blob is deleted.
         *
         * @param container        Container housing the blobs.
         * @param tempBlob         Temporary blob.
         * @param permanentBlobKey Key of the permanent blob.
         * @param uploadingKeys    All keys that are currently being uploaded
         *                         in any thread, including {@code
         *                         permanentBlobKey}, which {@link #close()}
         *                         will remove.
         */
        CustomBlobOutputStream(BlobContainerClient container,
                               BlobClient tempBlob,
                               BlobHttpHeaders headers,
                               String permanentBlobKey,
                               Set<String> uploadingKeys) {
            this.container        = container;
            this.blob             = tempBlob;
            this.blobKey          = permanentBlobKey;
            this.uploadingKeys    = uploadingKeys;
            this.blobOutputStream = blob.getBlockBlobClient().
                    getBlobOutputStream(new BlockBlobOutputStreamOptions().
                            setHeaders(headers));
        }

        @Override
        public void close() throws IOException {
            try {
                blobOutputStream.flush();
                blobOutputStream.close();
                if (container != null) {
                    if (isComplete()) {
                        // Copy the temporary blob into place.
                        BlobClient destBlob = container.getBlobClient(blobKey);
                        BlobSasPermission permission = new BlobSasPermission().
                                setReadPermission(true);
                        BlobServiceSasSignatureValues values =
                                new BlobServiceSasSignatureValues(
                                        OffsetDateTime.now().plusMinutes(5),
                                        permission);
                        String sourceUrl = blob.getBlobUrl() + "?" +
                                blob.generateSas(values);
                        destBlob.beginCopy(sourceUrl,
                                Duration.ofSeconds(1)).waitForCompletion();
                    }
                    blob.deleteIfExists();
                }
            } catch (RuntimeException e) {
                throw new IOException(e);
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

    private static final String INFO_EXTENSION = ".json";

    private static BlobServiceClient client;

    /**
     * Blob keys currently being written to Azure Storage from any thread.
     */
    private static final Set<String> uploadingKeys =
            new ConcurrentSkipListSet<>();

    static synchronized BlobServiceClient getClientInstance() {
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
                LOGGER.info("Using account: {}", accountName);
                client = new BlobServiceClientBuilder().
                        connectionString(connectionString).buildClient();
                client.getBlobContainerClient(getContainerName()).
                        createIfNotExists();
            } catch (RuntimeException e) {
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
    public Optional<Info> getInfo(Identifier identifier) throws IOException {
        final String containerName   = getContainerName();
        final BlobServiceClient client = getClientInstance();

        try {
            final Stopwatch watch = new Stopwatch();
            final BlobContainerClient container =
                    client.getBlobContainerClient(containerName);
            final String objectKey = getObjectKey(identifier);

            final BlobClient blob = container.getBlobClient(objectKey);
            if (blob.exists()) {
                if (isValid(blob)) {
                    try (InputStream is = blob.openInputStream()) {
                        Info info = Info.fromJSON(is);
                        // Populate the serialization timestamp if it is not
                        // already, as suggested by the method contract.
                        if (info.getSerializationTimestamp() == null) {
                            info.setSerializationTimestamp(
                                    blob.getProperties().getLastModified().toInstant());
                        }
                        LOGGER.debug("getInfo(): read {} from container {} in {}",
                                objectKey, containerName, watch);
                        return Optional.of(info);
                    }
                } else {
                    LOGGER.debug("getInfo(): deleting invalid item " +
                                    "asynchronously: {} in container {}",
                            objectKey, containerName);
                    purgeAsync(blob);
                }
            }
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        final String containerName = getContainerName();

        final BlobServiceClient client = getClientInstance();
        try {
            final BlobContainerClient container =
                    client.getBlobContainerClient(containerName);
            final String objectKey = getObjectKey(opList);

            LOGGER.debug("newDerivativeImageInputStream(): bucket: {}; key: {}",
                    containerName, objectKey);
            final BlobClient blob = container.getBlobClient(objectKey);
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
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList opList) throws IOException {
        final String objectKey = getObjectKey(opList);
        if (!uploadingKeys.contains(objectKey)) {
            uploadingKeys.add(objectKey);
            final String containerName   = getContainerName();
            final String tempObjectKey   = getTempObjectKey(opList);
            final BlobServiceClient client = getClientInstance();
            try {
                final BlobContainerClient container =
                        client.getBlobContainerClient(containerName);
                final BlobClient blob = container.getBlobClient(tempObjectKey);
                final BlobHttpHeaders headers = new BlobHttpHeaders().
                        setContentType(opList.getOutputFormat().
                                getPreferredMediaType().toString());
                return new CustomBlobOutputStream(
                        container, blob, headers, objectKey, uploadingKeys);
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return new CompletableNullOutputStream();
    }

    /**
     * @return Object key of the serialized {@link Info} associated with the
     *         given identifier.
     */
    String getObjectKey(Identifier identifier) {
        return getObjectKeyPrefix() + "info/" +
                StringUtils.md5(identifier.toString()) + INFO_EXTENSION;
    }

    /**
     * @return Object key of the derivative image associated with the given
     *         operation list.
     */
    String getObjectKey(OperationList opList) {
        final String idStr = StringUtils.md5(opList.getIdentifier().toString());
        final String opsStr = StringUtils.md5(opList.toString());

        String extension = "";
        Encode encode = (Encode) opList.getFirst(Encode.class);
        if (encode != null) {
            extension = "." + encode.getFormat().getPreferredExtension();
        }

        return String.format("%simage/%s/%s%s",
                getObjectKeyPrefix(), idStr, opsStr, extension);
    }

    /**
     * @return Value of {@link Key#AZURESTORAGECACHE_OBJECT_KEY_PREFIX}
     *         with trailing slash.
     */
    String getObjectKeyPrefix() {
        String prefix = Configuration.getInstance().
                getString(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX);
        if (prefix.isEmpty() || prefix.equals("/")) {
            return "";
        }
        return StringUtils.stripEnd(prefix, "/") + "/";
    }

    String getTempObjectKey(OperationList opList) {
        return getObjectKey(opList) + getTempObjectKeySuffix();
    }

    private String getTempObjectKeySuffix() {
        return "_" + Thread.currentThread().getName() + ".tmp";
    }

    private boolean isValid(BlobClient blob) {
        return blob.getProperties().getLastModified().toInstant().
                isAfter(getEarliestValidInstant());
    }

    @Override
    public void purge() throws IOException {
        final String containerName = getContainerName();

        final BlobServiceClient client = getClientInstance();

        try {
            final BlobContainerClient container =
                    client.getBlobContainerClient(containerName);
            int count = 0;
            for (BlobItem item : listBlobs(container, getObjectKeyPrefix())) {
                BlobClient blob = container.getBlobClient(item.getName());
                if (blob.deleteIfExists()) {
                    count++;
                }
            }
            LOGGER.debug("purge(): deleted {} items", count);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(OperationList opList) throws IOException {
        final String containerName = getContainerName();

        final BlobServiceClient client = getClientInstance();
        final String objectKey = getObjectKey(opList);

        try {
            final BlobContainerClient container =
                    client.getBlobContainerClient(containerName);
            final BlobClient blob = container.getBlobClient(objectKey);
            blob.deleteIfExists();
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void purgeAsync(BlobClient blob) {
        TaskQueue.getInstance().submit(() -> {
            LOGGER.debug("purgeAsync(): {}", blob);
            try {
                blob.deleteIfExists();
            } catch (RuntimeException e) {
                LOGGER.warn("purgeAsync(): failed to delete {}: {}",
                        blob, e.getMessage());
            }
        });
    }

    @Override
    public void purgeInfos() throws IOException {
        final String containerName   = getContainerName();
        final BlobServiceClient client = getClientInstance();
        try {
            final BlobContainerClient container =
                    client.getBlobContainerClient(containerName);
            int count = 0, deletedCount = 0;
            for (BlobItem item : listBlobs(container, getObjectKeyPrefix())) {
                BlobClient blob = container.getBlobClient(item.getName());
                count++;
                if (blob.getBlobName().endsWith(INFO_EXTENSION)) {
                    if (blob.deleteIfExists()) {
                        deletedCount++;
                    }
                }
            }
            LOGGER.debug("purgeInfos(): deleted {} of {} items",
                    deletedCount, count);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void purgeInvalid() throws IOException {
        final String containerName = getContainerName();
        final BlobServiceClient client = getClientInstance();

        try {
            final BlobContainerClient container =
                    client.getBlobContainerClient(containerName);
            int count = 0, deletedCount = 0;
            for (BlobItem item : listBlobs(container, getObjectKeyPrefix())) {
                BlobClient blob = container.getBlobClient(item.getName());
                count++;
                if (!isValid(blob)) {
                    if (blob.deleteIfExists()) {
                        deletedCount++;
                    }
                }
            }
            LOGGER.debug("purgeInvalid(): deleted {} of {} items",
                    deletedCount, count);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(Identifier identifier) throws IOException {
        try {
            final BlobServiceClient client = getClientInstance();
            final String containerName   = getContainerName();
            final BlobContainerClient container =
                    client.getBlobContainerClient(containerName);
            int count = 0;

            // purge the info
            BlobClient blob = container.getBlobClient(getObjectKey(identifier));
            if (blob.deleteIfExists()) {
                count++;
            }

            // purge images
            final String prefix = getObjectKeyPrefix() + "image/" +
                    StringUtils.md5(identifier.toString());
            for (BlobItem item : listBlobs(container, prefix)) {
                BlobClient cblob = container.getBlobClient(item.getName());
                LOGGER.trace("purge(Identifier): deleting {}",
                        cblob.getBlobName());
                if (cblob.deleteIfExists()) {
                    count++;
                }
            }
            LOGGER.debug("purge(Identifier): deleted {} items", count);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void put(Identifier identifier, Info info) throws IOException {
        if (!info.isPersistable()) {
            LOGGER.debug("put(): info for {} is incomplete; ignoring",
                    identifier);
            return;
        }
        LOGGER.debug("put(): caching info for {}", identifier);
        final String objectKey = getObjectKey(identifier);
        if (!uploadingKeys.contains(objectKey)) {
            uploadingKeys.add(objectKey);
            try {
                final String containerName = getContainerName();
                final BlobServiceClient client = getClientInstance();
                final BlobContainerClient container =
                        client.getBlobContainerClient(containerName);
                final BlobClient blob = container.getBlobClient(objectKey);
                final BlobHttpHeaders headers = new BlobHttpHeaders().
                        setContentType("application/json").
                        setContentEncoding("UTF-8");

                // writeAsJSON() will close this.
                CustomBlobOutputStream os = new CustomBlobOutputStream(
                        blob, headers, uploadingKeys);
                info.writeAsJSON(os);
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    @Override
    public void put(Identifier identifier, String info) throws IOException {
        LOGGER.debug("put(): caching info for {}", identifier);
        final String objectKey = getObjectKey(identifier);
        if (!uploadingKeys.contains(objectKey)) {
            uploadingKeys.add(objectKey);
            try {
                final String containerName = getContainerName();
                final BlobServiceClient client = getClientInstance();
                final BlobContainerClient container =
                        client.getBlobContainerClient(containerName);
                final BlobClient blob = container.getBlobClient(objectKey);
                final BlobHttpHeaders headers = new BlobHttpHeaders().
                        setContentType("application/json").
                        setContentEncoding("UTF-8");

                CustomBlobOutputStream os =
                        new CustomBlobOutputStream(blob, headers, uploadingKeys);
                try (OutputStreamWriter writer = new OutputStreamWriter(os)) {
                    writer.write(info);
                }
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    private Iterable<BlobItem> listBlobs(BlobContainerClient container,
                                         String prefix) {
        return container.listBlobs(
                new ListBlobsOptions().setPrefix(prefix), null);
    }

}
