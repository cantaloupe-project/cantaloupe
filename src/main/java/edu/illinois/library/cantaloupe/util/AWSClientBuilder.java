package edu.illinois.library.cantaloupe.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

    private URI endpointURI;
    private String accessKeyID, secretKey;
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;

    /**
     * Returns credentials using a similar strategy as the {@link
     * DefaultAWSCredentialsProviderChain} except the application configuration
     * is consulted after the VM arguments.
     *
     * @param accessKeyIDFromConfig Access key ID from the application
     *                              configuration.
     * @param secretKeyFromConfig   Secret key from the application
     *                              configuration.
     */
    public static AWSCredentialsProvider newCredentialsProvider(
            @Nullable final String accessKeyIDFromConfig,
            @Nullable final String secretKeyFromConfig) {
        // The provider chain will consult each provider in this list in order,
        // and use the first one that returns a non-null access and secret key pair.
        final List<AWSCredentialsProvider> providers = new ArrayList<>();
        providers.add(new EnvironmentVariableCredentialsProvider());
        providers.add(new SystemPropertiesCredentialsProvider());
        providers.add(new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return new AWSCredentials() {
                    @Override
                    public String getAWSAccessKeyId() {
                        return accessKeyIDFromConfig;
                    }

                    @Override
                    public String getAWSSecretKey() {
                        return secretKeyFromConfig;
                    }
                };
            }
            @Override
            public void refresh() {}
        });
        providers.add(new ProfileCredentialsProvider());
        providers.add(new EC2ContainerCredentialsProviderWrapper());
        return new AWSCredentialsProviderChain(providers);
    }

    /**
     * @param accessKeyID AWS access key ID.
     * @return The instance.
     */
    public AWSClientBuilder accessKeyID(@Nullable String accessKeyID) {
        this.accessKeyID = accessKeyID;
        return this;
    }

    /**
     * @param secretKey AWS secret key.
     * @return The instance.
     */
    public AWSClientBuilder secretKey(@Nullable String secretKey) {
        this.secretKey = secretKey;
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

    public AmazonS3 build() {
        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(getEndpointConfiguration())
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(getClientConfiguration())
                .withCredentials(newCredentialsProvider(accessKeyID, secretKey))
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
