package edu.illinois.library.cantaloupe.util;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides some convenience methods for working with S3 buckets and objects
 * using {@link S3Client}.
 */
public final class S3Utils {

    public interface ResponseObjectHandler<T> {
        void handle(T object);
    }

    /**
     * Creates the given bucket. If it already exists, it is emptied out.
     *
     * @param client     S3 client.
     * @param bucketName Bucket name.
     */
    public static void createBucket(S3Client client, String bucketName) {
        try {
            emptyBucket(client, bucketName);
        } catch (NoSuchBucketException ignore) {
            client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName).build());
        }
    }

    /**
     * Empties the given bucket and then deletes it.
     *
     * @param client     S3 client.
     * @param bucketName Bucket to empty out and delete.
     */
    public static void deleteBucket(S3Client client,
                                    String bucketName) throws S3Exception {
        emptyBucket(client, bucketName);
        client.deleteBucket(DeleteBucketRequest.builder()
                .bucket(bucketName).build());
    }

    /**
     * @param client     S3 client.
     * @param bucketName Name of the bucket to empty out.
     * @return           Number of objects deleted.
     */
    public static int emptyBucket(S3Client client, String bucketName) {
        final AtomicInteger counter = new AtomicInteger();
        walkObjects(client, bucketName, null, (object) -> {
            try {
                client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(object.key())
                        .build());
                counter.incrementAndGet();
            } catch (S3Exception ignore) {
                // TODO: don't ignore this
            }
        });
        return counter.get();
    }

    /**
     * Uploads all files in {@code rootPath} into the given bucket.
     *
     * @param client     S3 client.
     * @param rootPath   Root path to upload.
     * @param bucketName Bucket to upload into.
     */
    public void seedBucket(S3Client client,
                           Path rootPath,
                           String bucketName) throws IOException {
        Files.walk(rootPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String key = path.toString().replace(rootPath.toString(), "").substring(1);
                    client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(), path);
                });
    }

    /**
     * Invokes the given handler on all objects in the given bucket.
     *
     * @param client     S3 client.
     * @param bucketName Bucket name.
     * @param handler    Handler to invoke.
     */
    public static void walkObjects(S3Client client,
                                   String bucketName,
                                   ResponseObjectHandler<S3Object> handler) {
        walkObjects(client, bucketName, null, handler);
    }

    /**
     * Invokes the given handler on all objects in the given bucket that have
     * the given key prefix.
     *
     * @param client     S3 client.
     * @param bucketName Bucket name.
     * @param prefix     Key prefix. May be {@code null}.
     * @param handler    Handler to invoke.
     */
    public static void walkObjects(S3Client client,
                                   String bucketName,
                                   String prefix,
                                   ResponseObjectHandler<S3Object> handler) {
        String marker = null;
        ListObjectsResponse response;
        do {
            ListObjectsRequest request = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .marker(marker)
                    .build();
            response = client.listObjects(request);
            response.contents().forEach(handler::handle);
            marker = response.nextMarker();
        } while (response.isTruncated());
    }

    private S3Utils() {}

}
