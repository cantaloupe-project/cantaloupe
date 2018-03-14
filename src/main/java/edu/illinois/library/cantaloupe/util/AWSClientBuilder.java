package edu.illinois.library.cantaloupe.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an AWS client using the Builder pattern.
 */
public class AWSClientBuilder {

    private class CustomCredentialsProvider implements AWSCredentialsProvider {

        @Override
        public AWSCredentials getCredentials() {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return accessKeyID;
                }

                @Override
                public String getAWSSecretKey() {
                    return secretKey;
                }
            };
        }

        @Override
        public void refresh() {}

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AWSClientBuilder.class);

    private static final int DEFAULT_CLIENT_EXECUTION_TIMEOUT_MSEC = 10 * 60 * 1000;
    private static final long DEFAULT_CONNECTION_TTL_MSEC          = 30 * 60 * 1000;
    private static final int  DEFAULT_MAX_CONNECTIONS              = 200;

    private String accessKeyID;
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private String region;
    private String secretKey;

    /**
     * @param accessKeyID AWS access key ID.
     * @return The instance.
     */
    public AWSClientBuilder accessKeyID(String accessKeyID) {
        this.accessKeyID = accessKeyID;
        return this;
    }

    /**
     * @param maxConnections Maximum concurrent connections to AWS.
     * @return The instance.
     */
    public AWSClientBuilder maxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    /**
     * @param region AWS region.
     * @return The instance.
     */
    public AWSClientBuilder region(String region) {
        this.region = region;
        return this;
    }

    /**
     * @param secretKey AWS secret key.
     * @return The instance.
     */
    public AWSClientBuilder secretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public AmazonS3 build() {
        LOGGER.debug("Building an AWS client with region: {}; max connections: {}",
                region, maxConnections);

        final ClientConfiguration clientConfig = new ClientConfiguration();
        // The AWS SDK default is 50.
        clientConfig.setMaxConnections(maxConnections);
        clientConfig.setConnectionTTL(DEFAULT_CONNECTION_TTL_MSEC);
        clientConfig.setClientExecutionTimeout(DEFAULT_CLIENT_EXECUTION_TIMEOUT_MSEC);
        clientConfig.setUseTcpKeepAlive(true);
        
    	List<AWSCredentialsProvider> creds = new ArrayList<>(
    	        Arrays.asList(
    	                new EnvironmentVariableCredentialsProvider(),
                        new SystemPropertiesCredentialsProvider(),
                        new ProfileCredentialsProvider(),
                        new InstanceProfileCredentialsProvider(false)));

        final AWSCredentialsProvider credsProvider =
                new CustomCredentialsProvider();
    	if (!credsProvider.getCredentials().getAWSAccessKeyId().isEmpty()) {
    		creds.add(0, credsProvider);
        }
    	
    	final AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(creds);
    	
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
        		.withCredentials(chain)
        		.withClientConfiguration(clientConfig);
        
        String regionStr = region;
        if (regionStr != null && !regionStr.isEmpty()) {
            builder.setRegion(regionStr);
        }        

        return builder.build();
    }

}
