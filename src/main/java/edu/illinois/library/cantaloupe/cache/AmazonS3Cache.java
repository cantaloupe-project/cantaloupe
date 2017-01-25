package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
class AmazonS3Cache implements DerivativeCache {

    /**
     * <p>S3 does not allow uploads without a Content-Length header, which is
     * impossible to provide when streaming. From the documentation of
     * {@link PutObjectRequest}:</p>
     *
     * <blockquote>"When uploading directly from an input stream, content
     * length must be specified before data can be uploaded to Amazon S3. If
     * not provided, the library will have to buffer the contents of the input
     * stream in order to calculate it. Amazon S3 explicitly requires that the
     * content length be sent in the request headers before any of the data is
     * sent."</blockquote>
     *
     * <p>Since it is therefore not possible to write an OutputStream of
     * unknown length to the S3 client as the {@link Cache} interface requires,
     * this output stream buffers written data in a byte array before uploading
     * it to S3.</p>
     */
    private static class AmazonS3OutputStream extends OutputStream {

        private static Logger logger = LoggerFactory.
                getLogger(AmazonS3OutputStream.class);

        // Buffers the data written to the instance.
        private final ByteArrayOutputStream bufferStream =
                new ByteArrayOutputStream();
        private final String bucketName;
        private final ObjectMetadata metadata;
        private final String objectKey;
        private final AmazonS3 s3;
        private final Set<String> uploadingKeys;

        /**
         * @param s3            S3 client.
         * @param bucketName    S3 bucket name.
         * @param objectKey     S3 object key.
         * @param metadata      S3 object metadata.
         * @param uploadingKeys Set of keys of objects currently being uploaded
         *                      to S3 in any thread.
         */
        AmazonS3OutputStream(final AmazonS3 s3,
                             final String bucketName,
                             final String objectKey,
                             final ObjectMetadata metadata,
                             final Set<String> uploadingKeys) {
            this.bucketName = bucketName;
            this.s3 = s3;
            this.objectKey = objectKey;
            this.metadata = metadata;
            this.uploadingKeys = uploadingKeys;
        }

        @Override
        public void close() throws IOException {
            final byte[] bytes = bufferStream.toByteArray();
            metadata.setContentLength(bytes.length);

            final ByteArrayInputStream s3Stream = new ByteArrayInputStream(
                    bytes);
            try {
                final Stopwatch watch = new Stopwatch();
                final PutObjectRequest request = new PutObjectRequest(
                        bucketName, objectKey, s3Stream, metadata);
                s3.putObject(request);
                logger.info("Wrote {} bytes to {} in bucket {} in {} msec",
                        bytes.length, objectKey, bucketName,
                        watch.timeElapsed());
            } catch (AmazonS3Exception e) {
                throw new IOException(e.getMessage(), e);
            } finally {
                uploadingKeys.remove(objectKey);
            }
        }

        @Override
        public void flush() throws IOException {
            bufferStream.flush();
        }

        @Override
        public void write(int b) throws IOException {
            bufferStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            bufferStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            bufferStream.write(b, off, len);
        }

    }

    private static Logger logger = LoggerFactory.getLogger(AmazonS3Cache.class);

    static final String ACCESS_KEY_ID_CONFIG_KEY =
            "AmazonS3Cache.access_key_id";
    static final String BUCKET_NAME_CONFIG_KEY = "AmazonS3Cache.bucket.name";
    static final String BUCKET_REGION_CONFIG_KEY =
            "AmazonS3Cache.bucket.region";
    static final String OBJECT_KEY_PREFIX_CONFIG_KEY =
            "AmazonS3Cache.object_key_prefix";
    static final String SECRET_KEY_CONFIG_KEY = "AmazonS3Cache.secret_key";

    /** Lazy-initialized by {@link #getClientInstance} */
    private static AmazonS3 client;

    private static final Set<String> uploadingKeys =
            new ConcurrentSkipListSet<>();

    static synchronized AmazonS3 getClientInstance() {
        if (client == null) {
            final Configuration config = ConfigurationFactory.getInstance();

            class ConfigFileCredentials implements AWSCredentials {
                @Override
                public String getAWSAccessKeyId() {
                    return config.getString(ACCESS_KEY_ID_CONFIG_KEY);
                }

                @Override
                public String getAWSSecretKey() {
                    return config.getString(SECRET_KEY_CONFIG_KEY);
                }
            }
            AWSCredentials credentials = new ConfigFileCredentials();
            client = new AmazonS3Client(credentials);

            final String regionName = config.
                    getString(BUCKET_REGION_CONFIG_KEY);
            if (regionName != null && regionName.length() > 0) {
                Regions regions = Regions.fromName(regionName);
                Region region = Region.getRegion(regions);
                logger.info("Using region: {}", region);
                client.setRegion(region);
            }
        }
        return client;
    }

    /**
     * Does nothing, as this cache is always clean.
     */
    @Override
    public void cleanUp() {}

    String getBucketName() {
        return ConfigurationFactory.getInstance().
                getString(BUCKET_NAME_CONFIG_KEY);
    }

