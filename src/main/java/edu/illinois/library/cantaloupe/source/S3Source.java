package edu.illinois.library.cantaloupe.source;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import edu.illinois.library.cantaloupe.async.ThreadPool;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
 * @see <a href="https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/">
 *     AWS SDK for Java API Reference</a>
 */
class S3Source extends AbstractSource implements StreamSource {

    static class ObjectInfo {

        private String bucketName;
        private String key;

        ObjectInfo(String key, String bucketName) {
            this.key = key;
            this.bucketName = bucketName;
        }

        String getBucketName() {
            return bucketName;
        }

        String getKey() {
            return key;
        }

        @Override
        public String toString() {
            return getBucketName() + "/" + getKey();
        }

    }

    private static class S3ObjectStreamFactory implements StreamFactory {

        private ObjectInfo objectInfo;

        S3ObjectStreamFactory(ObjectInfo objectInfo) {
            this.objectInfo = objectInfo;
        }

        @Override
        public InputStream newInputStream() throws IOException {
            S3Object object = fetchObject(objectInfo);
            final InputStream responseStream = object.getObjectContent();
            final AtomicBoolean isClosed     = new AtomicBoolean();

            // Ideally we would just return responseStream. However, if
            // responseStream is close()d before all of its data has been read,
            // its underlying TCP connection will also be closed, thus negating
            // the advantage of the connection pool, and triggering a warning
            // log message from the S3 client.
            //
            // This wrapper stream's close() method will drain the stream
            // before closing it. Because draining the stream may be expensive,
            // it will happen in another thread.
            return new InputStream() {
                @Override
                public void close() {
                    if (!isClosed.get()) {
                        isClosed.set(true);

                        ThreadPool.getInstance().submit(() -> {
                            try {
                                try {
                                    while (responseStream.read() != -1) {
                                        // drain the stream
                                    }
                                } finally {
                                    try {
                                        responseStream.close();
                                    } finally {
                                        super.close();
                                    }
                                }
                            } catch (IOException e) {
                                LOGGER.warn(e.getMessage(), e);
                            }
                        });
                    }
                }

                @Override
                public int read() throws IOException {
                    return responseStream.read();
                }

                @Override
                public int read(byte[] b) throws IOException {
                    return responseStream.read(b);
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return responseStream.read(b, off, len);
                }

                @Override
                public long skip(long n) throws IOException {
                    return responseStream.skip(n);
                }
            };
        }

    }

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
    private ObjectInfo objectInfo;

    private static synchronized AmazonS3 getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();

            URI endpointURI = null;
            try {
                endpointURI = new URI(config.getString(Key.S3SOURCE_ENDPOINT));
            } catch (URISyntaxException e) {
                LOGGER.error("Invalid URI for {}: {}",
                        Key.S3SOURCE_ENDPOINT, e.getMessage());
            }

            client = new AWSClientBuilder()
                    .endpointURI(endpointURI)
                    .accessKeyID(config.getString(Key.S3SOURCE_ACCESS_KEY_ID))
                    .secretKey(config.getString(Key.S3SOURCE_SECRET_KEY))
                    .maxConnections(config.getInt(Key.S3SOURCE_MAX_CONNECTIONS, 0))
                    .build();
        }
        return client;
    }

    /**
     * N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.
     */
    private static S3Object fetchObject(ObjectInfo info) throws IOException {
        return fetchObject(info, 0);
    }

    /**
     * N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.
     *
     * @param info   Object info.
     * @param length Number of bytes to fetch.
     */
    private static S3Object fetchObject(ObjectInfo info,
                                        int length) throws IOException {
        final AmazonS3 s3 = getClientInstance();
        try {
            GetObjectRequest request = new GetObjectRequest(
                    info.getBucketName(),
                    info.getKey());
            if (length > 0) {
                request.setRange(0, length);
                LOGGER.debug("Requesting {} bytes from {}", length, info);
            } else {
                LOGGER.debug("Requesting {}", info);
            }
            return s3.getObject(request);
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                throw new NoSuchFileException(info.toString());
            } else {
                throw new IOException(e);
            }
        } catch (SdkBaseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void checkAccess() throws IOException {
        final AmazonS3 s3 = getClientInstance();
        final ObjectInfo info = getObjectInfo();
        try {
            s3.getObjectMetadata(info.getBucketName(), info.getKey());
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new NoSuchFileException(info.toString());
            } else {
                throw new IOException(e);
            }
        } catch (SdkBaseException e) {
            throw new IOException(e);
        }
    }

    /**
     * N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.
     *
     * @param length Number of object bytes to return.
     * @throws NoSuchFileException   if the object corresponding to {@link
     *                               #identifier} does not exist.
     * @throws AccessDeniedException if the object corresponding to {@link
     *                               #identifier} is not readable.
     * @throws IOException           if there is some other issue accessing the
     *                               object.
     */
    private S3Object getObject(int length) throws IOException {
        if (cachedAccessException != null) {
            throw cachedAccessException;
        } else {
            try {
                final ObjectInfo info = getObjectInfo();
                return fetchObject(info, length);
            } catch (IOException e) {
                cachedAccessException = e;
                throw e;
            }
        }
    }

    /**
     * @return Info for the current object. The result is cached.
     */
    ObjectInfo getObjectInfo() throws IOException {
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
    private ObjectInfo getObjectInfoUsingBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        final String bucketName = config.getString(Key.S3SOURCE_BUCKET_NAME);
        final String keyPrefix = config.getString(Key.S3SOURCE_PATH_PREFIX, "");
        final String keySuffix = config.getString(Key.S3SOURCE_PATH_SUFFIX, "");
        final String key = keyPrefix + identifier.toString() + keySuffix;
        return new ObjectInfo(key, bucketName);
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
    private ObjectInfo getObjectInfoUsingDelegateStrategy()
            throws ScriptException, NoSuchFileException {
        Map<String,String> result = getDelegateProxy().getS3SourceObjectInfo();

        if (result.isEmpty()) {
            throw new NoSuchFileException(
                    DelegateMethod.S3SOURCE_OBJECT_INFO +
                    " returned nil for " + identifier);
        }

        String bucketName, objectKey;

        if (result.containsKey("bucket") && result.containsKey("key")) {
            bucketName = result.get("bucket");
            objectKey = result.get("key");
        } else {
            throw new IllegalArgumentException(
                    "Returned hash does not include bucket and key");
        }

        return new ObjectInfo(objectKey, bucketName);
    }

    @Override
    public Format getFormat() throws IOException {
        if (format == null) {
            format = Format.UNKNOWN;

            final ObjectInfo info = getObjectInfo();

            // Try to infer a format from the object key.
            LOGGER.debug("Inferring format from the object key for {}",
                    info);
            format = Format.inferFormat(info.getKey());

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the identifier.
                LOGGER.debug("Inferring format from the identifier for {}",
                        info);
                format = Format.inferFormat(identifier);
            }

            if (Format.UNKNOWN.equals(format)) {
                try (S3Object object = getObject(FORMAT_INFERENCE_RANGE_LENGTH)) {
                    // Try to infer a format from the Content-Type header.
                    LOGGER.debug("Inferring format from the Content-Type header for {}",
                            info);
                    String contentType = object.getObjectMetadata().getContentType();
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
        return new S3ObjectStreamFactory(getObjectInfo());
    }

    @Override
    public synchronized void shutdown() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }

}
