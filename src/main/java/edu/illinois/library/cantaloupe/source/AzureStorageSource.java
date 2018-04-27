package edu.illinois.library.cantaloupe.source;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.util.List;

/**
 * <p>Maps an identifier to a
 * <a href="https://azure.microsoft.com/en-us/services/storage/">Microsoft
 * Azure Storage</a> blob, for retrieving images from Azure Storage.</p>
 *
 * <h1>Format Inference</h1>
 *
 * <ol>
 *     <li>If the blob key has a recognized filename extension, the format will
 *     be inferred from that.</li>
 *     <li>Otherwise, if the source image's URI identifier has a recognized
 *     filename extension, the format will be inferred from that.</li>
 *     <li>Otherwise, a {@literal HEAD} request will be sent. If a {@literal
 *     Content-Type} header is present in the response, and is specific enough
 *     (i.e. not {@literal application/octet-stream}), a format will be
 *     inferred from that.</li>
 *     <li>Otherwise, a {@literal GET} request will be sent with a {@literal
 *     Range} header specifying a small range of data from the beginning of the
 *     resource, and a format will be inferred from the magic bytes in the
 *     response body.</li>
 * </ol>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#AZURESTORAGESOURCE_LOOKUP_STRATEGY}. BasicLookupStrategy maps
 * identifiers directly to blob keys. ScriptLookupStrategy invokes a delegate
 * method to retrieve blob keys dynamically.</p>
 *
 * @see <a href="https://github.com/azure/azure-storage-java">
 *     Microsoft Azure Storage DSK for Java</a>
 */
class AzureStorageSource extends AbstractSource implements StreamSource {

    private static class AzureStorageStreamFactory implements StreamFactory {

        private final CloudBlockBlob blob;

        AzureStorageStreamFactory(CloudBlockBlob blob) {
            this.blob = blob;
        }