    @Override
    public ImageInfo getImageInfo(Identifier identifier) throws CacheException {
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();
        final String objectKey = getObjectKey(identifier);
        try {
            final Stopwatch watch = new Stopwatch();
            final S3Object object = s3.getObject(
                    new GetObjectRequest(bucketName, objectKey));
            final ImageInfo info = ImageInfo.fromJson(object.getObjectContent());
            logger.info("getImageInfo(): read {} from bucket {} in {} msec",
                    objectKey, bucketName, watch.timeElapsed());
            return info;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw new CacheException(e.getMessage(), e);
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws CacheException {
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();
        final String objectKey = getObjectKey(opList);
        logger.info("newDerivativeImageInputStream(): bucket: {}; key: {}",
                bucketName, objectKey);
        try {
            final S3Object object = s3.getObject(
                    new GetObjectRequest(bucketName, objectKey));
            return object.getObjectContent();
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList opList)
            throws CacheException {
        final String objectKey = getObjectKey(opList);
        if (!uploadingKeys.contains(objectKey)) {
            uploadingKeys.add(objectKey);
            final String bucketName = getBucketName();
            final AmazonS3 s3 = getClientInstance();
            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(
                    opList.getOutputFormat().getPreferredMediaType().toString());
            return new AmazonS3OutputStream(s3, bucketName, objectKey,
                    metadata, uploadingKeys);
        }
        return new NullOutputStream();
    }

    /**
     * @param identifier
     * @return Object key of the serialized ImageInfo associated with the given
     *         identifier.
     */
    String getObjectKey(Identifier identifier) {
        return getObjectKeyPrefix() + "info/" + identifier.toString() + ".json";
    }

    /**
     * @param opList
     * @return Object key of the image associated with the given operation list.
     */
    String getObjectKey(OperationList opList) {
        return getObjectKeyPrefix() + "image/" + opList.toString();
    }

    /**
     * @return Value of {@link #OBJECT_KEY_PREFIX_CONFIG_KEY} with trailing
     *         slash.
     */
    String getObjectKeyPrefix() {
        String prefix = ConfigurationFactory.getInstance().
                getString(OBJECT_KEY_PREFIX_CONFIG_KEY);
        if (prefix.length() < 1 || prefix.equals("/")) {
            return "";
        }
        return StringUtils.stripEnd(prefix, "/") + "/";
    }

    @Override
    public void purge() throws CacheException {
        final AmazonS3 s3 = getClientInstance();
        final ObjectListing listing = s3.listObjects(getBucketName(),
                getObjectKeyPrefix());
        int count = 0;
        for (S3ObjectSummary summary : listing.getObjectSummaries()) {
            s3.deleteObject(getBucketName(), summary.getKey());
            count++;
        }
        logger.info("purge(): deleted {} items", count);
    }

    @Override
    public void purge(final OperationList opList) {
        purge(getObjectKey(opList));
    }

    private void purge(final String objectKey) {
        final AmazonS3 s3 = getClientInstance();
        s3.deleteObject(getBucketName(), objectKey);
    }

    @Override
    public void purgeExpired() throws CacheException {
        final Configuration config = ConfigurationFactory.getInstance();
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();

        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, 0 - config.getInt(Cache.TTL_CONFIG_KEY));
        Date cutoffDate = c.getTime();

        final S3Objects objects = S3Objects.withPrefix(s3, bucketName,
                getObjectKeyPrefix());
        int count = 0, deletedCount = 0;
        for (S3ObjectSummary summary : objects) {
            count++;
            if (summary.getLastModified().before(cutoffDate)) {
                deletedCount++;
                s3.deleteObject(bucketName, summary.getKey());
            }
        }
        logger.info("purgeExpired(): deleted {} of {} items",
                deletedCount, count);
    }

    @Override
    public void purge(final Identifier identifier) {
        // purge the info
        purge(getObjectKey(identifier));

        // purge images
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();
        final S3Objects objects = S3Objects.withPrefix(s3, bucketName,
                getObjectKeyPrefix() + "image/" + identifier.toString());
        int count = 0;
        for (S3ObjectSummary summary : objects) {
            count++;
            s3.deleteObject(bucketName, summary.getKey());
        }
        logger.info("purge(Identifier): deleted {} items", count);
    }

    @Override
    public void put(Identifier identifier, ImageInfo imageInfo)
            throws CacheException {
        final String objectKey = getObjectKey(identifier);
        if (!uploadingKeys.contains(objectKey)) {
            uploadingKeys.add(objectKey);
            try {
                final AmazonS3 s3 = getClientInstance();
                final String bucketName = getBucketName();
                final String json = imageInfo.toJson();
                final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

                final ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("application/json");
                metadata.setContentEncoding("UTF-8");
                metadata.setContentLength(jsonBytes.length);

                final InputStream s3Stream = new ByteArrayInputStream(jsonBytes);

                final Stopwatch watch = new Stopwatch();
                final PutObjectRequest request = new PutObjectRequest(
                        bucketName, objectKey, s3Stream, metadata);
                s3.putObject(request);
                logger.info("put(): wrote {} to bucket {} in {} msec",
                        objectKey, bucketName, watch.timeElapsed());
            } catch (AmazonS3Exception | JsonProcessingException e) {
                throw new CacheException(e.getMessage(), e);
            } finally {
                uploadingKeys.remove(objectKey);
            }
        } else {
            logger.debug("put(): {} is being written in another " +
                    "thread; aborting.");
        }
    }

}
