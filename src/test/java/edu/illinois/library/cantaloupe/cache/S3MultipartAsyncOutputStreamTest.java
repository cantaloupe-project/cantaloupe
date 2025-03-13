package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.S3ClientBuilder;
import edu.illinois.library.cantaloupe.util.S3Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class S3MultipartAsyncOutputStreamTest extends BaseTest {

    private static S3Client client;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        S3Utils.createBucket(client(), getBucket());
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        if (client != null) {
            client.close();
        }
    }

    private static synchronized S3Client client() {
        if (client == null) {
            client = new S3ClientBuilder()
                    .endpointURI(getEndpoint())
                    .region(getRegion())
                    .accessKeyID(getAccessKeyId())
                    .secretAccessKey(getSecretKey())
                    .build();
        }
        return client;
    }

    private static void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(getBucket())
                .key(key)
                .build();
        client.deleteObject(request);
    }

    private static String getAccessKeyId() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_ACCESS_KEY_ID.getKey());
    }

    private static String getBucket() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_BUCKET.getKey());
    }

    private static URI getEndpoint() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        String endpointStr = testConfig.getString(ConfigurationConstants.S3_ENDPOINT.getKey());
        if (endpointStr != null && !endpointStr.isBlank()) {
            try {
                return new URI(endpointStr);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return null;
    }

    private static String getRegion() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_REGION.getKey());
    }

    private static String getSecretKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    private static byte[] readBytes(String key) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(getBucket())
                .key(key)
                .build();
        ResponseInputStream<GetObjectResponse> is = client().getObject(request);
        return is.readAllBytes();
    }

    @Test
    void closeMarksInstanceComplete() throws Exception {
        final String key = S3MultipartAsyncOutputStreamTest.class.getSimpleName() +
                "/closeMarksInstanceComplete";
        S3MultipartAsyncOutputStream instance = new S3MultipartAsyncOutputStream(
                client(), getBucket(), key, "image/jpeg");
        instance.observer = this;

        try {
            byte[] bytes = new byte[1024 * 1024];
            new SecureRandom().nextBytes(bytes);
            instance.write(bytes);
            instance.setComplete(true);
            instance.close();

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (instance) {
                instance.wait();
            }
            assertTrue(instance.isComplete());
        } finally {
            delete(key);
        }
    }

    @Test
    void write1WithMultipleParts() throws Exception {
        final String key = S3MultipartAsyncOutputStreamTest.class.getSimpleName() +
                "/write1WithMultipleParts";
        S3MultipartAsyncOutputStream instance = new S3MultipartAsyncOutputStream(
                client(), getBucket(), key, "image/jpeg");
        instance.observer = this;

        byte[] expectedBytes = new byte[
                S3MultipartAsyncOutputStream.MINIMUM_PART_LENGTH * 2 + 1024 * 1024];
        new SecureRandom().nextBytes(expectedBytes);

        try {
            for (byte b : expectedBytes) {
                instance.write(b);
            }
            instance.setComplete(true);
            instance.close();

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (instance) {
                instance.wait();
            }

            byte[] actualBytes = readBytes(key);
            assertArrayEquals(expectedBytes, actualBytes);
        } finally {
            delete(key);
        }
    }

    @Test
    void write1WithSinglePart() throws Exception {
        final String key = S3MultipartAsyncOutputStreamTest.class.getSimpleName() +
                "/write1WithSinglePart";
        S3MultipartAsyncOutputStream instance = new S3MultipartAsyncOutputStream(
                client(), getBucket(), key, "image/jpeg");
        instance.observer = this;

        try {
            byte[] expectedBytes = new byte[1024 * 1024]; // smaller than part size
            new SecureRandom().nextBytes(expectedBytes);
            for (byte b : expectedBytes) {
                instance.write(b);
            }
            instance.setComplete(true);
            instance.close();

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (instance) {
                instance.wait();
            }

            byte[] actualBytes = readBytes(key);
            assertArrayEquals(expectedBytes, actualBytes);
        } finally {
            delete(key);
        }
    }

    /**
     * Tests that an object larger than the part size is written correctly.
     */
    @Test
    void write2WithMultipleParts() throws Exception {
        final String key = S3MultipartAsyncOutputStreamTest.class.getSimpleName() +
                "/write2WithMultipleParts";
        S3MultipartAsyncOutputStream instance = new S3MultipartAsyncOutputStream(
                client(), getBucket(), key, "image/jpeg");
        instance.observer = this;

        byte[] expectedBytes = new byte[
                S3MultipartAsyncOutputStream.MINIMUM_PART_LENGTH * 2 + 1024 * 1024];
        new SecureRandom().nextBytes(expectedBytes);

        try {
            instance.write(expectedBytes);
            instance.setComplete(true);
            instance.close();

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (instance) {
                instance.wait();
            }

            byte[] actualBytes = readBytes(key);
            assertArrayEquals(expectedBytes, actualBytes);
        } finally {
            delete(key);
        }
    }

    /**
     * Tests that an object smaller than the part size is written correctly.
     */
    @Test
    void write2WithSinglePart() throws Exception {
        final String key = S3MultipartAsyncOutputStreamTest.class.getSimpleName() +
                "/write2WithSinglePart";
        S3MultipartAsyncOutputStream instance = new S3MultipartAsyncOutputStream(
                client(), getBucket(), key, "image/jpeg");
        instance.observer = this;

        try {
            byte[] expectedBytes = new byte[1024 * 1024]; // smaller than part size
            new SecureRandom().nextBytes(expectedBytes);
            instance.write(expectedBytes);
            instance.setComplete(true);
            instance.close();

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (instance) {
                instance.wait();
            }

            byte[] actualBytes = readBytes(key);
            assertArrayEquals(expectedBytes, actualBytes);
        } finally {
            delete(key);
        }
    }

    /**
     * Tests that an object larger than the part size is written correctly.
     */
    @Test
    void write3WithMultipleParts() throws Exception {
        final String key = S3MultipartAsyncOutputStreamTest.class.getSimpleName() +
                "/write3WithMultipleParts";
        S3MultipartAsyncOutputStream instance = new S3MultipartAsyncOutputStream(
                client(), getBucket(), key, "image/jpeg");
        instance.observer = this;

        byte[] expectedBytes = new byte[
                S3MultipartAsyncOutputStream.MINIMUM_PART_LENGTH * 2 + 1024 * 1024];
        new SecureRandom().nextBytes(expectedBytes);

        try {
            instance.write(expectedBytes, 0, expectedBytes.length);
            instance.setComplete(true);
            instance.close();

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (instance) {
                instance.wait();
            }

            byte[] actualBytes = readBytes(key);
            assertArrayEquals(expectedBytes, actualBytes);
        } finally {
            delete(key);
        }
    }

    /**
     * Tests that an object smaller than the part size is written correctly.
     */
    @Test
    void write3WithSinglePart() throws Exception {
        final String key = S3MultipartAsyncOutputStreamTest.class.getSimpleName() +
                "/write3WithSinglePart";
        S3MultipartAsyncOutputStream instance = new S3MultipartAsyncOutputStream(
                client(), getBucket(), key, "image/jpeg");
        instance.observer = this;

        try {
            byte[] expectedBytes = new byte[1024 * 1024]; // smaller than part size
            new SecureRandom().nextBytes(expectedBytes);
            instance.write(expectedBytes, 0, expectedBytes.length);
            instance.setComplete(true);
            instance.close();

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (instance) {
                instance.wait();
            }

            byte[] actualBytes = readBytes(key);
            assertArrayEquals(expectedBytes, actualBytes);
        } finally {
            delete(key);
        }
    }

}