        @Override
        public BlobInputStream newInputStream() throws IOException {
            try {
                return blob.openInputStream();
            } catch (StorageException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AzureStorageSource.class);

    /**
     * Byte length of the range used to infer the source image format.
     */
    private static final int FORMAT_INFERENCE_RANGE_LENGTH = 32;

    private static final String GET_KEY_DELEGATE_METHOD =
            "AzureStorageSource::get_blob_key";

    private static CloudStorageAccount account;
    private static CloudBlobClient client;

    private CloudBlockBlob cachedBlob;
    private IOException cachedBlobException;
    private String objectKey;

    static synchronized CloudStorageAccount getAccount() {
        if (account == null) {
            try {
                final Configuration config = Configuration.getInstance();
                final String accountName =
                        config.getString(Key.AZURESTORAGESOURCE_ACCOUNT_NAME);
                final String accountKey =
                        config.getString(Key.AZURESTORAGESOURCE_ACCOUNT_KEY);

                final String connectionString = String.format(
                        "DefaultEndpointsProtocol=https;" +
                                "AccountName=%s;" +
                                "AccountKey=%s", accountName, accountKey);
                account = CloudStorageAccount.parse(connectionString);

                LOGGER.info("Using account: {}", accountName);
            } catch (URISyntaxException | InvalidKeyException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return account;
    }

    private static synchronized CloudBlobClient getClientInstance() {
        if (client == null) {
            client = getAccount().createCloudBlobClient();
        }
        return client;
    }

    @Override
    public void checkAccess() throws IOException {
        getObject();
    }

    private CloudBlockBlob getObject() throws IOException {
        if (cachedBlobException != null) {
            throw cachedBlobException;
        } else if (cachedBlob == null) {
            try {
                final Configuration config = Configuration.getInstance();
                final String containerName =
                        config.getString(Key.AZURESTORAGESOURCE_CONTAINER_NAME);
                LOGGER.info("Using container: {}", containerName);

                try {
                    final CloudBlockBlob blob;
                    final String objectKey = getObjectKey();
                    //Add support for direct URI references.  See: https://docs.microsoft.com/en-us/rest/api/storageservices/naming-and-referencing-containers--blobs--and-metadata#resource-uri-syntax
                    //Add support for SAS Token Authentication.  See: https://docs.microsoft.com/en-us/azure/storage/common/storage-dotnet-shared-access-signature-part-1#how-a-shared-access-signature-works
                    if(StringUtils.isEmpty(containerName)) {  //use URI with sas token + container + path directly
                        final URI uri = URI.create(objectKey);
                        LOGGER.info("Using full uri: {}", uri);
                        LOGGER.info("Requesting {}", objectKey);
                        blob = new CloudBlockBlob(uri);
                    } else {  //use a fixed storage account with fixed container.
                        final CloudBlobClient client = getClientInstance();
                        final CloudBlobContainer container;
                        LOGGER.info("Using account with fixed container: {}", containerName);
                        container = client.getContainerReference(containerName);
                        LOGGER.info("Requesting {}", objectKey);
                        blob = container.getBlockBlobReference(objectKey);
                    }

                    if (!blob.exists()) {
                        throw new NoSuchFileException("Not found: " + objectKey);
                    }
                    cachedBlob = blob;
                } catch (URISyntaxException | StorageException e) {
                    throw new IOException(e);
                }
            } catch (IOException e) {
                cachedBlobException = e;
                throw e;
            }
        }
        return cachedBlob;
    }

    private String getObjectKey() throws IOException {
        if (objectKey == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.AZURESTORAGESOURCE_LOOKUP_STRATEGY);
            switch (strategy) {
                case DELEGATE_SCRIPT:
                    try {
                        objectKey = getObjectKeyWithDelegateStrategy();
                    } catch (ScriptException e) {
                        LOGGER.error(e.getMessage(), e);
                        throw new IOException(e);
                    }
                default:
                    objectKey = identifier.toString();
            }
        }
        return objectKey;
    }

    /**
     * @throws NoSuchFileException if the delegate script does not exist.
     * @throws ScriptException     if the delegate method throws an exception.
     */
    private String getObjectKeyWithDelegateStrategy()
            throws NoSuchFileException, ScriptException {
        final String key = getDelegateProxy().getAzureStorageSourceBlobKey();

        if (key == null) {
            throw new NoSuchFileException(GET_KEY_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return key;
    }

    @Override
    public Format getFormat() throws IOException {
        if (sourceFormat == null) {
            final String key = getObjectKey();

            // Try to infer a format based on the object key.
            LOGGER.debug("Inferring format from the object key for {}", key);
            sourceFormat = Format.inferFormat(key);

            if (Format.UNKNOWN.equals(sourceFormat)) {
                // Try to infer a format based on the identifier.
                LOGGER.debug("Inferring format from the identifier for {}", key);
                sourceFormat = Format.inferFormat(identifier);
            }

            if (Format.UNKNOWN.equals(sourceFormat)) {
                // Try to infer the format from the Content-Type header.
                LOGGER.debug("Inferring format from the Content-Type header for {}",
                        key);
                final CloudBlockBlob blob = getObject();
                final String contentType = blob.getProperties().getContentType();
                if (contentType != null && !contentType.isEmpty()) {
                    sourceFormat = new MediaType(contentType).toFormat();
                }

                if (Format.UNKNOWN.equals(sourceFormat)) {
                    // Try to infer a format from the object's magic bytes.
                    LOGGER.debug("Inferring format from magic bytes for {}",
                            key);
                    try {
                        byte[] bytes = new byte[FORMAT_INFERENCE_RANGE_LENGTH];
                        blob.downloadRangeToByteArray(
                                0, (long) FORMAT_INFERENCE_RANGE_LENGTH, bytes, 0);

                        try (InputStream is = new ByteArrayInputStream(bytes)) {
                            List<MediaType> types = MediaType.detectMediaTypes(is);
                            if (!types.isEmpty()) {
                                sourceFormat = types.get(0).toFormat();
                            }
                        }
                    } catch (StorageException e) {
                        throw new IOException(e);
                    }
                }
            }
        }
        return sourceFormat;
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        return new AzureStorageStreamFactory(getObject());
    }

}
