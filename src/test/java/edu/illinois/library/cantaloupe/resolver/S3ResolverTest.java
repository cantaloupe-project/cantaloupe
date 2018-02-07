package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import io.findify.s3mock.S3Mock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Tests S3Resolver against Amazon S3. An AWS account is required.
 */
public class S3ResolverTest extends AbstractResolverTest {

    private static final String OBJECT_KEY = "jpeg.jpg";

    private static S3Mock mockS3;
    private static int mockS3Port;

    private S3Resolver instance;

    @BeforeClass
    public static void beforeClass() throws IOException {
        startServiceIfNecessary();
        createBucket();
        seedFixtures();
    }

    @AfterClass
    public static void afterClass() {
        deleteFixtures();
        if (mockS3 != null) {
            mockS3.stop();
        }
    }

    private static void createBucket() {
        final AmazonS3 s3 = client();
        final String bucketName = getBucket();

        try {
            s3.deleteBucket(bucketName);
        } catch (AmazonS3Exception e) {
            // This probably means it already exists. We'll find out shortly.
        }
        s3.createBucket(new CreateBucketRequest(bucketName));
    }

    private static void seedFixtures() throws IOException {
        final AmazonS3 s3 = client();
        Path fixture = TestUtil.getImage("jpg-rgb-64x56x8-line.jpg");

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Files.copy(fixture, os);
            byte[] imageBytes = os.toByteArray();

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg");
            metadata.setContentLength(imageBytes.length);

            try (ByteArrayInputStream s3Stream = new ByteArrayInputStream(imageBytes)) {
                final PutObjectRequest request = new PutObjectRequest(
                        getBucket(), OBJECT_KEY, s3Stream, metadata);
                s3.putObject(request);
            }
        }
    }

    /**
     * Starts a mock S3 service if {@link #getAccessKeyId()} and
     * {@link #getSecretKey()} return an empty value.
     */
    private static void startServiceIfNecessary() {
        if ("localhost".equals(getEndpoint().getHost())) {
            mockS3Port = SocketUtils.getOpenPort();
            mockS3 = new S3Mock.Builder()
                    .withPort(mockS3Port)
                    .withInMemoryBackend()
                    .build();
            mockS3.start();
        }
    }

    private static AmazonS3 client() {
        return new AWSClientBuilder()
                .endpointURI(getEndpoint())
                .accessKeyID(getAccessKeyId())
                .secretKey(getSecretKey())
                .build();
    }

    private static void deleteFixtures() {
        final AmazonS3 s3 = client();
        s3.deleteObject(getBucket(), OBJECT_KEY);
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
        if (endpointStr == null || endpointStr.isEmpty()) {
            endpointStr = "http://localhost:" + mockS3Port;
        }
        try {
            return new URI(endpointStr);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String getSecretKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    void destroyEndpoint() {
        // will be done in @AfterClass
    }

    @Override
    void initializeEndpoint() {
        // will be done in @BeforeClass
    }

    @Override
    S3Resolver newInstance() {
        S3Resolver instance = new S3Resolver();
        instance.setIdentifier(new Identifier(OBJECT_KEY));
        instance.setContext(new RequestContext());
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3RESOLVER_BUCKET_NAME, getBucket());
        config.setProperty(Key.S3RESOLVER_ENDPOINT, getEndpoint());
        config.setProperty(Key.S3RESOLVER_ACCESS_KEY_ID, getAccessKeyId());
        config.setProperty(Key.S3RESOLVER_SECRET_KEY, getSecretKey());
        config.setProperty(Key.S3RESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    @Override
    void useScriptLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.S3RESOLVER_LOOKUP_STRATEGY,
                    "ScriptLookupStrategy");
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb"));
        } catch (IOException e) {
            fail();
        }
    }

    /* checkAccess() */

    @Test
    public void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        instance.checkAccess();
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccessUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();

        instance.setIdentifier(new Identifier("bogus"));
        instance.checkAccess();
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyReturningHash()
            throws Exception {
        useScriptLookupStrategy();

        instance.setIdentifier(new Identifier("bucket:" + getBucket() +
                ";key:" + OBJECT_KEY));
        instance.checkAccess();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckAccessUsingScriptLookupStrategyWithMissingKeyInReturnedHash()
            throws Exception {
        useScriptLookupStrategy();

        instance.setIdentifier(new Identifier("key:" + OBJECT_KEY));
        instance.checkAccess();
    }

    /* getObjectInfo() */

    @Test
    public void testGetObjectInfo() throws Exception {
        assertNotNull(instance.getObjectInfo());
    }

    @Test
    public void testGetObjectInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3RESOLVER_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.S3RESOLVER_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id/suffix", instance.getObjectInfo().getKey());
    }

    @Test
    public void testGetObjectInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3RESOLVER_PATH_PREFIX, "");
        config.setProperty(Key.S3RESOLVER_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("id", instance.getObjectInfo().getKey());
    }

    /* getSourceFormat() */

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategy()
            throws IOException {
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategy()
            throws IOException {
        useScriptLookupStrategy();
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithImageWithRecognizedExtension()
            throws IOException {
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithImageWithUnrecognizedExtension() {
        // TODO: write this
    }

    @Test
    public void testGetSourceFormatWithImageWithNoExtension() {
        // TODO: write this
    }

    /* newStreamSource() */

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategy() throws Exception {
        assertNotNull(instance.newStreamSource());
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategy()
            throws Exception {
        useScriptLookupStrategy();
        assertNotNull(instance.newStreamSource());
    }

}
