package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.services.s3.AmazonS3;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Tests AmazonS3Resolver against Amazon S3. An AWS account is required.
 */
public class AmazonS3ResolverTest extends AbstractResolverTest {

    private static final String OBJECT_KEY = "jpeg.jpg";

    private AmazonS3Resolver instance;

    @BeforeClass
    public static void uploadFixtures() throws IOException {
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

    @AfterClass
    public static void deleteFixtures() {
        final AmazonS3 s3 = client();
        s3.deleteObject(getBucket(), OBJECT_KEY);
    }

    private static AmazonS3 client() {
        return new AWSClientBuilder()
                .accessKeyID(getAccessKeyId())
                .secretKey(getSecretKey())
                .region(getRegion())
                .build();
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
    AmazonS3Resolver newInstance() {
        AmazonS3Resolver instance = new AmazonS3Resolver();
        instance.setIdentifier(new Identifier(OBJECT_KEY));
        instance.setContext(new RequestContext());
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.AMAZONS3RESOLVER_BUCKET_NAME, getBucket());
        config.setProperty(Key.AMAZONS3RESOLVER_BUCKET_REGION, getRegion());
        config.setProperty(Key.AMAZONS3RESOLVER_ACCESS_KEY_ID, getAccessKeyId());
        config.setProperty(Key.AMAZONS3RESOLVER_SECRET_KEY, getSecretKey());
        config.setProperty(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    @Override
    void useScriptLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY,
                    "ScriptLookupStrategy");
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb"));
        } catch (IOException e) {
            fail();
        }
    }

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
