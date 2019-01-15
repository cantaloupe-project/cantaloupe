package edu.illinois.library.cantaloupe.source;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Maps an identifier to an <a href="https://aws.amazon.com/s3/">Amazon
 * Simple Storage Service (S3)</a> object, for retrieving images from S3.</p>
 *
 * <p>Past versions of this class used the AWS Java SDK's S3 client, which is
 * poorly designed and not suitable for long-running servers (it leaks
 * connections like a sieve). This spurred a change to the Minio client.</p>
 *
 * <h1>Format Inference</h1>
 *
 * <p>See {@link #getFormat()}.</p>
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
 *             <li>If {@link #getFormat()} needs to check magic bytes:
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
 * @see <a href="https://docs.minio.io/docs/java-client-api-reference">
 *     Minio Java Client API Reference</a>
 */
class S3Source extends AbstractSource implements StreamSource {

    private static class S3ObjectAttributes {
        String contentType;
        long length;
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(S3Source.class);

    /**
     * Range used to infer the source image format.
     */
    private static final Range FORMAT_INFERENCE_RANGE = new Range(0, 32);

    private static final Pattern URL_REGION_PATTERN =
            Pattern.compile("[.-]([a-z]{2}-[a-z]+-[0-9]).amazonaws.com");

    private static MinioClient client;

    /**
     * Cached by {@link #getObjectInfo()}.
     */
    private S3ObjectInfo objectInfo;

    /**
     * Cached by {@link #getObjectAttributes()}.
     */
    private S3ObjectAttributes objectAttributes;

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

    static synchronized MinioClient getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            final AWSCredentialsProvider credentialsProvider =
                    AWSClientBuilder.newCredentialsProvider(
                            config.getString(Key.S3SOURCE_ACCESS_KEY_ID),
                            config.getString(Key.S3SOURCE_SECRET_KEY));
            final AWSCredentials credentials =
                    credentialsProvider.getCredentials();
            try {
                final String endpoint = config.getString(Key.S3SOURCE_ENDPOINT);
                client = new MinioClient(
                        endpoint,
                        credentials.getAWSAccessKeyId(),
                        credentials.getAWSSecretKey(),
                        awsRegionFromURL(endpoint));
            } catch (InvalidEndpointException | InvalidPortException e) {
                throw new IllegalArgumentException(e);
            }
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
     * @param range Byte range. May be {@literal null}.
     */
    static InputStream newObjectInputStream(S3ObjectInfo info,
                                            Range range) throws IOException {
        final MinioClient mc = getClientInstance();
        try {
            if (range != null) {
                LOGGER.debug("Requesting bytes {}-{} from {}",
                        range.start, range.end, info);
                return mc.getObject(info.getBucketName(), info.getKey(),
                        range.start, range.end - range.start + 1);
            } else {
                LOGGER.debug("Requesting {}", info);
                return mc.getObject(info.getBucketName(), info.getKey());
            }
        } catch (InvalidBucketNameException | InvalidKeyException e) {
            throw new NoSuchFileException(info.toString());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(info.toString(), e);
        }
    }

    @Override
    public void checkAccess() throws IOException {
        getObjectAttributes();
    }

    private S3ObjectAttributes getObjectAttributes() throws IOException {
        if (objectAttributes == null) {
            // https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#ErrorCodeList
            final S3ObjectInfo info = getObjectInfo();
            final String bucket     = info.getBucketName();
            final String key        = info.getKey();
            final MinioClient mc    = getClientInstance();
            try {
                objectAttributes        = new S3ObjectAttributes();
                objectAttributes.length = mc.statObject(bucket, key).length();
            } catch (InvalidBucketNameException | InvalidKeyException e) {
                throw new NoSuchFileException(info.toString());
            } catch (ErrorResponseException e) {
                final String code = e.errorResponse().code();
                if ("NoSuchBucket".equals(code) || "NoSuchKey".equals(code)) {
                    throw new NoSuchFileException(info.toString());
                } else if ("AccessDenied".equals(code) ||
                        "AllAccessDisabled".equals(code)) {
                    throw new AccessDeniedException(info.toString());
                } else {
                    LOGGER.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw e;
            } catch (Exception e) {
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
     */
    @Override
    public Format getFormat() throws IOException {
        if (format == null) {
            format = Format.UNKNOWN;

            final S3ObjectInfo info = getObjectInfo();

            // Try to infer a format from the object key.
            LOGGER.debug("Inferring format from object key for {}", info);
            format = Format.inferFormat(info.getKey());

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the identifier.
                LOGGER.debug("Inferring format from identifier for {}", info);
                format = Format.inferFormat(identifier);
            }

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the Content-Type header.
                LOGGER.debug("Inferring format from Content-Type header for {}",
                        info);
                String contentType = getObjectAttributes().contentType;
                if (contentType != null && !contentType.isEmpty()) {
                    format = new MediaType(contentType).toFormat();
                }

                if (Format.UNKNOWN.equals(format)) {
                    // Try to infer a format from the object's magic bytes.
                    LOGGER.debug("Inferring format from magic bytes for {}",
                            info);
                    try (InputStream is = new BufferedInputStream(
                                 newObjectInputStream(info, FORMAT_INFERENCE_RANGE))) {
                        List<MediaType> types = MediaType.detectMediaTypes(is);
                        if (!types.isEmpty()) {
                            format = types.get(0).toFormat();
                        }
                    }
                }
            }
        }
        return format;
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        S3ObjectInfo info = getObjectInfo();
        info.setLength(getObjectAttributes().length);
        return new S3StreamFactory(info);
    }

}
