package edu.illinois.library.cantaloupe.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/**
 * Creates an AWS client using the Builder pattern.
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
public final class AWSClientBuilder {

    private static final int DEFAULT_CLIENT_EXECUTION_TIMEOUT_MSEC = 10 * 60 * 1000;
    private static final long DEFAULT_CONNECTION_TTL_MSEC          = 30 * 60 * 1000;
    private static final int DEFAULT_MAX_CONNECTIONS               = 200;
    private static final boolean DEFAULT_USE_TCP_KEEPALIVE         = true;

    private String accessKeyID;
    private URI endpointURI;
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
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
     * @param uri URI of the S3 endpoint. If not supplied, the AWS S3 endpoint
     *            will be used.
     * @return The instance.
     */
    public AWSClientBuilder endpointURI(URI uri) {
        this.endpointURI = uri;
        return this;
    }

    /**
     * @param maxConnections Maximum concurrent connections to AWS. Supply
     *                       {@literal 0} to use the default.
     * @return The instance.
     */
    public AWSClientBuilder maxConnections(int maxConnections) {
        this.maxConnections = (maxConnections > 0) ?
                maxConnections : DEFAULT_MAX_CONNECTIONS;
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
        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(getEndpointConfiguration())
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(getClientConfiguration())
                .withCredentials(getCredentialsProvider())
                .build();
    }

    private ClientConfiguration getClientConfiguration() {
        final ClientConfiguration clientConfig = new ClientConfiguration();
        // The AWS SDK default is 50.
        clientConfig.setMaxConnections(maxConnections);
        clientConfig.setConnectionTTL(DEFAULT_CONNECTION_TTL_MSEC);
        clientConfig.setClientExecutionTimeout(DEFAULT_CLIENT_EXECUTION_TIMEOUT_MSEC);
        clientConfig.setUseTcpKeepAlive(DEFAULT_USE_TCP_KEEPALIVE);
        return clientConfig;
    }

    private AWSCredentialsProvider getCredentialsProvider() {
        if ((accessKeyID == null || accessKeyID.isEmpty()) &&
                (secretKey == null || secretKey.isEmpty())) {
            return new AWSStaticCredentialsProvider(new AnonymousAWSCredentials());
        }

        final List<AWSCredentialsProvider> creds = new ArrayList<>(
                Arrays.asList(
                        new EnvironmentVariableCredentialsProvider(),
                        new SystemPropertiesCredentialsProvider(),
                        new ProfileCredentialsProvider(),
                        new InstanceProfileCredentialsProvider(false)));

        final AWSCredentialsProvider provider = new AWSCredentialsProvider () {
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

        };

        if (!provider.getCredentials().getAWSAccessKeyId().isEmpty()) {
            creds.add(0, provider);
        }
        return new AWSCredentialsProviderChain(creds);
    }

    /**
     * @return New instance, or {@literal null} if using the default endpoint.
     */
    private AwsClientBuilder.EndpointConfiguration getEndpointConfiguration() {
        AwsClientBuilder.EndpointConfiguration endpointConfig = null;
        if (endpointURI != null) {
            endpointConfig = new AwsClientBuilder.EndpointConfiguration(
                    endpointURI.toString(), null);
        }
        return endpointConfig;
    }

}
