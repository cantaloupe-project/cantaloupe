package edu.illinois.library.cantaloupe.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

/**
 * Creates an AWS client using the Builder pattern.
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
public final class AWSClientBuilder {

    /**
     * Draws credentials from the parent class' instance variables, which
     * generally come from the application configuration, although not
     * necessarily the same keys all the time.
     */
    private class ConfigurationCredentialsProvider
            implements AWSCredentialsProvider {

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
        // The AWS client will consult each provider in this list in order,
        // and use the first one that works.
        final List<AWSCredentialsProvider> providers = new ArrayList<>();

        // Use anonymous credentials with S3Mock when testing.
        // This is rather crude but will do for now.
        if ("memory".equals(System.getProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT))) {
            providers.add(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()));
        }

        // As a first resort, add a provider that draws from the application
        // configuration.
        AWSCredentialsProvider configProvider =
                new ConfigurationCredentialsProvider();
        String accessKeyId = configProvider.getCredentials().getAWSAccessKeyId();
        if (accessKeyId != null && !accessKeyId.isEmpty()) {
            providers.add(configProvider);
        }

        // Add default providers as fallbacks:
        // https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html?com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html
        providers.add(new DefaultAWSCredentialsProviderChain());

        return new AWSCredentialsProviderChain(providers);
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
