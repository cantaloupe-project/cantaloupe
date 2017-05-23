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

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
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

    private static Logger logger = LoggerFactory.
            getLogger(AzureStorageResolver.class);

    static final String GET_KEY_DELEGATE_METHOD =
            "AzureStorageResolver::get_blob_key";

    private static CloudBlobClient client;

    /** Lock object for synchronization */
    private static final Object lock = new Object();

    private static CloudBlobClient getClientInstance() {
        if (client == null) {
            synchronized (lock) {
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

                    logger.info("Using account: {}", accountName);

                    client = account.createCloudBlobClient();
                } catch (URISyntaxException | InvalidKeyException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return client;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        return new AzureStorageStreamSource(getObject());
    }

    private CloudBlockBlob getObject() throws IOException {
        final Configuration config = Configuration.getInstance();
        final String containerName =
                config.getString(Key.AZURESTORAGERESOLVER_CONTAINER_NAME);
        logger.info("Using container: {}", containerName);

        final CloudBlobClient client = getClientInstance();
        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final String objectKey = getObjectKey();

            logger.info("Requesting {}", objectKey);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            if (!blob.exists()) {
                throw new FileNotFoundException("Not found: " + objectKey);
            }
            return blob;
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e);
        }
    }

    private String getObjectKey() throws IOException {
        final Configuration config = Configuration.getInstance();
        switch (config.getString(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY)) {
            case "BasicLookupStrategy":
                return identifier.toString();
            case "ScriptLookupStrategy":
                try {
                    return getObjectKeyWithDelegateStrategy();
                } catch (ScriptException | DelegateScriptDisabledException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                throw new IOException(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY +
                        " is invalid or not set");
        }
    }

    /**
     * @return
     * @throws FileNotFoundException If the delegate script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     */
    private String getObjectKeyWithDelegateStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_KEY_DELEGATE_METHOD,
                identifier.toString());
        if (result == null) {
            throw new FileNotFoundException(GET_KEY_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return (String) result;
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
        }
        return sourceFormat;
    }

}
