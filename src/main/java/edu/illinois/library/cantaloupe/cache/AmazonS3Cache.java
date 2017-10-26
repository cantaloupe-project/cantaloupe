package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.illinois.library.cantaloupe.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.util.AWSClientFactory;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>Cache using an Amazon S3 bucket.</p>
 *
 * <p>To improve client-responsiveness, uploads are asynchronous.</p>
 *
 * <p>Keys are named according to the following template:</p>
 *
 * <dl>
 *     <dt>Images</dt>
 *     <dd><code>{@link Key#AMAZONS3CACHE_OBJECT_KEY_PREFIX}/image/{op list
 *     string representation}</code></dd>
 *     <dt>Info</dt>
 *     <dd><code>{@link Key#AMAZONS3CACHE_OBJECT_KEY_PREFIX}/info/{identifier}.json</code></dd>
 * </dl>
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
class AmazonS3Cache implements DerivativeCache {

    /**
     * <p>Wraps a {@link ByteArrayOutputStream} for upload to Amazon S3.</p>
     *
     * <p>N.B. S3 does not allow uploads without a Content-Length header, which
     * is impossible to provide when streaming an unknown amount of data. From
     * the documentation of {@link PutObjectRequest}:</p>
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
     * this class buffers written data in a byte array before uploading it to
     * S3 upon closure. (The upload is submitted to the
     * {@link ThreadPool} in order to allow {@link #close()} to return
     * immediately.)</p>
     */
    private class AmazonS3OutputStream extends OutputStream {

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
        AmazonS3OutputStream(final AmazonS3 s3,
                             final String bucketName,
                             final String objectKey,
                             final ObjectMetadata metadata) {
            this.bucketName = bucketName;
            this.s3 = s3;
            this.objectKey = objectKey;
            this.metadata = metadata;
        }

        @Override
        public void close() {
            // At this point, the client has received all image data, but its
            // progress indicator is still spinning while it waits for the
            // connection to close. Uploading in a separate thread will allow
            // this to happen immediately.
            ThreadPool.getInstance().submit(new AmazonS3Upload(
                    s3, bufferStream, bucketName, objectKey, metadata));
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

    private class AmazonS3Upload implements Runnable {

        private Logger logger = LoggerFactory.getLogger(AmazonS3Upload.class);

        private String bucketName;
        private ByteArrayOutputStream byteStream;
        private ObjectMetadata metadata;
        private String objectKey;
        private AmazonS3 s3;

        /**
         * @param s3         S3 client.
         * @param byteStream Data to upload.
         * @param bucketName S3 bucket name.
         * @param objectKey  S3 object key.
         * @param metadata   S3 object metadata.
         */
        AmazonS3Upload(AmazonS3 s3,
                       ByteArrayOutputStream byteStream,
                       String bucketName,
                       String objectKey,
                       ObjectMetadata metadata) {
            this.bucketName = bucketName;
            this.byteStream = byteStream;
            this.s3 = s3;
            this.metadata = metadata;
            this.objectKey = objectKey;
        }

        @Override
        public void run() {
            byte[] bytes = byteStream.toByteArray();
            metadata.setContentLength(bytes.length);

            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            PutObjectRequest request = new PutObjectRequest(
                    bucketName, objectKey, is, metadata);
            final Stopwatch watch = new Stopwatch();

            logger.info("Uploading {} bytes to {} in bucket {}",
                    bytes.length, request.getKey(), request.getBucketName());
            s3.putObject(request);
            logger.info("Wrote {} bytes to {} in bucket {} in {} msec",
                    bytes.length, request.getKey(), request.getBucketName(),
                    watch.timeElapsed());
        }

    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(AmazonS3Cache.class);

    /** Lazy-initialized by {@link #getClientInstance} */
    private static AmazonS3 client;

    static synchronized AmazonS3 getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            final AWSClientFactory factory = new AWSClientFactory(
                    config.getString(Key.AMAZONS3CACHE_ACCESS_KEY_ID),
                    config.getString(Key.AMAZONS3CACHE_SECRET_KEY),
                    config.getString(Key.AMAZONS3CACHE_BUCKET_REGION));
            client = factory.newClient();
        }
        return client;
    }

    String getBucketName() {
        return Configuration.getInstance().
                getString(Key.AMAZONS3CACHE_BUCKET_NAME);
    }

    @Override
    public Info getImageInfo(Identifier identifier) throws CacheException {
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();
        final String objectKey = getObjectKey(identifier);

        final Stopwatch watch = new Stopwatch();
        try {
            final String jsonStr = s3.getObjectAsString(bucketName, objectKey);
            final Info info = Info.fromJSON(jsonStr);
            LOGGER.info("getImageInfo(): read {} from bucket {} in {} msec",
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
        LOGGER.info("newDerivativeImageInputStream(): bucket: {}; key: {}",
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
        final String bucketName = getBucketName();
        final AmazonS3 s3 = getClientInstance();
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(
                opList.getOutputFormat().getPreferredMediaType().toString());
        return new AmazonS3OutputStream(s3, bucketName, objectKey, metadata);
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
     * @return Value of {@link Key#AMAZONS3CACHE_OBJECT_KEY_PREFIX}
     *         with trailing slash.
     */
    String getObjectKeyPrefix() {
        String prefix = Configuration.getInstance().
                getString(Key.AMAZONS3CACHE_OBJECT_KEY_PREFIX);
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

    @Override
    public void purgeExpired() throws CacheException {
        final Configuration config = Configuration.getInstance();
        final AmazonS3 s3 = getClientInstance();
        final String bucketName = getBucketName();

        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, 0 - config.getInt(Key.CACHE_SERVER_TTL));
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
        LOGGER.info("purgeExpired(): deleted {} of {} items",
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
        LOGGER.info("purge(Identifier): deleted {} items", count);
    }

    /**
     * Uploads the given info to S3 asynchronously and returns immediately.
     *
     * @param identifier Image identifier.
     * @param info       Info to upload to S3.
     */
    @Override
    public void put(Identifier identifier, Info info) throws CacheException {
        final AmazonS3 s3 = getClientInstance();
        final String objectKey = getObjectKey(identifier);
        final String bucketName = getBucketName();

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            info.writeAsJSON(os);

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/json");
            metadata.setContentEncoding("UTF-8");
            metadata.setContentLength(os.size());

            ThreadPool.getInstance().submit(new AmazonS3Upload(
                    s3, os, bucketName, objectKey, metadata));
        } catch (IllegalStateException e) {
            LOGGER.warn("put(): the upload queue is full.");
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

}
