package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.util.AWSClientFactory;

import org.jruby.RubyHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    private static Logger logger = LoggerFactory.
            getLogger(AmazonS3Resolver.class);

    static final String GET_KEY_DELEGATE_METHOD =
            "AmazonS3Resolver::get_object_key";

    private static AmazonS3 client;
    
    private String bucketName;

    static synchronized AmazonS3 getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            final AWSClientFactory factory = new AWSClientFactory(
            		config.getString(Key.AMAZONS3RESOLVER_ACCESS_KEY_ID),
            		config.getString(Key.AMAZONS3RESOLVER_SECRET_KEY),
            		config.getString(Key.AMAZONS3RESOLVER_BUCKET_REGION));
            client = factory.newClient();
        }
        return client;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        S3Object object = getObject();
        return new InputStreamStreamSource(object.getObjectContent());
    }

    /**
     * N.B.: Either the returned instance, or the return value of
     * {@link S3Object#getObjectContent()}, must be closed.
     */
    private S3Object getObject() throws IOException {
        AmazonS3 s3 = getClientInstance();

        final Configuration config = Configuration.getInstance();
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
        final Configuration config = Configuration.getInstance();
        switch (config.getString(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY)) {
            case "BasicLookupStrategy":
                return identifier.toString();
            case "ScriptLookupStrategy":
                try {
                	String objectKey;
                	Object object = getObjectInfoWithDelegateStrategy();
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
    private Object getObjectInfoWithDelegateStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_KEY_DELEGATE_METHOD,
                identifier.toString(), context.asMap());
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
                    sourceFormat = Format.inferFormat(getObjectKey());
                }                
            }
        }
        return sourceFormat;
    }

}
