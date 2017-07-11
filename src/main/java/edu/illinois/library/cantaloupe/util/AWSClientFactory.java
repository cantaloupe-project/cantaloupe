package edu.illinois.library.cantaloupe.util;

import java.util.ArrayList;
import java.util.Arrays;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/**
 * Used to obtain an AWS client respecting the configuration provided to the
 * constructor.
 */
public class AWSClientFactory {

    private class CustomCredentialsProvider implements AWSCredentialsProvider {

        @Override
        public AWSCredentials getCredentials() {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return accessKeyId;
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

    private String accessKeyId;
    private String region;
    private String secretKey;

    public AWSClientFactory(String accessKeyId, String secretKey) {
        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
    }

    public AWSClientFactory(String accessKeyId, String secretKey,
                            String region) {
        this(accessKeyId, secretKey);
        this.region = region;
    }

    public AmazonS3 newClient() {
        final AWSCredentialsProvider credsProvider =
                new CustomCredentialsProvider();

        // All kinds of stuff in here to configure. For now we'll go with
        // the defaults.
        final ClientConfiguration clientConfig = new ClientConfiguration();
        
    	ArrayList<AWSCredentialsProvider> creds = new ArrayList<AWSCredentialsProvider>();
    	creds.addAll(Arrays.asList(new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                new ProfileCredentialsProvider(),
                new InstanceProfileCredentialsProvider(false)
                )
    	);
    	
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
