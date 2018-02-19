package edu.illinois.library.cantaloupe.resolver;

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

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;

/**
 * <p>Maps an identifier to a
 * <a href="https://azure.microsoft.com/en-us/services/storage/">Microsoft
 * Azure Storage</a> blob, for retrieving images from Azure Storage.</p>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#AZURESTORAGERESOLVER_LOOKUP_STRATEGY}. BasicLookupStrategy maps
 * identifiers directly to blob keys. ScriptLookupStrategy invokes a delegate
 * method to retrieve blob keys dynamically.</p>
 *
 * @see <a href="https://github.com/azure/azure-storage-java">
 *     Microsoft Azure Storage DSK for Java</a>
 */
class AzureStorageResolver extends AbstractResolver implements StreamResolver {

    private static class AzureStorageStreamSource implements StreamSource {

        private final CloudBlockBlob blob;

        AzureStorageStreamSource(CloudBlockBlob blob) {
            this.blob = blob;
        }

        @Override
        public ImageInputStream newImageInputStream() throws IOException {
            return ImageIO.createImageInputStream(newInputStream());
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

    private static final Logger LOGGER = LoggerFactory.
            getLogger(AzureStorageResolver.class);

    private static final String GET_KEY_DELEGATE_METHOD =
            "AzureStorageResolver::get_blob_key";

    private static CloudStorageAccount account;
    private static CloudBlobClient client;

    private CloudBlockBlob cachedBlob;
    private IOException cachedBlobException;

    static synchronized CloudStorageAccount getAccount() {
        if (account == null) {
            try {
                final Configuration config = Configuration.getInstance();
                final String accountName =
                        config.getString(Key.AZURESTORAGERESOLVER_ACCOUNT_NAME);
                final String accountKey =
                        config.getString(Key.AZURESTORAGERESOLVER_ACCOUNT_KEY);

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
                        config.getString(Key.AZURESTORAGERESOLVER_CONTAINER_NAME);
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
        final LookupStrategy strategy =
                LookupStrategy.from(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY);
        switch (strategy) {
            case DELEGATE_SCRIPT:
                try {
                    return getObjectKeyWithDelegateStrategy();
                } catch (ScriptException e) {
                    LOGGER.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                return identifier.toString();
        }
    }

    /**
     * @throws NoSuchFileException if the delegate script does not exist.
     * @throws ScriptException     if the delegate method throws an exception.
     */
    private String getObjectKeyWithDelegateStrategy()
            throws NoSuchFileException, ScriptException {
        final String key = getDelegateProxy().getAzureStorageResolverBlobKey();

        if (key == null) {
            throw new NoSuchFileException(GET_KEY_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return key;
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            final CloudBlockBlob blob = getObject();
            final String contentType = blob.getProperties().getContentType();
            // See if we can determine the format from the Content-Type header.
            if (contentType != null) {
                sourceFormat = new MediaType(contentType).toFormat();
            }
            if (sourceFormat == null || sourceFormat.equals(Format.UNKNOWN)) {
                // Try to infer a format based on the identifier.
                sourceFormat = Format.inferFormat(identifier);
            }
            if (Format.UNKNOWN.equals(sourceFormat)) {
                // Try to infer a format based on the objectKey.
                sourceFormat = Format.inferFormat(getObjectKey());
            }
        }
        return sourceFormat;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        return new AzureStorageStreamSource(getObject());
    }

}
