package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.util.S3Utils;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.S3ClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class S3HTTPImageInputStreamClientTest extends BaseTest {

    private static final String FIXTURE_KEY = "jpg";

    private S3HTTPImageInputStreamClient instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        S3Utils.createBucket(client(), bucket());
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        S3Utils.deleteBucket(client(), bucket());
    }

    private static String accessKeyID() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_ACCESS_KEY_ID.getKey());
    }

    private static String bucket() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_BUCKET.getKey());
    }

    private static String endpoint() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_ENDPOINT.getKey());
    }

    private static String secretAccessKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    private static void configureS3Source() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_ENDPOINT, endpoint());
        config.setProperty(Key.S3SOURCE_ACCESS_KEY_ID, accessKeyID());
        config.setProperty(Key.S3SOURCE_SECRET_KEY, secretAccessKey());
    }

    private static S3Client client() {
        return new S3ClientBuilder()
                .endpointURI(URI.create(endpoint()))
                .accessKeyID(accessKeyID())
                .secretKey(secretAccessKey())
                .build();
    }

    private static void seedFixtures() {
        final S3Client client  = client();
        final Path fixture = TestUtil.getImage(FIXTURE_KEY);
        client.putObject(PutObjectRequest.builder()
                .bucket(bucket())
                .key(fixture.getFileName().toString())
                .build(), fixture);
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        configureS3Source();
        seedFixtures();

        S3ObjectInfo info = new S3ObjectInfo(FIXTURE_KEY, bucket());
        info.setLength(1584);

        instance = new S3HTTPImageInputStreamClient(info);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        S3Utils.emptyBucket(client(), bucket());
    }

    @Test
    void testSendHEADRequest() throws Exception {
        Response actual = instance.sendHEADRequest();
        assertEquals(200, actual.getStatus());
        assertEquals("bytes", actual.getHeaders().getFirstValue("Accept-Ranges"));
        assertEquals("1584", actual.getHeaders().getFirstValue("Content-Length"));
    }

    @Test
    void testSendGETRequest() throws Exception {
        Response actual = instance.sendGETRequest(new Range(10, 50, 1584));
        assertEquals(206, actual.getStatus());
        assertEquals(41, actual.getBody().length);
    }

}
