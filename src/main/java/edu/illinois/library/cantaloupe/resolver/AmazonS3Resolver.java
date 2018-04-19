package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Map;

/**
 * <p>Maps an identifier to an <a href="https://aws.amazon.com/s3/">Amazon
 * Simple Storage Service (S3)</a> object, for retrieving images from Amazon
 * S3.</p>
 *
 * <h1>Format Inference</h1>
 *
 * <ol>
 *     <li>If the object key has a recognized filename extension, the format
 *     will be inferred from that.</li>
 *     <li>Otherwise, if the source image's URI identifier has a recognized
 *     filename extension, the format will be inferred from that.</li>
 *     <li>Otherwise, a {@literal HEAD} request will be sent. If a {@literal
 *     Content-Type} header is present in the response, and is specific enough
 *     (i.e. not {@literal application/octet-stream}), a format will be
 *     inferred from that.</li>
 * </ol>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#AMAZONS3RESOLVER_LOOKUP_STRATEGY}. BasicLookupStrategy maps
 * identifiers directly to S3 object keys. ScriptLookupStrategy invokes a
 * delegate method to retrieve object keys dynamically.</p>
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
class AmazonS3Resolver extends AbstractResolver implements StreamResolver {

    private static class ObjectInfo {

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
        public InputStream newInputStream() throws IOException {
            S3Object object = fetchObject(objectInfo);
            return object.getObjectContent();
        }

        /**
         * N.B.: Either the returned instance, or the return value of
         * {@link S3Object#getObjectContent()}, must be closed.
         */
        private S3Object fetchObject(ObjectInfo info) throws IOException {
            final AmazonS3 s3 = getClientInstance();
            try {
                LOGGER.debug("Requesting {}", info);
                return s3.getObject(info.getBucketName(), info.getKey());
            } catch (AmazonS3Exception e) {
                if (e.getErrorCode().equals("NoSuchKey")) {
                    throw new NoSuchFileException(e.getMessage());
                } else {
                    throw new IOException(e);
                }
            }
        }

    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(AmazonS3Resolver.class);

    private static final String GET_KEY_DELEGATE_METHOD =
            "AmazonS3Resolver::get_object_key";

    private static AmazonS3 client;

    private ObjectInfo objectInfo;

    private static synchronized AmazonS3 getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            client = new AWSClientBuilder()
                    .accessKeyID(config.getString(Key.AMAZONS3RESOLVER_ACCESS_KEY_ID))
                    .secretKey(config.getString(Key.AMAZONS3RESOLVER_SECRET_KEY))
                    .region(config.getString(Key.AMAZONS3RESOLVER_BUCKET_REGION))
                    .maxConnections(config.getInt(Key.AMAZONS3RESOLVER_MAX_CONNECTIONS))
                    .build();
        }
        return client;
    }

    @Override
    public void checkAccess() throws IOException {
        try {
            final AmazonS3 s3 = getClientInstance();
            final ObjectInfo info = getObjectInfo();
            s3.getObjectMetadata(info.getBucketName(), info.getKey());
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new NoSuchFileException(e.getMessage());
            } else {
                throw new IOException(e);
            }
        }
    }

    private ObjectInfo getObjectInfo() throws IOException {
        if (objectInfo == null) {
            final Configuration config = Configuration.getInstance();

            switch (LookupStrategy.from(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY)) {
                case DELEGATE_SCRIPT:
                    try {
                        String bucketName, objectKey;
                        Object object = getObjectInfoWithDelegateStrategy();
                        if (object instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) object;
                            if (map.containsKey("bucket") && map.containsKey("key")) {
                                bucketName = map.get("bucket").toString();
                                objectKey = map.get("key").toString();
                            } else {
                                throw new IllegalArgumentException(
                                        "Hash does not include bucket and key");
                            }
                        } else {
                            objectKey = (String) object;
                            bucketName = config.getString(
                                    Key.AMAZONS3RESOLVER_BUCKET_NAME);
                        }
                        objectInfo = new ObjectInfo(objectKey, bucketName);
                    } catch (ScriptException | DelegateScriptDisabledException e) {
                        throw new IOException(e);
                    }
                    break;
                default:
                    objectInfo = new ObjectInfo(identifier.toString(),
                            config.getString(Key.AMAZONS3RESOLVER_BUCKET_NAME));
                    break;
            }
        }
        return objectInfo;
    }

    /**
     * @return
     * @throws NoSuchFileException If the delegate script does not exist.
     * @throws IOException
     * @throws ScriptException If the delegate method throws an exception.
     */
    private Object getObjectInfoWithDelegateStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_KEY_DELEGATE_METHOD,
                identifier.toString(), context.asMap());
        if (result == null) {
            throw new NoSuchFileException(GET_KEY_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return result;
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            final ObjectInfo info = getObjectInfo();

            // Try to infer a format based on the object key.
            sourceFormat = Format.inferFormat(info.getKey());

            if (Format.UNKNOWN.equals(sourceFormat)) {
                // Try to infer a format based on the identifier.
                sourceFormat = Format.inferFormat(identifier);
            }

            if (Format.UNKNOWN.equals(sourceFormat)) {
                // Try to infer the format from the Content-Type header.
                final AmazonS3 s3 = getClientInstance();
                final ObjectMetadata metadata = s3.getObjectMetadata(
                        info.getBucketName(), info.getKey());
                final String contentType = metadata.getContentType();

                if (contentType != null && !contentType.isEmpty()) {
                    sourceFormat = new MediaType(contentType).toFormat();
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
