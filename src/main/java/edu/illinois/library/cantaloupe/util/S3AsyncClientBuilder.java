package edu.illinois.library.cantaloupe.util;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsProfileRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;
import software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Creates an S3 async client using the Builder pattern. The client is backed
 * by the AWS Common Runtime (CRT) async HTTP client, which provides
 * non-blocking I/O and higher throughput than the URL-connection-based
 * client.
 *
 * @see <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration-crt.html">
 *     Configure the AWS CRT-based HTTP client</a>
 * @see S3ClientBuilder
 */
public final class S3AsyncClientBuilder {

    /**
     * This region is used when the region provider chain used by {@link
     * #getEffectiveRegion()} is not able to obtain a region.
     */
    private static final Region DEFAULT_REGION = Region.US_EAST_1;

    private URI endpointURI;
    private Region region;
    private String accessKeyID, secretAccessKey;

    /**
     * Returns a region using a similar strategy as the {@link
     * software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain}
     * except the application configuration is consulted between the
     * environment and AWS profile.
     *
     * @return Region, or {@link #DEFAULT_REGION} if none could be found.
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
    public S3AsyncClientBuilder accessKeyID(String accessKeyID) {
        this.accessKeyID = accessKeyID;
        return this;
    }

    /**
     * @param uri URI of the S3 endpoint. If not supplied, an AWS endpoint is
     *            used based on {@link #region(String)}.
     * @return    The instance.
     */
    public S3AsyncClientBuilder endpointURI(URI uri) {
        this.endpointURI = uri;
        return this;
    }

    /**
     * @param region Region to use. This is relevant only for AWS endpoints.
     * @return       The instance.
     */
    public S3AsyncClientBuilder region(String region) {
        try {
            this.region = (region != null) ? Region.of(region) : null;
        } catch (IllegalArgumentException | SdkClientException e) {
            this.region = null;
        }
        return this;
    }

    /**
     * @param secretAccessKey AWS secret access key.
     * @return                The instance.
     */
    public S3AsyncClientBuilder secretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
        return this;
    }

    public S3AsyncClient build() {
        final S3Configuration config = S3Configuration.builder()
                .pathStyleAccessEnabled(endpointURI != null)
                .checksumValidationEnabled(false)
                .build();
        software.amazon.awssdk.services.s3.S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
                .serviceConfiguration(config)
                // A region is required even for non-AWS endpoints.
                .region(getEffectiveRegion())
                .credentialsProvider(S3ClientBuilder.newCredentialsProvider(accessKeyID, secretAccessKey));
        if (endpointURI != null) {
            builder = builder.endpointOverride(endpointURI);
        }
        return builder.build();
    }

}
