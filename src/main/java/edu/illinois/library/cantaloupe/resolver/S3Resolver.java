package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
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
 * {@link Key#S3RESOLVER_LOOKUP_STRATEGY}. BasicLookupStrategy maps
 * identifiers directly to S3 object keys. ScriptLookupStrategy invokes a
 * delegate method to retrieve object keys dynamically.</p>
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
class S3Resolver extends AbstractResolver implements StreamResolver {

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

    private static class S3ObjectStreamSource implements StreamSource {

        private ObjectInfo objectInfo;

        S3ObjectStreamSource(ObjectInfo objectInfo) {
            this.objectInfo = objectInfo;
        }

        @Override
        public ImageInputStream newImageInputStream() throws IOException {
            return ImageIO.createImageInputStream(newInputStream());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            S3Object object = fetchObject(objectInfo);
            return object.getObjectContent();
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(S3Resolver.class);

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
                endpointURI = new URI(config.getString(Key.S3RESOLVER_ENDPOINT));
            } catch (URISyntaxException e) {
                LOGGER.error("Invalid URI for {}: {}",
                        Key.S3RESOLVER_ENDPOINT, e.getMessage());
            }

            client = new AWSClientBuilder()
                    .endpointURI(endpointURI)
                    .accessKeyID(config.getString(Key.S3RESOLVER_ACCESS_KEY_ID))
                    .secretKey(config.getString(Key.S3RESOLVER_SECRET_KEY))
                    .maxConnections(config.getInt(Key.S3RESOLVER_MAX_CONNECTIONS, 0))
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
            switch (LookupStrategy.from(Key.S3RESOLVER_LOOKUP_STRATEGY)) {
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
        final String bucketName = config.getString(Key.S3RESOLVER_BUCKET_NAME);
        final String keyPrefix = config.getString(Key.S3RESOLVER_PATH_PREFIX, "");
        final String keySuffix = config.getString(Key.S3RESOLVER_PATH_SUFFIX, "");
        final String key = keyPrefix + identifier.toString() + keySuffix;
        return new ObjectInfo(key, bucketName);
    }

    /**
     * @return Object info drawn from the {@link
     *         DelegateMethod#S3RESOLVER_OBJECT_INFO} delegate method.
     * @throws IllegalArgumentException if the return value of the delegate
     *                                  method is invalid.
     * @throws NoSuchFileException      if the delegate script does not exist.
     * @throws ScriptException          if the delegate method throws an
     *                                  exception.
     */
    private ObjectInfo getObjectInfoUsingDelegateStrategy()
            throws ScriptException, NoSuchFileException {
        Map<String,String> result = getDelegateProxy().getS3ResolverObjectInfo();

        if (result.isEmpty()) {
            throw new NoSuchFileException(
                    DelegateMethod.S3RESOLVER_OBJECT_INFO +
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
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            sourceFormat = Format.UNKNOWN;

            final ObjectInfo info = getObjectInfo();

            // Try to infer a format from the object key.
            LOGGER.debug("Inferring format from the object key for {}",
                    info);
            sourceFormat = Format.inferFormat(info.getKey());

            if (Format.UNKNOWN.equals(sourceFormat)) {
                // Try to infer a format from the identifier.
                LOGGER.debug("Inferring format from the identifier for {}",
                        info);
                sourceFormat = Format.inferFormat(identifier);
            }

            if (Format.UNKNOWN.equals(sourceFormat)) {
                try (S3Object object = getObject(FORMAT_INFERENCE_RANGE_LENGTH)) {
                    // Try to infer a format from the Content-Type header.
                    LOGGER.debug("Inferring format from the Content-Type header for {}",
                            info);
                    String contentType = object.getObjectMetadata().getContentType();
                    if (contentType != null && !contentType.isEmpty()) {
                        sourceFormat = new MediaType(contentType).toFormat();
                    }

                    if (Format.UNKNOWN.equals(sourceFormat)) {
                        // Try to infer a format from the object's magic bytes.
                        LOGGER.debug("Inferring format from magic bytes for {}",
                                info);

                        try (InputStream contentStream = new BufferedInputStream(
                                object.getObjectContent(),
                                FORMAT_INFERENCE_RANGE_LENGTH)) {
                            List<MediaType> types =
                                    MediaType.detectMediaTypes(contentStream);
                            if (!types.isEmpty()) {
                                sourceFormat = types.get(0).toFormat();
                            }
                        }
                    }
                }
            }
        }
        return sourceFormat;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        return new S3ObjectStreamSource(getObjectInfo());
    }

    @Override
    public synchronized void shutdown() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }

}
