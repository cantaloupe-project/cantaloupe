package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.delegate.DelegateMethod;
import edu.illinois.library.cantaloupe.util.S3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.script.ScriptException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * <p>Maps an identifier to an <a href="https://aws.amazon.com/s3/">Amazon
 * Simple Storage Service (S3)</a> object, for retrieving images from S3.</p>
 *
 * <h1>Format Inference</h1>
 *
 * <p>See {@link FormatIterator}.</p>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#S3SOURCE_LOOKUP_STRATEGY}. BasicLookupStrategy maps
 * identifiers directly to S3 object keys. ScriptLookupStrategy invokes a
 * delegate method to retrieve object keys dynamically.</p>
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
 *             <li>If {@link FormatIterator#next()} needs to check magic bytes:
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
 * @author Alex Dolski UIUC
 */
final class S3Source extends AbstractSource implements Source {

    private static class S3ObjectAttributes {
        String contentType;
        Instant lastModified;
        long length;
    }

    /**
     * <ol>
     *     <li>If the object key has a recognized filename extension, the
     *     format is inferred from that.</li>
     *     <li>Otherwise, if the source image's URI identifier has a recognized
     *     filename extension, the format will be inferred from that.</li>
     *     <li>Otherwise, a {@literal GET} request will be sent with a
     *     {@literal Range} header specifying a small range of data from the
     *     beginning of the resource.
     *         <ol>
     *             <li>If a {@literal Content-Type} header is present in the
     *             response, and is specific enough (i.e. not {@literal
     *             application/octet-stream}), a format will be inferred from
     *             that.</li>
     *             <li>Otherwise, a format is inferred from the magic bytes in
     *             the response body.</li>
     *         </ol>
     *     </li>
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
                String contentType = getObjectAttributes().contentType;
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
                try (InputStream is = new BufferedInputStream(
                        newObjectInputStream(getObjectInfo(), FORMAT_INFERENCE_RANGE))) {
                    List<MediaType> types = MediaType.detectMediaTypes(is);
                    if (!types.isEmpty()) {
                        return types.get(0).toFormat();
                    }
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
                    formatChecker = new NameFormatChecker(getObjectInfo().getKey());
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
            LoggerFactory.getLogger(S3Source.class);

    /**
     * Byte range used to infer the source image format.
     */
    private static final Range FORMAT_INFERENCE_RANGE = new Range(0, 32);

    /**
     * The keys are endpoint URIs. The default client's key is {@code null}.
     * This is not thread-safe, so should only be accessed via {@link
     * #getClientInstance(S3ObjectInfo)}.
     */
    private static final Map<String,S3Client> CLIENTS = new HashMap<>();

    /**
     * Cached by {@link #getObjectInfo()}.
     */
    private S3ObjectInfo objectInfo;

    /**
     * Cached by {@link #getObjectAttributes()}.
     */
    private S3ObjectAttributes objectAttributes;

    private FormatIterator<Format> formatIterator = new FormatIterator<>();

    static synchronized S3Client getClientInstance(S3ObjectInfo info) {
        String endpoint = info.getEndpoint();
        S3Client client = CLIENTS.get(endpoint);
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            if (endpoint == null) {
                endpoint = config.getString(Key.S3SOURCE_ENDPOINT);
            }
            // Convert the endpoint string into a URI which is required by the
            // client builder.
            URI endpointURI = null;
            if (endpoint != null) {
                try {
                    endpointURI = new URI(endpoint);
                } catch (URISyntaxException e) {
                    LOGGER.error("Invalid URI for {}: {}",
                            Key.S3SOURCE_ENDPOINT, e.getMessage());
                }
            }
            String region = info.getRegion();
            if (region == null) {
                region = config.getString(Key.S3SOURCE_REGION);
            }
            String accessKeyID = info.getAccessKeyID();
            if (accessKeyID == null) {
                accessKeyID = config.getString(Key.S3SOURCE_ACCESS_KEY_ID);
            }
            String secretAccessKey = info.getSecretAccessKey();
            if (secretAccessKey == null) {
                secretAccessKey = config.getString(Key.S3SOURCE_SECRET_KEY);
            }
            client = new S3ClientBuilder()
                    .accessKeyID(accessKeyID)
                    .secretAccessKey(secretAccessKey)
                    .endpointURI(endpointURI)
                    .region(region)
                    .build();
            CLIENTS.put(endpoint, client);
        }
        return client;
    }

    /**
     * Fetches a whole object.
     *
     * @param info Object info.
     */
    static InputStream newObjectInputStream(S3ObjectInfo info)
            throws IOException {
        return newObjectInputStream(info, null);
    }

    /**
     * Fetches a byte range of an object.
     *
     * @param info  Object info.
     * @param range Byte range. May be {@code null}.
     */
    static InputStream newObjectInputStream(S3ObjectInfo info,
                                            Range range) throws IOException {
        final S3Client client = getClientInstance(info);
        try {
            GetObjectRequest request;
            if (range != null) {
                LOGGER.debug("Requesting bytes {}-{} from {}",
                        range.start, range.end, info);
                request = GetObjectRequest.builder()
                        .bucket(info.getBucketName())
                        .key(info.getKey())
                        .range("bytes=" + range.start + "-" + range.end)
                        .build();
            } else {
                LOGGER.debug("Requesting {}", info);
                request = GetObjectRequest.builder()
                        .bucket(info.getBucketName())
                        .key(info.getKey())
                        .build();
            }
            return client.getObject(request);
        } catch (NoSuchBucketException | NoSuchKeyException e) {
            throw new NoSuchFileException(info.toString());
        } catch (SdkException e) {
            throw new IOException(info.toString(), e);
        }
    }

    @Override
    public StatResult stat() throws IOException {
        S3ObjectAttributes attrs = getObjectAttributes();
        StatResult result = new StatResult();
        result.setLastModified(attrs.lastModified);
        return result;
    }

    @Override
    public FormatIterator<Format> getFormatIterator() {
        return formatIterator;
    }

    private S3ObjectAttributes getObjectAttributes() throws IOException {
        if (objectAttributes == null) {
            // https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#ErrorCodeList
            final S3ObjectInfo info = getObjectInfo();
            final String bucket     = info.getBucketName();
            final String key        = info.getKey();
            final S3Client client   = getClientInstance(info);
            try {
                HeadObjectResponse response = client.headObject(HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
                objectAttributes              = new S3ObjectAttributes();
                objectAttributes.length       = response.contentLength();
                objectAttributes.contentType  = response.contentType();
                objectAttributes.lastModified = response.lastModified();
            } catch (NoSuchBucketException | NoSuchKeyException e) {
                throw new NoSuchFileException(info.toString());
            } catch (S3Exception e) {
                final int code = e.statusCode();
                if (code == 403) {
                    throw new AccessDeniedException(info.toString());
                } else {
                    LOGGER.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            } catch (SdkClientException e) {
                LOGGER.error(e.getMessage(), e);
                throw new IOException(info.toString(), e);
            }
        }
        return objectAttributes;
    }

    /**
     * @return Info for the current object from either the configuration or the
     *         delegate system. The {@link S3ObjectInfo#getLength() length} is
     *         not set. The result is cached.
     */
    S3ObjectInfo getObjectInfo() throws IOException {
        if (objectInfo == null) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (LookupStrategy.from(Key.S3SOURCE_LOOKUP_STRATEGY)) {
                case DELEGATE_SCRIPT:
                    try {
                        objectInfo = getObjectInfoUsingDelegateStrategy();
                    } catch (ScriptException e) {
                        throw new IOException(e);
                    }
                    break;
                default:
                    objectInfo = getObjectInfoUsingBasicStrategy();
                    break;
            }
        }
        return objectInfo;
    }

    /**
     * @return Object info based on {@link #identifier} and the application
     *         configuration.
     */
    private S3ObjectInfo getObjectInfoUsingBasicStrategy() {
        final var config        = Configuration.getInstance();
        final String bucketName = config.getString(Key.S3SOURCE_BUCKET_NAME);
        final String keyPrefix  = config.getString(Key.S3SOURCE_PATH_PREFIX, "");
        final String keySuffix  = config.getString(Key.S3SOURCE_PATH_SUFFIX, "");
        final String key        = keyPrefix + identifier.toString() + keySuffix;
        S3ObjectInfo info       = new S3ObjectInfo();
        info.setBucketName(bucketName);
        info.setKey(key);
        return info;
    }

    /**
     * @return Object info drawn from the {@link
     *         DelegateMethod#S3SOURCE_OBJECT_INFO} delegate method.
     * @throws IllegalArgumentException if the return value of the delegate
     *                                  method is invalid.
     * @throws NoSuchFileException      if the delegate script does not exist.
     * @throws ScriptException          if the delegate method throws an
     *                                  exception.
     */
    private S3ObjectInfo getObjectInfoUsingDelegateStrategy()
            throws ScriptException, NoSuchFileException {
        final Map<String,String> result =
                getDelegateProxy().getS3SourceObjectInfo();
        if (result.isEmpty()) {
            throw new NoSuchFileException(
                    DelegateMethod.S3SOURCE_OBJECT_INFO +
                    " returned nil for " + identifier);
        }

        if (result.containsKey("bucket") && result.containsKey("key")) {
            final S3ObjectInfo info = new S3ObjectInfo();
            info.setBucketName(result.get("bucket"));
            info.setKey(result.get("key"));
            // These may be null.
            info.setRegion(result.get("region"));
            info.setEndpoint(result.get("endpoint"));
            info.setAccessKeyID(result.get("access_key_id"));
            info.setSecretAccessKey(result.get("secret_access_key"));
            return info;
        } else {
            throw new IllegalArgumentException(
                    "Returned hash must include bucket and key");
        }
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        return new S3StreamFactory(() -> {
            S3ObjectInfo info = getObjectInfo();
            info.setLength(getObjectAttributes().length);
            return info;
        });
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        super.setIdentifier(identifier);
        reset();
    }

    private void reset() {
        objectInfo       = null;
        objectAttributes = null;
        formatIterator   = new FormatIterator<>();
    }

}
