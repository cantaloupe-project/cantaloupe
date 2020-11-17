package edu.illinois.library.cantaloupe.test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3Utils {

    public static void createBucket(AmazonS3 client, String name) {
        try {
            deleteBucket(client, name);
        } catch (AmazonS3Exception e) {
            // This probably means it doesn't exist. We'll find out shortly.
        }
        try {
            client.createBucket(new CreateBucketRequest(name));
        } catch (AmazonS3Exception e) {
            if (!e.getMessage().contains("you already own it")) {
                throw e;
            }
        }
    }

    public static void deleteBucket(AmazonS3 client,
                                    String name) throws AmazonS3Exception {
        emptyBucket(client, name);
        client.deleteBucket(name);
    }

    public static void emptyBucket(AmazonS3 client, String name) {
        ObjectListing objectListing = client.listObjects(name);
        while (true) {
            for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                client.deleteObject(name, s3ObjectSummary.getKey());
            }
            if (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
    }

}
