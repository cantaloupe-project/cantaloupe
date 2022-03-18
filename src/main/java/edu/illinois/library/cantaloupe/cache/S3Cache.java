package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.util.S3ClientBuilder;
import edu.illinois.library.cantaloupe.util.S3Utils;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/">
 *     AWS SDK for Java API Reference</a>
 * @author Alex Dolski UIUC
 * @since 3.0
 */
class S3Cache implements DerivativeCache {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(S3Cache.class);

    private static final String IMAGE_KEY_PREFIX = "image/";
    private static final String INFO_EXTENSION   = ".json";
    private static final String INFO_KEY_PREFIX  = "info/";

    /**
     * Lazy-initialized by {@link #getClientInstance}.
     */
    private static S3Client client;

    static synchronized S3Client getClientInstance() {
        if (client == null) {
            final Configuration config = Configuration.getInstance();
            final String endpointStr = config.getString(Key.S3CACHE_ENDPOINT);
            URI endpointURI = null;
            if (endpointStr != null) {
                try {
                    endpointURI = new URI(endpointStr);
                } catch (URISyntaxException e) {
                    LOGGER.error("Invalid URI for {}: {}",
                            Key.S3CACHE_ENDPOINT, e.getMessage());
                }
            }
            client = new S3ClientBuilder()
                    .accessKeyID(config.getString(Key.S3CACHE_ACCESS_KEY_ID))
                    .secretAccessKey(config.getString(Key.S3CACHE_SECRET_KEY))
                    .endpointURI(endpointURI)
                    .region(config.getString(Key.S3CACHE_REGION))
                    .build();
        }
        return client;
    }

    /**
     * @return Earliest valid instant, with second resolution.
     */
    private static Instant earliestValidInstant() {
        final Configuration config = Configuration.getInstance();
        final long ttl = config.getLong(Key.DERIVATIVE_CACHE_TTL);
        return (ttl > 0) ? Instant.now().minusSeconds(ttl) : Instant.EPOCH;
    }

    private static boolean isValid(S3Object object) {
        return isValid(object.lastModified());
    }

    private static boolean isValid(Instant lastModified) {
        Instant earliestAllowed = earliestValidInstant();
        return lastModified.isAfter(earliestAllowed);
    }

    String getBucketName() {
        return Configuration.getInstance().getString(Key.S3CACHE_BUCKET_NAME);
    }

    @Override
    public Optional<Info> getInfo(Identifier identifier) throws IOException {
        final S3Client client   = getClientInstance();
        final String bucketName = getBucketName();
        final String objectKey  = getObjectKey(identifier);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .ifModifiedSince(earliestValidInstant())
                .build();
        final Stopwatch watch = new Stopwatch();
        try (ResponseInputStream<GetObjectResponse> is = client.getObject(request)) {
            // This extra validity check may be needed with minio server
            if (is != null && is.response().lastModified().isAfter(earliestValidInstant())) {
                final Info info = Info.fromJSON(is);
                // Populate the serialization timestamp if it is not already,
                // as suggested by the method contract.
                if (info.getSerializationTimestamp() == null) {
                    info.setSerializationTimestamp(is.response().lastModified());
                }
                LOGGER.debug("getInfo(): read {} from bucket {} in {}",
                        objectKey, bucketName, watch);
                touchAsync(objectKey);
                return Optional.of(info);
            } else {
                consumeStreamAsync(is);
                LOGGER.debug("{} in bucket {} is invalid; purging asynchronously",
                        objectKey, bucketName);
                purgeAsync(bucketName, objectKey);
            }
        } catch (S3Exception e) {
            if (e.statusCode() != 304 && e.statusCode() != 404) {
                throw new IOException(e);
            }
        } catch (SdkException e) {
            throw new IOException(e);
        }
        return Optional.empty();
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        final S3Client client   = getClientInstance();
        final String bucketName = getBucketName();
        final String objectKey  = getObjectKey(opList);
        LOGGER.debug("newDerivativeImageInputStream(): bucket: {}; key: {}",
                bucketName, objectKey);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .ifModifiedSince(earliestValidInstant())
                .build();
        try {
            ResponseInputStream<GetObjectResponse> is = client.getObject(request);
            // This extra validity check may be needed with minio server
            if (is != null && is.response().lastModified().isAfter(earliestValidInstant())) {
                touchAsync(objectKey);
                return is;
            } else {
                consumeStreamAsync(is);
                LOGGER.debug("{} in bucket {} is invalid; purging asynchronously",
                        objectKey, bucketName);
                purgeAsync(bucketName, objectKey);
            }
        } catch (S3Exception e) {
            if (e.statusCode() != 304 && e.statusCode() != 404) {
                throw new IOException(e);
            }
        } catch (SdkException e) {
            throw new IOException(e);
        }
        return null;
    }

