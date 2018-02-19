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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.Map;

/**
 * <p>Maps an identifier to an <a href="https://aws.amazon.com/s3/">Amazon
 * Simple Storage Service (S3)</a> object, for retrieving images from S3.</p>
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
            return "" + getBucketName() + "/" + getKey();
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

    private static AmazonS3 client;

    private IOException cachedAccessException;

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
                    .maxConnections(config.getInt(Key.S3RESOLVER_MAX_CONNECTIONS, 100))
                    .build();
        }
        return client;
    }

    /**
     * N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.
     */
    private static S3Object fetchObject(ObjectInfo info) throws IOException {
        final AmazonS3 s3 = getClientInstance();
        try {
            LOGGER.debug("Requesting {}", info);
            return s3.getObject(new GetObjectRequest(
                    info.getBucketName(),
                    info.getKey()));
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                throw new NoSuchFileException(e.getMessage());
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void checkAccess() throws IOException {
        S3Object object = null;
        try {
            object = getObject();
        } finally {
            if (object != null) {
                object.close();
            }
        }
    }

    /**
     * N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.
     *
     * @throws NoSuchFileException   if the object corresponding to {@link
     *                               #identifier} does not exist.
     * @throws AccessDeniedException if the object corresponding to {@link
     *                               #identifier} is not readable.
     * @throws IOException           if there is some other issue accessing the
     *                               object.
     */
    private S3Object getObject() throws IOException {
        if (cachedAccessException != null) {
            throw cachedAccessException;
        } else {
            try {
                final ObjectInfo info = getObjectInfo();
                return fetchObject(info);
            } catch (IOException e) {
                cachedAccessException = e;
                throw e;
            }
        }
    }

    ObjectInfo getObjectInfo() throws IOException {
        ObjectInfo objectInfo;

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
            try (S3Object object = getObject()) {
                String contentType = object.getObjectMetadata().getContentType();
                // See if we can determine the format from the Content-Type header.
                if (contentType != null && !contentType.isEmpty()) {
                    sourceFormat = new MediaType(contentType).toFormat();
                }
                if (sourceFormat == null || Format.UNKNOWN.equals(sourceFormat)) {
                    // Try to infer a format based on the identifier.
                    sourceFormat = Format.inferFormat(identifier);
                }
                if (Format.UNKNOWN.equals(sourceFormat)) {
                    // Try to infer a format based on the objectKey.
                    sourceFormat = Format.inferFormat(object.getKey());
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
