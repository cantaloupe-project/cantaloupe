package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * <p>Cache using an S3 bucket.</p>
 *
 * <p>To improve client-responsiveness, uploads are asynchronous.</p>
 *
 * <p>Object keys are named according to the following template:</p>
 *
 * <dl>
 *     <dt>Images</dt>
 *     <dd><code>{@link Key#S3CACHE_OBJECT_KEY_PREFIX}/image/{op list string
 *     representation}</code></dd>
 *     <dt>Info</dt>
 *     <dd><code>{@link Key#S3CACHE_OBJECT_KEY_PREFIX}/info/{identifier}.json</code></dd>
 * </dl>
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 * @since 3.0
 */
class S3Cache implements DerivativeCache {

    /**
     * <p>Wraps a {@link ByteArrayOutputStream} for upload to S3.</p>
     *
     * <p>N.B.: S3 does not allow uploads without a <code>Content-Length</code>
     * header, which is impossible to provide when streaming an unknown amount
     * of data (which this class is going to be doing all the time). From the
     * documentation of {@link PutObjectRequest}:</p>
     *
     * <blockquote>"When uploading directly from an input stream, content
     * length must be specified before data can be uploaded to Amazon S3. If
     * not provided, the library will have to buffer the contents of the input
     * stream in order to calculate it. Amazon S3 explicitly requires that the
     * content length be sent in the request headers before any of the data is
     * sent."</blockquote>
     *
     * <p>Since it's not possible to write an {@link OutputStream} of unknown
     * length to the S3 client as the {@link Cache} interface requires, this
     * class buffers written data in a byte array before uploading it to S3
     * upon closure. (The upload is submitted to the
     * {@link ThreadPool#getInstance() application thread pool} in order to
     * endable {@link #close()} to return immediately.)</p>
     */
    private static class S3OutputStream extends OutputStream {

        private final ByteArrayOutputStream bufferStream =
                new ByteArrayOutputStream();
        private final String bucketName;
        private final ObjectMetadata metadata;
        private final String objectKey;
        private final AmazonS3 s3;

        /**
         * @param s3         S3 client.
         * @param bucketName S3 bucket name.
         * @param objectKey  S3 object key.
         * @param metadata   S3 object metadata.
         */
        S3OutputStream(final AmazonS3 s3,
                       final String bucketName,
                       final String objectKey,
                       final ObjectMetadata metadata) {
            this.bucketName = bucketName;
            this.s3 = s3;
            this.objectKey = objectKey;
            this.metadata = metadata;
        }

        @Override
        public void close() throws IOException {
            try {
                bufferStream.close();

                byte[] data = bufferStream.toByteArray();
                if (data.length > 0) {
                    // At this point, the client has received all image data,
                    // but it is still waiting for the connection to close.
                    // Uploading in a separate thread will allow this to happen
                    // immediately.
                    ThreadPool.getInstance().submit(new S3Upload(
                            s3, data, bucketName, objectKey, metadata));
                }
            } finally {
                super.close();
            }
        }

        @Override
        public void flush() throws IOException {
            bufferStream.flush();
        }

        @Override
        public void write(int b) {
            bufferStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            bufferStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            bufferStream.write(b, off, len);
        }

    }

    private static class S3Upload implements Runnable {

        private static final Logger UPLOAD_LOGGER =
                LoggerFactory.getLogger(S3Upload.class);

        private String bucketName;
        private byte[] data;
        private ObjectMetadata metadata;
        private String objectKey;
        private AmazonS3 s3;

        /**
         * @param s3         S3 client.
         * @param data       Data to upload.
         * @param bucketName S3 bucket name.
         * @param objectKey  S3 object key.
         * @param metadata   S3 object metadata.
         */
        S3Upload(AmazonS3 s3,
                 byte[] data,
                 String bucketName,
                 String objectKey,
                 ObjectMetadata metadata) {
            this.bucketName = bucketName;
            this.data = data;
            this.s3 = s3;
            this.metadata = metadata;
            this.objectKey = objectKey;
        }

        @Override
        public void run() {
            if (data.length > 0) {
                metadata.setContentLength(data.length);

                ByteArrayInputStream is = new ByteArrayInputStream(data);
                PutObjectRequest request = new PutObjectRequest(
                        bucketName, objectKey, is, metadata);
                final Stopwatch watch = new Stopwatch();

                UPLOAD_LOGGER.info("Uploading {} bytes to {} in bucket {}",
                        data.length, request.getKey(), request.getBucketName());

                s3.putObject(request);

                UPLOAD_LOGGER.info("Wrote {} bytes to {} in bucket {} in {} msec",
                        data.length, request.getKey(), request.getBucketName(),
                        watch.timeElapsed());
            } else {
                UPLOAD_LOGGER.debug("No data to upload; returning");
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(S3Cache.class);

    /**
     * Lazy-initialized by {@link #getClientInstance}.
     */
    private static AmazonS3 client;

    static synchronized AmazonS3 getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();

            URI endpointURI = null;
            try {
                endpointURI = new URI(config.getString(Key.S3CACHE_ENDPOINT));
            } catch (URISyntaxException e) {
                LOGGER.error("Invalid URI for {}: {}",
                        Key.S3CACHE_ENDPOINT, e.getMessage());
            }

            client = new AWSClientBuilder()
                    .endpointURI(endpointURI)
                    .accessKeyID(config.getString(Key.S3CACHE_ACCESS_KEY_ID))
                    .secretKey(config.getString(Key.S3CACHE_SECRET_KEY))
                    .maxConnections(config.getInt(Key.S3CACHE_MAX_CONNECTIONS, 0))
                    .build();
        }
        return client;
    }

    /**
     * @return Earliest valid date, with second resolution.
     */
    private static Date getEarliestValidDate() {
        return Date.from(getEarliestValidInstant());
    }

    /**
     * @return Earliest valid instant, with second resolution.
     */
    private static Instant getEarliestValidInstant() {
        final Configuration config = Configuration.getInstance();
        final long ttl = config.getInt(Key.DERIVATIVE_CACHE_TTL);
        return (ttl > 0) ?
                Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(ttl) :
                Instant.EPOCH;
    }

    private static boolean isValid(ObjectMetadata metadata) {
        return isValid(metadata.getLastModified());
    }

    private static boolean isValid(S3ObjectSummary summary) {
        return isValid(summary.getLastModified());
    }

    private static boolean isValid(Date lastModified) {
        Instant earliestAllowed = getEarliestValidInstant();
        return lastModified.toInstant().isAfter(earliestAllowed);
    }

    String getBucketName() {
        return Configuration.getInstance().getString(Key.S3CACHE_BUCKET_NAME);
    }

    @Override
    public Info getImageInfo(Identifier identifier) throws IOException {
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();
        final String objectKey = getObjectKey(identifier);

        final Stopwatch watch = new Stopwatch();
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, objectKey);
            request.setModifiedSinceConstraint(getEarliestValidDate());
            S3Object object = s3.getObject(request);

            if (object != null) {
                try (InputStream is =
                             new BufferedInputStream(object.getObjectContent())) {
                    final Info info = Info.fromJSON(is);
                    LOGGER.info("getImageInfo(): read {} from bucket {} in {}",
                            objectKey, bucketName, watch);
                    return info;
                }
            } else {
                LOGGER.debug("{} in bucket {} is invalid; purging asynchronously",
                        objectKey, bucketName);
                purgeAsync(bucketName, objectKey);
            }
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();
        final String objectKey = getObjectKey(opList);
        LOGGER.info("newDerivativeImageInputStream(): bucket: {}; key: {}",
                bucketName, objectKey);
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, objectKey);
            request.setModifiedSinceConstraint(getEarliestValidDate());
            S3Object object = s3.getObject(request);

            if (object != null) {
                return object.getObjectContent();
            } else {
                LOGGER.debug("{} in bucket {} is invalid; purging asynchronously",
                        objectKey, bucketName);
                purgeAsync(bucketName, objectKey);
            }
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList opList) {
        final String objectKey = getObjectKey(opList);
        final String bucketName = getBucketName();
        final AmazonS3 s3 = getClientInstance();
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(
                opList.getOutputFormat().getPreferredMediaType().toString());
        return new S3OutputStream(s3, bucketName, objectKey, metadata);
    }

    /**
     * @param identifier
     * @return Object key of the serialized Info associated with the given
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
     * @return Value of {@link Key#S3CACHE_OBJECT_KEY_PREFIX}
     *         with trailing slash.
     */
    String getObjectKeyPrefix() {
        String prefix = Configuration.getInstance().
                getString(Key.S3CACHE_OBJECT_KEY_PREFIX, "");
        if (prefix.isEmpty() || prefix.equals("/")) {
            return "";
        }
        return StringUtils.stripEnd(prefix, "/") + "/";
    }

    @Override
    public void purge() {
        final AmazonS3 s3 = getClientInstance();

        ObjectListing listing = s3.listObjects(
                getBucketName(),
                getObjectKeyPrefix());
        int count = 0;

        while (true) {
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                try {
                    s3.deleteObject(getBucketName(), summary.getKey());
                    count++;
                } catch (AmazonS3Exception e) {
                    LOGGER.warn("purge(): {}", e.getMessage());
                }
            }

            if (listing.isTruncated()) {
                LOGGER.debug("purge(): retrieving next batch");
                listing = s3.listNextBatchOfObjects(listing);
            } else {
                break;
            }
        }

        LOGGER.info("purge(): deleted {} items", count);
    }

    @Override
    public void purge(final OperationList opList) {
        purge(getObjectKey(opList));
    }

    private void purge(final String objectKey) {
        final AmazonS3 s3 = getClientInstance();
        s3.deleteObject(getBucketName(), objectKey);
    }

    private void purgeAsync(final String bucketName, final String key) {
        ThreadPool.getInstance().submit(() -> {
            final AmazonS3 s3 = getClientInstance();

            LOGGER.debug("purgeAsync(): deleting {} from bucket {}",
                    key, bucketName);
            s3.deleteObject(bucketName, key);
            return null;
        });
    }

    @Override
    public void purgeInvalid() {
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();

        ObjectListing listing = s3.listObjects(
                getBucketName(),
                getObjectKeyPrefix());
        int count = 0, deletedCount = 0;

        while (true) {
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                count++;
                if (!isValid(summary)) {
                    s3.deleteObject(bucketName, summary.getKey());
                    deletedCount++;
                }
            }

            if (listing.isTruncated()) {
                LOGGER.debug("purgeInvalid(): retrieving next batch");
                listing = s3.listNextBatchOfObjects(listing);
            } else {
                break;
            }
        }

        LOGGER.info("purgeInvalid(): deleted {} of {} items",
                deletedCount, count);
    }

    @Override
    public void purge(final Identifier identifier) {
        // purge the info
        purge(getObjectKey(identifier));

        // purge images
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();

        ObjectListing listing = s3.listObjects(
                getBucketName(),
                getObjectKeyPrefix() + "image/" + identifier.toString());
        int count = 0;

        while (true) {
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                s3.deleteObject(bucketName, summary.getKey());
                count++;
            }

            if (listing.isTruncated()) {
                LOGGER.debug("purge(Identifier): retrieving next batch");
                listing = s3.listNextBatchOfObjects(listing);
            } else {
                break;
            }
        }
        LOGGER.info("purge(Identifier): deleted {} items", count);
    }

    /**
     * Uploads the given info to S3.
     *
     * @param identifier Image identifier.
     * @param info       Info to upload to S3.
     */
    @Override
    public void put(Identifier identifier, Info info) throws IOException {
        final AmazonS3 s3 = getClientInstance();
        final String objectKey = getObjectKey(identifier);
        final String bucketName = getBucketName();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            info.writeAsJSON(os);

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/json");
            metadata.setContentEncoding("UTF-8");
            metadata.setContentLength(os.size());

            new S3Upload(s3, os.toByteArray(), bucketName, objectKey,
                    metadata).run();
        }
    }

}
