package edu.illinois.library.cantaloupe.util;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Creates an S3 client using the Builder pattern.
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
public final class S3ClientBuilder {

    private URI endpointURI;
    private Region region;
    private String accessKeyID, secretKey;

    /**
     * Returns credentials using a similar strategy as the {@link
     * software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}
     * except the application configuration is consulted between the
     * environment and AWS profile.
     *
     * @param accessKeyIDFromConfig Access key ID from the application
     *                              configuration.
     * @param secretKeyFromConfig   Secret key from the application
     *                              configuration.
     * @see <a href="https://sdk.amazonaws.com/java/api/latest/index.html?software/amazon/awssdk/auth/credentials/AwsCredentialsProvider.html">
     *     AwsCredentialsProvider</a>
     */
    public static AwsCredentialsProvider newCredentialsProvider(
            final String accessKeyIDFromConfig,
            final String secretKeyFromConfig) {
        return AwsCredentialsProviderChain.builder()
                .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
                .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .addCredentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return accessKeyIDFromConfig;
                    }
                    @Override
                    public String secretAccessKey() {
                        return secretKeyFromConfig;
                    }
                }))
                .addCredentialsProvider(ProfileCredentialsProvider.create())
                .addCredentialsProvider(ContainerCredentialsProvider.builder().build())
                .addCredentialsProvider(InstanceProfileCredentialsProvider.builder().build())
                .build();
    }

    /**
     * @param accessKeyID AWS access key ID.
     * @return            The instance.
     */
    public S3ClientBuilder accessKeyID(String accessKeyID) {
        this.accessKeyID = accessKeyID;
        return this;
    }

    /**
     * @param secretKey AWS secret key.
     * @return          The instance.
     */
    public S3ClientBuilder secretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    /**
     * @param uri URI of the S3 endpoint. If not supplied, an AWS endpoint is
     *            used based on {@link #region(String)}.
     * @return    The instance.
     */
    public S3ClientBuilder endpointURI(URI uri) {
        this.endpointURI = uri;
        return this;
    }

    /**
     * @param region Region to use. Relevant only to AWS endpoints; for non-AWS
     *               endpoints, {@link #endpointURI(URI)} must be set.
     * @return       The instance.
     */
    public S3ClientBuilder region(String region) {
        this.region = (region != null && !region.isBlank()) ? Region.of(region) : null;
        return this;
    }

    public S3Client build() {
        final S3Configuration config = S3Configuration.builder()
                .pathStyleAccessEnabled(endpointURI != null)
                .checksumValidationEnabled(false)
                .build();
        software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(config)
                .credentialsProvider(newCredentialsProvider(accessKeyID, secretKey));
        if (endpointURI != null) {
            builder = builder.endpointOverride(endpointURI);
        } else {
            builder = builder.region(region);
        }
        return builder.build();
    }

}
