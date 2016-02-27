package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import edu.illinois.library.cantaloupe.Application;
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
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
class AmazonS3Resolver extends AbstractResolver implements StreamResolver {

    private static class AmazonS3StreamSource implements StreamSource {

        private final S3Object object;

        public AmazonS3StreamSource(S3Object object) {
            this.object = object;
        }

        @Override
        public ImageInputStream newImageInputStream() throws IOException {
            return ImageIO.createImageInputStream(newInputStream());
        }

        @Override
        public S3ObjectInputStream newInputStream() throws IOException {
            return object.getObjectContent();
        }

    }

    private static Logger logger = LoggerFactory.
            getLogger(AmazonS3Resolver.class);

    public static final String ACCESS_KEY_ID_CONFIG_KEY =
            "AmazonS3Resolver.access_key_id";
    public static final String BUCKET_NAME_CONFIG_KEY =
            "AmazonS3Resolver.bucket.name";
    public static final String BUCKET_REGION_CONFIG_KEY =
            "AmazonS3Resolver.bucket.region";
    public static final String ENDPOINT_CONFIG_KEY =
            "AmazonS3Resolver.endpoint";
    public static final String LOOKUP_STRATEGY_CONFIG_KEY =
            "AmazonS3Resolver.lookup_strategy";
    public static final String SECRET_KEY_CONFIG_KEY =
            "AmazonS3Resolver.secret_key";

    private static AmazonS3 client;

    private static AmazonS3 getClientInstance() {
        if (client == null) {
            class ConfigFileCredentials implements AWSCredentials {
                @Override
                public String getAWSAccessKeyId() {
                    Configuration config = Application.getConfiguration();
                    return config.getString(ACCESS_KEY_ID_CONFIG_KEY);
                }

                @Override
                public String getAWSSecretKey() {
                    Configuration config = Application.getConfiguration();
                    return config.getString(SECRET_KEY_CONFIG_KEY);
                }
            }

            AWSCredentials credentials = new ConfigFileCredentials();
            client = new AmazonS3Client(credentials);
            Configuration config = Application.getConfiguration();

            // a custom endpoint will be used in testing
            final String endpoint = config.getString(ENDPOINT_CONFIG_KEY);
            if (endpoint != null) {
                logger.info("Using endpoint: {}", endpoint);
                client.setEndpoint(endpoint);
            }

            final String regionName = config.getString(BUCKET_REGION_CONFIG_KEY);
            if (regionName != null && regionName.length() > 0) {
                Regions regions = Regions.fromName(regionName);
                Region region = Region.getRegion(regions);
                logger.info("Using region: {}", region);
                client.setRegion(region);
            }
        }
        return client;
    }

    @Override
    public StreamSource getStreamSource()
            throws IOException {
        return new AmazonS3StreamSource(getObject());
    }

    private S3Object getObject() throws IOException {
        AmazonS3 s3 = getClientInstance();

        Configuration config = Application.getConfiguration();
        final String bucketName = config.getString(BUCKET_NAME_CONFIG_KEY);
        logger.info("Using bucket: {}", bucketName);
        final String objectKey = getObjectKey();
        try {
            logger.info("Requesting {}", objectKey);
            return s3.getObject(new GetObjectRequest(bucketName, objectKey));
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                throw new FileNotFoundException(e.getMessage());
            } else {
                throw new IOException(e);
            }
        }
    }

    private String getObjectKey() throws IOException {
        final Configuration config = Application.getConfiguration();
        switch (config.getString(LOOKUP_STRATEGY_CONFIG_KEY)) {
            case "BasicLookupStrategy":
                return identifier.toString();
            case "ScriptLookupStrategy":
                try {
                    return getObjectKeyWithDelegateStrategy();
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
     * @return
     * @throws FileNotFoundException If the delegate script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     */
    private String getObjectKeyWithDelegateStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String[] args = { identifier.toString() };
        final String method = "get_s3_object_key";
        final Object result = engine.invoke(method, args);
        if (result == null) {
            throw new FileNotFoundException(method + " returned nil for " +
                    identifier);
        }
        return (String) result;
    }

    @Override
    public Format getSourceFormat() throws IOException {
        S3Object object = getObject();
        String contentType = object.getObjectMetadata().getContentType();
        if (contentType != null) {
            Format format = Format.getFormat(contentType);
            if (format != null && !format.equals(Format.UNKNOWN)) {
                return format;
            }
        }
        return Format.getFormat(identifier);
    }

}
