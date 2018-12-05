package edu.illinois.library.cantaloupe.source;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;

/**
 * <p>Maps an identifier to an <a href="https://aws.amazon.com/s3/">Amazon
 * Simple Storage Service (S3)</a> object, for retrieving images from S3.</p>
 *
 * <h1>Format Inference</h1>
 *
 * <ol>
 *     <li>If the object key has a recognized filename extension, the format
 *     will be inferred from that.</li>
 *     <li>Otherwise, if the source image's URI identifier has a recognized
 *     filename extension, the format will be inferred from that.</li>
 *     <li>Otherwise, a {@literal GET} request will be sent with a {@literal
 *     Range} header specifying a small range of data from the beginning of the
 *     resource.
 *         <ol>
 *             <li>If a {@literal Content-Type} header is present in the
 *             response, and is specific enough (i.e. not {@literal
 *             application/octet-stream}), a format will be inferred from
 *             that.</li>
 *             <li>Otherwise, a format will be inferred from the magic bytes in
 *             the response body.</li>
 *         </ol>
 *     </li>
 * </ol>
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
 *             <li>If {@link StreamFactory#newImageInputStream()} is used:
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
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
class S3Source extends AbstractSource implements StreamSource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(S3Source.class);

    /**
     * Byte length of the range used to infer the source image format.
     */
    private static final int FORMAT_INFERENCE_RANGE_LENGTH = 32;

    private static AmazonS3 client;

    private IOException cachedAccessException;

    /**
     * Cached by {@link #getObjectInfo()}.
     */
    private S3ObjectInfo objectInfo;

    /**
     * Cached by {@link #getObjectMetadata()}.
     */
    private ObjectMetadata objectMetadata;

    static synchronized AmazonS3 getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            try {
                client = new AWSClientBuilder()
                        .endpointURI(new URI(config.getString(Key.S3SOURCE_ENDPOINT)))
                        .accessKeyID(config.getString(Key.S3SOURCE_ACCESS_KEY_ID))
                        .secretKey(config.getString(Key.S3SOURCE_SECRET_KEY))
                        .maxConnections(config.getInt(Key.S3SOURCE_MAX_CONNECTIONS, 0))
                        .build();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return client;
    }

    /**
     * <p>Fetches a whole object.</p>
     *
     * <p>N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.</p>
     */
    static S3Object fetchObject(S3ObjectInfo info) throws IOException {
        return fetchObject(info, 0, 0);
    }

    /**
     * <p>Fetches a byte range of an object.</p>
     *
     * <p>N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.</p>
     *
     * @param info  Object info.
     * @param start Starting byte offset.
     * @param end   Ending byte offset.
     */
    static S3Object fetchObject(S3ObjectInfo info,
                                long start,
                                long end) throws IOException {
        final AmazonS3 s3 = getClientInstance();
        try {
            GetObjectRequest request = new GetObjectRequest(
                    info.getBucketName(),
                    info.getKey());
            if (end - start > 0) {
                request.setRange(start, end);
                LOGGER.debug("Requesting bytes {}-{} from {}",
                        start, end, info);
            } else {
                LOGGER.debug("Requesting {}", info);
            }
            return s3.getObject(request);
        } catch (AmazonS3Exception e) {
            if ("NoSuchKey".equals(e.getErrorCode()) ||
                    "NoSuchBucket".equals(e.getErrorCode())) {
                throw new NoSuchFileException(info.toString());
            } else {
                throw new IOException(e);
            }
        } catch (AmazonClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void checkAccess() throws IOException {
        final AmazonS3 s3 = getClientInstance();
        final S3ObjectInfo info = getObjectInfo();
        try {
            s3.getObjectMetadata(info.getBucketName(), info.getKey());
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new NoSuchFileException(info.toString());
            } else {
                throw new IOException(e);
            }
        } catch (AmazonClientException e) {
            throw new IOException(e);
        }
    }

    /**
     * N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.
     *
     * @param start Starting byte offset.
     * @param end   Ending byte offset.
     * @throws NoSuchFileException   if the object corresponding to {@link
     *                               #identifier} does not exist.
     * @throws AccessDeniedException if the object corresponding to {@link
     *                               #identifier} is not readable.
     * @throws IOException           if there is some other issue accessing the
     *                               object.
     */
    private S3Object getObject(long start, long end) throws IOException {
        if (cachedAccessException != null) {
            throw cachedAccessException;
        } else {
            try {
                final S3ObjectInfo info = getObjectInfo();
                return fetchObject(info, start, end);
            } catch (IOException e) {
                cachedAccessException = e;
                throw e;
            }
        }
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

    private ObjectMetadata getObjectMetadata() throws IOException {
        if (objectMetadata == null) {
            try {
                final AmazonS3 s3 = getClientInstance();
                final S3ObjectInfo objectInfo = getObjectInfo();
                objectMetadata = s3.getObjectMetadata(
                        objectInfo.getBucketName(), objectInfo.getKey());
            } catch (AmazonS3Exception e) {
                if (e.getStatusCode() == 404) {
                    throw new NoSuchFileException(objectInfo.toString());
                } else {
                    throw new IOException(e);
                }
            } catch (AmazonClientException e) {
                throw new IOException(e);
            }
        }
        return objectMetadata;
    }

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
                try (S3Object object = getObject(0, FORMAT_INFERENCE_RANGE_LENGTH)) {
                    // Try to infer a format from the Content-Type header.
                    LOGGER.debug("Inferring format from Content-Type header for {}",
                            info);
                    String contentType = getObjectMetadata().getContentType();
                    if (contentType != null && !contentType.isEmpty()) {
                        format = new MediaType(contentType).toFormat();
                    }

                    if (Format.UNKNOWN.equals(format)) {
                        // Try to infer a format from the object's magic bytes.
                        LOGGER.debug("Inferring format from magic bytes for {}",
                                info);

                        try (InputStream contentStream = new BufferedInputStream(
                                object.getObjectContent(),
                                FORMAT_INFERENCE_RANGE_LENGTH)) {
                            List<MediaType> types =
                                    MediaType.detectMediaTypes(contentStream);
                            if (!types.isEmpty()) {
                                format = types.get(0).toFormat();
                            }
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
        info.setLength(getObjectMetadata().getContentLength());
        return new S3StreamFactory(info, fetchObject(info));
    }

    @Override
    public void shutdown() {
        synchronized (S3Source.class) {
            if (client != null) {
                client.shutdown();
                client = null;
            }
        }
    }

}
