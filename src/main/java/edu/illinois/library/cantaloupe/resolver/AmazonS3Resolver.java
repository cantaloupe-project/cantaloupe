package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.configuration.Configuration;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
class AmazonS3Resolver extends AbstractResolver implements ChannelResolver {

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
            if (regionName != null) {
                Regions regions = Regions.fromName(regionName);
                Region region = Region.getRegion(regions);
                logger.info("Using region: {}", region);
                client.setRegion(region);
            }
        }
        return client;
    }

    @Override
    public ReadableByteChannel getChannel(Identifier identifier)
            throws IOException {
        final S3Object object = getObject(identifier);
        return Channels.newChannel(object.getObjectContent());
    }

    private S3Object getObject(Identifier identifier) throws IOException {
        AmazonS3 s3 = getClientInstance();

        Configuration config = Application.getConfiguration();
        final String bucketName = config.getString(BUCKET_NAME_CONFIG_KEY);
        logger.debug("Using bucket: {}", bucketName);
        final String objectKey = getObjectKey(identifier);
        try {
            logger.debug("Requesting {}", objectKey);
            return s3.getObject(new GetObjectRequest(bucketName, objectKey));
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                throw new FileNotFoundException(e.getMessage());
            } else {
                throw new IOException(e);
            }
        }
    }

    private String getObjectKey(Identifier identifier) throws IOException {
        final Configuration config = Application.getConfiguration();
        switch (config.getString(LOOKUP_STRATEGY_CONFIG_KEY)) {
            case "BasicLookupStrategy":
                return identifier.toString();
            case "ScriptLookupStrategy":
                try {
                    return getObjectKeyWithDelegateStrategy(identifier);
                } catch (ScriptException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                throw new IOException(LOOKUP_STRATEGY_CONFIG_KEY +
                        " is invalid or not set");
        }
    }

    /**
     * @param identifier
     * @return
     * @throws FileNotFoundException If the delegate script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     */
    private String getObjectKeyWithDelegateStrategy(Identifier identifier)
            throws IOException, ScriptException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String[] args = { identifier.toString() };
        final String method = "Cantaloupe::get_s3_object_key";
        final long msec = System.currentTimeMillis();
        final Object result = engine.invoke(method, args);
        logger.debug("{} load+exec time: {} msec", method,
                System.currentTimeMillis() - msec);
        if (result == null) {
            throw new FileNotFoundException(method + " returned nil for " +
                    identifier);
        }
        return (String) result;
    }

    @Override
    public SourceFormat getSourceFormat(Identifier identifier) throws IOException {
        S3Object object = getObject(identifier);
        String contentType = object.getObjectMetadata().getContentType();
        if (contentType != null) {
            MediaType mediaType = new MediaType(contentType);
            SourceFormat sourceFormat = SourceFormat.getSourceFormat(mediaType);
            if (sourceFormat != null && !sourceFormat.equals(SourceFormat.UNKNOWN)) {
                return sourceFormat;
            }
        }
        return SourceFormat.getSourceFormat(identifier);
    }

}
