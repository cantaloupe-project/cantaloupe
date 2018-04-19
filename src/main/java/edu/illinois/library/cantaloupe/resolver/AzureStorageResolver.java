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
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;

/**
 * <p>Maps an identifier to a
 * <a href="https://azure.microsoft.com/en-us/services/storage/">Microsoft
 * Azure Storage</a> blob, for retrieving images from Azure Storage.</p>
 *
 * <h3>Lookup Strategies</h3>
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

    private static CloudBlobClient client;

    private CloudBlockBlob cachedBlob;
    private IOException cachedBlobException;
    private String objectKey;

    private static synchronized CloudBlobClient getClientInstance() {
        if (client == null) {
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
                final CloudStorageAccount account =
                        CloudStorageAccount.parse(connectionString);

                LOGGER.info("Using account: {}", accountName);

                client = account.createCloudBlobClient();
            } catch (URISyntaxException | InvalidKeyException e) {
                LOGGER.error(e.getMessage());
            }
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

                final CloudBlobClient client = getClientInstance();
                try {
                    final CloudBlobContainer container =
                            client.getContainerReference(containerName);
                    final String objectKey = getObjectKey();

                    LOGGER.info("Requesting {}", objectKey);
                    final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
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
                    LookupStrategy.from(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY);
            switch (strategy) {
                case DELEGATE_SCRIPT:
                    try {
                        objectKey = getObjectKeyWithDelegateStrategy();
                    } catch (ScriptException | DelegateScriptDisabledException e) {
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
     * @return
     * @throws NoSuchFileException If the delegate script does not exist.
     * @throws IOException
     * @throws ScriptException If the delegate method throws an exception.
     */
    private String getObjectKeyWithDelegateStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_KEY_DELEGATE_METHOD,
                identifier.toString(), context.asMap());
        if (result == null) {
            throw new NoSuchFileException(GET_KEY_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return (String) result;
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            // Try to infer a format based on the objectKey.
            sourceFormat = Format.inferFormat(getObjectKey());

            if (sourceFormat.equals(Format.UNKNOWN)) {
                // Try to infer a format based on the identifier.
                sourceFormat = Format.inferFormat(identifier);
            }

            if (sourceFormat.equals(Format.UNKNOWN)) {
                // Try to infer the format from the Content-Type header.
                final CloudBlockBlob blob = getObject();
                final String contentType = blob.getProperties().getContentType();
                if (contentType != null) {
                    sourceFormat = new MediaType(contentType).toFormat();
                }
            }
        }
        return sourceFormat;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        return new AzureStorageStreamSource(getObject());
    }

}
