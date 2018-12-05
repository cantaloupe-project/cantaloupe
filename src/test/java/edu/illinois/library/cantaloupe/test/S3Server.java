package edu.illinois.library.cantaloupe.test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import io.findify.s3mock.S3Mock;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * In-memory S3 server using S3Mock, serving the {@link
 * TestUtil#getImage(String) image fixtures}.
 */
public class S3Server {

    public static final String ACCESS_KEY_ID        = "s3mock";
    public static final String SECRET_KEY           = "s3mock";
    public static final String FIXTURES_BUCKET_NAME = "fixtures";

    private static S3Mock mockS3;
    private static int mockS3Port;

    public URI getEndpoint() {
        try {
            return new URI("http://localhost:" + mockS3Port);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void start() throws IOException {
        mockS3Port = SocketUtils.getOpenPort();
        mockS3 = new S3Mock.Builder()
                .withPort(mockS3Port)
                .withInMemoryBackend()
                .build();
        mockS3.start();

        createFixturesBucket();
        seedFixtures();
    }

    public void stop() {
        if (mockS3 != null) {
            mockS3.stop();
            mockS3 = null;
        }
    }

    private AmazonS3 client() {
        return new AWSClientBuilder()
                .endpointURI(getEndpoint())
                .accessKeyID(ACCESS_KEY_ID)
                .secretKey(SECRET_KEY)
                .build();
    }

    private void createFixturesBucket() {
        final AmazonS3 s3 = client();
        try {
            s3.deleteBucket(FIXTURES_BUCKET_NAME);
        } catch (AmazonS3Exception e) {
            // This probably means it doesn't exist. We'll find out shortly.
        }
        try {
            s3.createBucket(new CreateBucketRequest(FIXTURES_BUCKET_NAME));
        } catch (AmazonS3Exception e) {
            if (!e.getMessage().contains("you already own it")) {
                throw e;
            }
        }
    }

    private void seedFixtures() throws IOException {
        TransferManager xferMgr = TransferManagerBuilder.standard()
                .withS3Client(client()).build();
        try {
            MultipleFileUpload upload = xferMgr.uploadDirectory(
                    FIXTURES_BUCKET_NAME, null,
                    TestUtil.getImagesPath().toFile(), true);
            upload.waitForCompletion();
        } catch (AmazonServiceException | InterruptedException e) {
            throw new IOException(e);
        }
    }

}
