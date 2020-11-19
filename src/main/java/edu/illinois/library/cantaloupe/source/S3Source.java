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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @see <a href="https://docs.minio.io/docs/java-client-api-reference">
 *     Minio Java Client API Reference</a>
 */
final class S3Source extends AbstractSource implements Source {

    private static class S3ObjectAttributes {
        String contentType;
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
     * Range used to infer the source image format.
     */
    private static final Range FORMAT_INFERENCE_RANGE = new Range(0, 32);

    private static final Pattern URL_REGION_PATTERN =
            Pattern.compile("[.-]([a-z]{2}-[a-z]+-[0-9]).amazonaws.com");

    private static S3Client client;

    /**
     * Cached by {@link #getObjectInfo()}.
     */
    private S3ObjectInfo objectInfo;

    /**
     * Cached by {@link #getObjectAttributes()}.
     */
    private S3ObjectAttributes objectAttributes;

    private FormatIterator<Format> formatIterator = new FormatIterator<>();

    /**
     * Extracts the AWS region from a URL or hostname like {@literal
     * s3.us-east-2.amazonaws.com}.
     *
     * @param url S3 endpoint URL.
     * @return    AWS region for the given URL, or {@literal null} if the URL
     *            is not a recognized AWS URL.
     * @see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html">AWS Regions and Endpoints</a>
     */
    static String awsRegionFromURL(String url) {
        // special cases
        if ("s3.amazonaws.com".equals(url) ||
                "s3-external-1.amazonaws.com".equals(url)) {
            return "us-east-1";
        }

        Matcher matcher = URL_REGION_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    static synchronized S3Client getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            final String endpointStr = config.getString(Key.S3SOURCE_ENDPOINT);
            URI endpointURI = null;
            if (endpointStr != null) {
                try {
                    endpointURI = new URI(endpointStr);
                } catch (URISyntaxException e) {
                    LOGGER.error("Invalid URI for {}: {}",
                            Key.S3SOURCE_ENDPOINT, e.getMessage());
                }
            }
            client = new S3ClientBuilder()
                    .accessKeyID(config.getString(Key.S3SOURCE_ACCESS_KEY_ID))
                    .secretKey(config.getString(Key.S3SOURCE_SECRET_KEY))
                    .endpointURI(endpointURI)
                    .region(config.getString(Key.S3SOURCE_REGION))
                    .build();
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
        final S3Client client = getClientInstance();
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
    public void checkAccess() throws IOException {
        getObjectAttributes();
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
            final S3Client client   = getClientInstance();
            try {
                HeadObjectResponse response = client.headObject(HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
                objectAttributes        = new S3ObjectAttributes();
                objectAttributes.length = response.contentLength();
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
        final Configuration config = Configuration.getInstance();
        final String bucketName = config.getString(Key.S3SOURCE_BUCKET_NAME);
        final String keyPrefix = config.getString(Key.S3SOURCE_PATH_PREFIX, "");
        final String keySuffix = config.getString(Key.S3SOURCE_PATH_SUFFIX, "");
        final String key = keyPrefix + identifier.toString() + keySuffix;
        return new S3ObjectInfo(key, bucketName);
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
            String bucketName = result.get("bucket");
            String objectKey  = result.get("key");
            return new S3ObjectInfo(objectKey, bucketName);
        } else {
            throw new IllegalArgumentException(
                    "Returned hash does not include bucket and key");
        }
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        S3ObjectInfo info = getObjectInfo();
        info.setLength(getObjectAttributes().length);
        return new S3StreamFactory(info);
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
