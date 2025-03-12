package edu.illinois.library.cantaloupe.source;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.delegate.DelegateMethod;
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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>Maps an identifier to a
 * <a href="https://azure.microsoft.com/en-us/services/storage/">Microsoft
 * Azure Storage</a> blob, for retrieving images from Azure Storage.</p>
 *
 * <h1>Format Inference</h1>
 *
 * <p>See {@link FormatIterator}.</p>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#AZURESTORAGESOURCE_LOOKUP_STRATEGY}. BasicLookupStrategy maps
 * identifiers directly to blob keys. ScriptLookupStrategy invokes a delegate
 * method to retrieve blob keys dynamically.</p>
 *
 * <h1>Resource Access</h1>
 *
 * <p>While proceeding through the client request fulfillment flow, the
 * following server requests are sent:</p>
 *
 * <ol>
 *     <li>{@literal HEAD}</li>
 *     <li>
 *         <ol>
 *             <li>If the return value of {@link #getFormatIterator()} needs to
 *             check magic bytes:
 *                 <ol>
 *                     <li>Ranged {@literal GET}</li>
 *                 </ol>
 *             </li>
 *             <li>If {@link StreamFactory#newSeekableStream()} is used:
 *                 <ol>
 *                     <li>A series of ranged {@literal GET} requests (see {@link
 *                     edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream}
 *                     for details)</li>
 *                 </ol>
 *             </li>
 *             <li>Else if {@link StreamFactory#newInputStream()} is used:
 *                 <ol>
 *                     <li>{@literal GET} to retrieve the full image bytes</li>
 *                 </ol>
 *             </li>
 *         </ol>
 *     </li>
 * </ol>
 *
 * @see <a href="https://github.com/azure/azure-storage-java">
 *     Microsoft Azure Storage SDK for Java</a>
 */
final class AzureStorageSource extends AbstractSource implements Source {

    /**
     * <ol>
     *     <li>If the blob key has a recognized filename extension, the format
     *     is inferred from that.</li>
     *     <li>Otherwise, if the source image's URI identifier has a recognized
     *     filename extension, the format is inferred from that.</li>
     *     <li>Otherwise, a {@literal HEAD} request will be sent. If a {@literal
     *     Content-Type} header is present in the response, and is specific
     *     enough (i.e. not {@literal application/octet-stream}), a format is
     *     inferred from that.</li>
     *     <li>Otherwise, a {@literal GET} request will be sent with a {@literal
     *     Range} header specifying a small range of data from the beginning of
     *     the resource, and a format is inferred from the magic bytes in the
     *     response body.</li>
     * </ol>
     *
     * @param <T> {@link Format}.
     */
    class FormatIterator<T> implements Iterator<T> {

        /**
         * Infers a {@link Format} based on the media type in a {@literal
         * Content-Type} header.
         */
        private class ContentTypeHeaderChecker implements FormatChecker {
            @Override
            public Format check() throws IOException {
                final CloudBlockBlob blob = fetchBlob();
                final String contentType  = blob.getProperties().getContentType();
                if (contentType != null && !contentType.isEmpty()) {
                    return new MediaType(contentType).toFormat();
                }
                return Format.UNKNOWN;
            }
        }

        /**
         * Infers a {@link Format} based on image magic bytes.
         */
        private class ByteChecker implements FormatChecker {
            @Override
            public Format check() throws IOException {
                try {
                    byte[] bytes = new byte[FORMAT_INFERENCE_RANGE_LENGTH];
                    fetchBlob().downloadRangeToByteArray(
                            0, (long) FORMAT_INFERENCE_RANGE_LENGTH, bytes, 0);

                    try (InputStream is = new ByteArrayInputStream(bytes)) {
                        List<MediaType> types = MediaType.detectMediaTypes(is);
                        if (!types.isEmpty()) {
                            return types.get(0).toFormat();
                        }
                    }
                } catch (StorageException e) {
                    throw new IOException(e);
                }
                return Format.UNKNOWN;
            }
        }

        private FormatChecker formatChecker;

        @Override
        public boolean hasNext() {
            return (formatChecker == null ||
                    formatChecker instanceof NameFormatChecker ||
                    formatChecker instanceof IdentifierFormatChecker ||
                    formatChecker instanceof FormatIterator.ContentTypeHeaderChecker);
        }

        @Override
        public T next() {
            if (formatChecker == null) {
                try {
                    formatChecker = new NameFormatChecker(getBlobKey());
                } catch (IOException e) {
                    LOGGER.warn("FormatIterator.next(): {}", e.getMessage(), e);
                    formatChecker = new NameFormatChecker("***BOGUS***");
                    return next();
                }
            } else if (formatChecker instanceof NameFormatChecker) {
                formatChecker = new IdentifierFormatChecker(getIdentifier());
            } else if (formatChecker instanceof IdentifierFormatChecker) {
                formatChecker = new ContentTypeHeaderChecker();
            } else if (formatChecker instanceof FormatIterator.ContentTypeHeaderChecker) {
                formatChecker = new ByteChecker();
            } else {
                throw new NoSuchElementException();
            }
            try {
                //noinspection unchecked
                return (T) formatChecker.check();
            } catch (IOException e) {
                LOGGER.warn("Error checking format: {}", e.getMessage());
                //noinspection unchecked
                return (T) Format.UNKNOWN;
            }
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AzureStorageSource.class);

    /**
     * Byte length of the range used to infer the source image format.
     */
    private static final int FORMAT_INFERENCE_RANGE_LENGTH = 32;

    private static CloudStorageAccount account;
    private static CloudBlobClient client;

    private CloudBlockBlob cachedBlob;
    private IOException cachedBlobException;
    private String objectKey;

    private FormatIterator<Format> formatIterator = new FormatIterator<>();

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
    public StatResult stat() throws IOException {
        CloudBlockBlob blob = fetchBlob();
        StatResult result = new StatResult();
        result.setLastModified(blob.getProperties().getLastModified().toInstant());
        return result;
    }

    private CloudBlockBlob fetchBlob() throws IOException {
        if (cachedBlobException != null) {
            throw cachedBlobException;
        } else if (cachedBlob == null) {
            try {
                final String containerName = getContainerName();
                LOGGER.debug("Using container: {}", containerName);

                final CloudBlockBlob blob;
                final String objectKey = getBlobKey();
                // Supports direct URI references:
                // https://docs.microsoft.com/en-us/rest/api/storageservices/naming-and-referencing-containers--blobs--and-metadata#resource-uri-syntax
                // Supports SAS Token Authentication:
                // https://docs.microsoft.com/en-us/azure/storage/common/storage-dotnet-shared-access-signature-part-1#how-a-shared-access-signature-works
                if (containerName.isEmpty()) { // use URI with sas token + container + path directly
                    final URI uri = URI.create(objectKey);
                    LOGGER.debug("Requesting {} from {}", objectKey, uri);
                    blob = new CloudBlockBlob(uri);
                } else { // use a fixed storage account with fixed container.
                    final CloudBlobClient client = getClientInstance();
                    final CloudBlobContainer container =
                            client.getContainerReference(containerName);
                    LOGGER.debug("Requesting {} from fixed container {}",
                            objectKey, containerName);
                    blob = container.getBlockBlobReference(objectKey);
                }

                if (!blob.exists()) {
                    throw new NoSuchFileException("Not found: " + objectKey);
                }
                cachedBlob = blob;
            } catch (URISyntaxException | StorageException e) {
                throw new IOException(e);
            } catch (IOException e) {
                cachedBlobException = e;
                throw e;
            }
        }
        return cachedBlob;
    }

    String getBlobKey() throws IOException {
        if (objectKey == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.AZURESTORAGESOURCE_LOOKUP_STRATEGY);
            //noinspection SwitchStatementWithTooFewBranches
            switch (strategy) {
                case DELEGATE_SCRIPT:
                    try {
                        objectKey = getBlobKeyWithDelegateStrategy();
                    } catch (ScriptException e) {
                        LOGGER.error(e.getMessage(), e);
                        throw new IOException(e);
                    }
                    break;
                default:
                    objectKey = identifier.toString();
                    break;
            }
        }
        return objectKey;
    }

    private String getContainerName() {
        var config = Configuration.getInstance();
        return config.getString(Key.AZURESTORAGESOURCE_CONTAINER_NAME);
    }

    /**
     * @throws NoSuchFileException if the delegate script does not exist.
     * @throws ScriptException     if the delegate method throws an exception.
     */
    private String getBlobKeyWithDelegateStrategy()
            throws NoSuchFileException, ScriptException {
        final String key = getDelegateProxy().getAzureStorageSourceBlobKey();
        if (key == null) {
            throw new NoSuchFileException(
                    DelegateMethod.AZURESTORAGESOURCE_BLOB_KEY +
                    " returned nil for " + identifier);
        }
        return key;
    }

    @Override
    public FormatIterator<Format> getFormatIterator() {
        return formatIterator;
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        return new AzureStorageStreamFactory(fetchBlob());
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        super.setIdentifier(identifier);
        reset();
    }

    private void reset() {
        formatIterator = new FormatIterator<>();
    }

}
