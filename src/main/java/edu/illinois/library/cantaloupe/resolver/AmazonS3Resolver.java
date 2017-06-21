package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.jruby.RubyHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Maps an identifier to an <a href="https://aws.amazon.com/s3/">Amazon
 * Simple Storage Service (S3)</a> object, for retrieving images from Amazon
 * S3.</p>
 *
 * <h3>Lookup Strategies</h3>
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

    private static class AmazonS3StreamSource implements StreamSource {

        private final S3Object object;

        AmazonS3StreamSource(S3Object object) {
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

    static final String GET_KEY_DELEGATE_METHOD =
            "AmazonS3Resolver::get_object_key";

    private static AmazonS3 client;
    
    private String bucketName;

    static synchronized AmazonS3 getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            final String awsKey = config.getString(Key.AMAZONS3RESOLVER_ACCESS_KEY_ID);
            final String awsSecret = config.getString(Key.AMAZONS3RESOLVER_SECRET_KEY);
            final String awsRegion = config.getString(Key.AMAZONS3RESOLVER_BUCKET_REGION);
            
        	ArrayList<AWSCredentialsProvider> creds = new ArrayList<AWSCredentialsProvider>();
        	creds.addAll(Arrays.asList(new EnvironmentVariableCredentialsProvider(),
                    new SystemPropertiesCredentialsProvider(),
                    new ProfileCredentialsProvider(),
                    new InstanceProfileCredentialsProvider(false)
                    )
        	);
        	
        	if (!awsKey.isEmpty()) {
        		creds.add(0, new AWSStaticCredentialsProvider(
        				new BasicAWSCredentials(awsKey, awsSecret)));
            }
        	
        	final AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(creds);
        	AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
        			.withCredentials(chain);
        	
        	if (!awsRegion.isEmpty()) {
        		builder.setRegion(awsRegion);
        	}
        	client = builder.build();
        }
        return client;
    }

    @Override
    public StreamSource newStreamSource()
            throws IOException {
        return new AmazonS3StreamSource(getObject());
    }

    private S3Object getObject() throws IOException {
        AmazonS3 s3 = getClientInstance();

        final Configuration config = ConfigurationFactory.getInstance();
        final String objectKey = getObjectKey();
        if (bucketName == null) {
        	bucketName = config.getString(Key.AMAZONS3RESOLVER_BUCKET_NAME);
        }
        try {
            logger.info("Requesting {} from bucket {}", objectKey, bucketName);
            return s3.getObject(new GetObjectRequest(bucketName, objectKey));
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                throw new FileNotFoundException(e.getMessage());
            } else {
                throw new IOException(e);
            }
        }
    }
    
    private Map<String, Object> convertToHashMap(final RubyHash rubyMap) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (Object key : rubyMap.keySet()) {
            map.put(key.toString(), rubyMap.get(key));
        }
        return map;
    }

    private String getObjectKey() throws IOException {
        final Configuration config = ConfigurationFactory.getInstance();
        switch (config.getString(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY)) {
            case "BasicLookupStrategy":
                return identifier.toString();
            case "ScriptLookupStrategy":
                try {
                	String objectKey;
                	Object object = getObjectKeyWithDelegateStrategy();
                    if (object instanceof RubyHash) {
                    	Map<String, Object> map = convertToHashMap((RubyHash)object);
                    	if (map.containsKey("bucket") && map.containsKey("key")) {
            	        	bucketName = map.get("bucket").toString();
            	        	objectKey = map.get("key").toString();
                    	} else {
                    		logger.error("Hash does not include bucket and key");
                    		throw new IOException();
                    	}
                    } else {
                    	objectKey = (String)object;
                    }
                    return objectKey;
                } catch (ScriptException | DelegateScriptDisabledException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                throw new IOException(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY +
                        " is invalid or not set");
        }
    }

    /**
     * @return
     * @throws FileNotFoundException If the delegate script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     */
    private Object getObjectKeyWithDelegateStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_KEY_DELEGATE_METHOD,
                identifier.toString());
        if (result == null) {
            throw new FileNotFoundException(GET_KEY_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return result;
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
                    sourceFormat = Format.inferFormat(new Identifier(getObjectKey()));
                }                
            }
        }
        return sourceFormat;
    }

}
