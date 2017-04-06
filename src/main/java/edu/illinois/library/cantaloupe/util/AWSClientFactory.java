package edu.illinois.library.cantaloupe.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
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

        String regionStr = region;
        if (regionStr == null || regionStr.length() < 1) {
            regionStr = Regions.DEFAULT_REGION.getName();
        }

        return AmazonS3ClientBuilder.standard().
                withRegion(regionStr).
                withCredentials(credsProvider).
                withClientConfiguration(clientConfig).build();
    }

}