    @Override
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList opList) {
        final String objectKey  = getObjectKey(opList);
        final String bucketName = getBucketName();
        final S3Client client   = getClientInstance();
        return new S3MultipartAsyncOutputStream(client, bucketName, objectKey,
                opList.getOutputFormat().getPreferredMediaType().toString());
    }

    /**
     * @return Object key of the serialized {@link Info} associated with the
     *         given identifier.
     */
    String getObjectKey(Identifier identifier) {
        return getObjectKeyPrefix() + INFO_KEY_PREFIX +
                StringUtils.md5(identifier.toString()) + INFO_EXTENSION;
    }

    /**
     * @return Object key of the derivative image associated with the given
     *         operation list.
     */
    String getObjectKey(OperationList opList) {
        final String idHash  = StringUtils.md5(opList.getIdentifier().toString());
        final String opsHash = StringUtils.md5(opList.toString());

        String extension = "";
        Encode encode = (Encode) opList.getFirst(Encode.class);
        if (encode != null) {
            extension = "." + encode.getFormat().getPreferredExtension();
        }
        return getObjectKeyPrefix() + IMAGE_KEY_PREFIX + idHash + "/" +
                opsHash + extension;
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
        final S3Client client       = getClientInstance();
        final String bucketName     = getBucketName();
        final AtomicInteger counter = new AtomicInteger();

        S3Utils.walkObjects(client, bucketName, getObjectKeyPrefix(), (object) -> {
            try {
                client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(object.key())
                        .build());
                counter.incrementAndGet();
            } catch (S3Exception e) {
                LOGGER.warn("purge(): {}", e.getMessage());
            }
        });
        LOGGER.debug("purge(): deleted {} items", counter.get());
    }

    @Override
    public void purge(final Identifier identifier) {
        // purge the info
        purge(getObjectKey(identifier));

        // purge images
        final S3Client client       = getClientInstance();
        final String bucketName     = getBucketName();
        final String prefix         = getObjectKeyPrefix() + IMAGE_KEY_PREFIX +
                StringUtils.md5(identifier.toString());
        final AtomicInteger counter = new AtomicInteger();

        S3Utils.walkObjects(client, bucketName, prefix, (object) -> {
            LOGGER.trace("purge(Identifier): deleting {}", object.key());
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(object.key())
                    .build());
            counter.incrementAndGet();
        });
        LOGGER.debug("purge(Identifier): deleted {} items", counter.get());
    }

    @Override
    public void purge(final OperationList opList) {
        purge(getObjectKey(opList));
    }

    private void purge(final String objectKey) {
        final S3Client client = getClientInstance();
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(getBucketName())
                .key(objectKey)
                .build());
    }

    private void purgeAsync(final String bucketName, final String key) {
        TaskQueue.getInstance().submit(() -> {
            final S3Client client = getClientInstance();
            LOGGER.debug("purgeAsync(): deleting {} from bucket {}",
                    key, bucketName);
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return null;
        });
    }

    @Override
    public void purgeInfos() {
        final S3Client client       = getClientInstance();
        final String bucketName     = getBucketName();
        final String prefix         = getObjectKeyPrefix() + INFO_KEY_PREFIX;
        final AtomicInteger counter = new AtomicInteger();

        S3Utils.walkObjects(client, bucketName, prefix, (object) -> {
            LOGGER.trace("purgeInfos(): deleting {}", object.key());
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(object.key())
                    .build());
            counter.incrementAndGet();
        });
        LOGGER.debug("purgeInfos(): deleted {} items", counter.get());
    }

    @Override
    public void purgeInvalid() {
        final S3Client client              = getClientInstance();
        final String bucketName            = getBucketName();
        final AtomicInteger counter        = new AtomicInteger();
        final AtomicInteger deletedCounter = new AtomicInteger();

        S3Utils.walkObjects(client, bucketName, getObjectKeyPrefix(), (object) -> {
            counter.incrementAndGet();
            if (!isValid(object)) {
                try {
                    client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(object.key())
                            .build());
                    deletedCounter.incrementAndGet();
                } catch (S3Exception e) {
                    LOGGER.warn("purgeInvalid(): {}", e.getMessage());
                }
            }
        });
        LOGGER.debug("purgeInvalid(): deleted {} of {} items",
                deletedCounter.get(), counter.get());
    }

    /**
     * Uploads the given info to S3.
     *
     * @param identifier Image identifier.
     * @param info       Info to upload to S3.
     */
    @Override
    public void put(Identifier identifier, Info info) throws IOException {
        if (!info.isPersistable()) {
            LOGGER.debug("put(): info for {} is incomplete; ignoring",
                    identifier);
            return;
        }
        put(identifier, info.toJSON());
    }

    /**
     * Uploads the given info to S3.
     *
     * @param identifier Image identifier.
     * @param info       Info to upload to S3.
     */
    @Override
    public void put(Identifier identifier, String info) throws IOException {
        LOGGER.debug("put(): caching info for {}", identifier);
        final Stopwatch watch = new Stopwatch();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(getBucketName())
                .key(getObjectKey(identifier))
                .contentType(MediaType.APPLICATION_JSON.toString())
                .contentEncoding("UTF-8")
                .build();
        byte[] data = info.getBytes(StandardCharsets.UTF_8);
        LOGGER.trace("put(): uploading {} bytes to {} in bucket {}",
                data.length, request.key(), request.bucket());

        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            getClientInstance().putObject(request,
                    RequestBody.fromInputStream(is, data.length));
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }

        LOGGER.trace("put(): wrote {} bytes to {} in bucket {} in {}",
                data.length, request.key(), request.bucket(),
                watch);
    }

    /**
     * The AWS client logs a warning when we close an InputStream without fully
     * reading it. This method does that and then closes the stream.
     */
    private void consumeStreamAsync(InputStream inputStream) {
        ThreadPool.getInstance().submit(() -> {
            try {
                inputStream.readAllBytes();
            } catch (IOException e) {
                LOGGER.warn("consumeStreamAsync(): failed to consume the stream: {}",
                        e.getMessage());
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.warn("consumeStreamAsync(): failed to close the stream: {}",
                            e.getMessage());
                }
            }
        });
    }

    /**
     * Updates an object's "last-accessed time." Since S3 doesn't support a
     * last-accessed time and S3 objects are immutable, this method copies the
     * object with the given key to a new object with the same key. The new
     * object has a new last-modified time which will serve as a last-accessed
     * time.
     */
    private void touchAsync(String objectKey) {
        final S3Client client   = getClientInstance();
        final String bucketName = getBucketName();
        ThreadPool.getInstance().submit(() -> {
            LOGGER.debug("touchAsync(): {}", objectKey);
            client.copyObject(CopyObjectRequest.builder()
                    .copySource(bucketName + "/" + Reference.encode(objectKey))
                    .destinationBucket(bucketName)
                    .destinationKey(objectKey)
                    // We aren't ever going to read this back in, but S3
                    // requires some kind of change to the object before it can
                    // be copied over itself. See:
                    // https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html
                    .metadata(Map.of("x-amz-meta-last-accessed",
                            String.valueOf(Instant.now().toEpochMilli())))
                    .metadataDirective(MetadataDirective.REPLACE)
                    .build());
        }, ThreadPool.Priority.LOW);
    }

}
