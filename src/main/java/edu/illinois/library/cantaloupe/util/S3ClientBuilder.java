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
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsProfileRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;
import software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * Creates an S3 client using the Builder pattern.
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
public final class S3ClientBuilder {

    /**
     * This region is used when the region provider chain used by {@link
     * #getEffectiveRegion()} is not able to obtain a region.
     */
    private static final Region DEFAULT_REGION = Region.US_EAST_1;

    private URI endpointURI;
    private Region region;
    private String accessKeyID, secretAccessKey;

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
            @Nullable final String accessKeyIDFromConfig,
            @Nullable final String secretKeyFromConfig) {
        final AwsCredentialsProviderChain.Builder builder =
                AwsCredentialsProviderChain.builder();
        builder.addCredentialsProvider(SystemPropertyCredentialsProvider.create());
        builder.addCredentialsProvider(EnvironmentVariableCredentialsProvider.create());
        if (accessKeyIDFromConfig != null && !accessKeyIDFromConfig.isBlank() &&
                secretKeyFromConfig != null && !secretKeyFromConfig.isBlank()) {
            builder.addCredentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
                @Override
                public String accessKeyId() {
                    return accessKeyIDFromConfig;
                }
                @Override
                public String secretAccessKey() {
                    return secretKeyFromConfig;
                }
            }));
        }
        builder.addCredentialsProvider(ProfileCredentialsProvider.create());
        builder.addCredentialsProvider(ContainerCredentialsProvider.builder().build());
        builder.addCredentialsProvider(InstanceProfileCredentialsProvider.builder().build());
        return builder.build();
    }

    /**
     * Returns a region using a similar strategy as the {@link
     * software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain}
     * except the application configuration is consulted between the
     * environment and AWS profile.
     *
     * @return Region, or {@link #DEFAULT_REGION} if none could be found.
     * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/regions/providers/DefaultAwsRegionProviderChain.html">
     *     DefaultAwsRegionProviderChain</a>
     */
    private Region getEffectiveRegion() {
        try {
            return new AwsRegionProviderChain(
                    new SystemSettingsRegionProvider(),
                    () -> region,
                    new AwsProfileRegionProvider(),
                    new InstanceProfileRegionProvider()).getRegion();
        } catch (SdkClientException e) {
            return DEFAULT_REGION;
        }
    }

    /**
     * @param accessKeyID AWS access key ID.
     * @return            The instance.
     */
    public S3ClientBuilder accessKeyID(@Nullable String accessKeyID) {
        this.accessKeyID = accessKeyID;
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
     * @param region Region to use. This is relevant only for AWS endpoints.
     * @return       The instance.
     */
    public S3ClientBuilder region(String region) {
        try {
            this.region = (region != null) ? Region.of(region) : null;
        } catch (IllegalArgumentException | SdkClientException e) {
            this.region = null;
        }
        return this;
    }

    /**
     * @param secretAccessKey AWS secret access key.
     * @return          The instance.
     */
    public S3ClientBuilder secretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
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
                // A region is required even for non-AWS endpoints.
                .region(getEffectiveRegion())
                .credentialsProvider(newCredentialsProvider(accessKeyID, secretAccessKey));
        if (endpointURI != null) {
            builder = builder.endpointOverride(endpointURI);
        }
        return builder.build();
    }

}
