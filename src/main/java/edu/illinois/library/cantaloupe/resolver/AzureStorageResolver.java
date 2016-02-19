package edu.illinois.library.cantaloupe.resolver;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.security.InvalidKeyException;

/**
 * @see <a href="https://github.com/azure/azure-storage-java">
 *     Microsoft Azure Storage DSK for Java</a>
 */
class AzureStorageResolver extends AbstractResolver implements StreamResolver {

    private static class AzureStorageStreamSource implements StreamSource {

        private final CloudBlockBlob blob;

        public AzureStorageStreamSource(CloudBlockBlob blob) {
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

    public static final String ACCOUNT_KEY_CONFIG_KEY =
            "AzureStorageResolver.account_key";
    public static final String ACCOUNT_NAME_CONFIG_KEY =
            "AzureStorageResolver.account_name";
    public static final String CONTAINER_NAME_CONFIG_KEY =
            "AzureStorageResolver.container_name";
    public static final String LOOKUP_STRATEGY_CONFIG_KEY =
            "AzureStorageResolver.lookup_strategy";

    private static CloudBlobClient client;

    private static CloudBlobClient getClientInstance() {
        if (client == null) {
            try {
                final Configuration config = Application.getConfiguration();
                final String accountName = config.getString(ACCOUNT_NAME_CONFIG_KEY);
                final String accountKey = config.getString(ACCOUNT_KEY_CONFIG_KEY);

                final String connectionString = String.format(
                        "DefaultEndpointsProtocol=https;" +
                                "AccountName=%s;" +
                                "AccountKey=%s", accountName, accountKey);
                final CloudStorageAccount account = CloudStorageAccount.parse(connectionString);

                logger.info("Using account: {}", accountName);

                client = account.createCloudBlobClient();
            } catch (URISyntaxException | InvalidKeyException e) {
                logger.error(e.getMessage());
            }
        }
        return client;
    }

    @Override
    public StreamSource getStreamSource(Identifier identifier)
            throws IOException {
        return new AzureStorageStreamSource(getObject(identifier));
    }

    private CloudBlockBlob getObject(Identifier identifier) throws IOException {
        final Configuration config = Application.getConfiguration();
        final String containerName = config.getString(CONTAINER_NAME_CONFIG_KEY);
        logger.info("Using container: {}", containerName);

        final CloudBlobClient client = getClientInstance();
        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final String objectKey = getObjectKey(identifier);

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

    private String getObjectKey(Identifier identifier) throws IOException {
        final Configuration config = Application.getConfiguration();
        switch (config.getString(LOOKUP_STRATEGY_CONFIG_KEY)) {
            case "BasicLookupStrategy":
                return identifier.toString();
            case "ScriptLookupStrategy":
                try {
                    return getObjectKeyWithDelegateStrategy(identifier);
                } catch (ScriptException | DelegateScriptDisabledException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                throw new IOException(LOOKUP_STRATEGY_CONFIG_KEY +
                        " is invalid or not set");
        }
    }

    /**
     * @param identifier
     * @return
     * @throws FileNotFoundException If the delegate script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     */
    private String getObjectKeyWithDelegateStrategy(Identifier identifier)
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String method = "get_azure_storage_blob_key";
        final String[] args = { identifier.toString() };
        final Object result = engine.invoke(method, args);
        if (result == null) {
            throw new FileNotFoundException(method + " returned nil for " +
                    identifier);
        }
        return (String) result;
    }

    @Override
    public Format getSourceFormat(Identifier identifier) throws IOException {
        final CloudBlockBlob blob = getObject(identifier);
        final String contentType = blob.getProperties().getContentType();
        if (contentType != null) {
            final Format format = Format.getFormat(contentType);
            if (format != null && !format.equals(Format.UNKNOWN)) {
                return format;
            }
        }
        return Format.getFormat(identifier);
    }

}
