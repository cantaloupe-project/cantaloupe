package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.AWSClientFactory;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests AmazonS3Resolver against Amazon S3. An AWS account is required.
 */
public class AmazonS3ResolverTest extends BaseTest {

    private static final String OBJECT_KEY = "jpeg.jpg";

    private AmazonS3Resolver instance;

    @BeforeClass
    public static void uploadFixtures() throws IOException {
        final AmazonS3 s3 = client();
        try (FileInputStream fis = new FileInputStream(TestUtil.getImage("jpg-rgb-64x56x8-line.jpg"));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtils.copy(fis, baos);
            byte[] imageBytes = baos.toByteArray();

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
    public static void removeFixtures() {
        final AmazonS3 s3 = client();
        s3.deleteObject(getBucket(), OBJECT_KEY);
    }

    private static AmazonS3 client() {
        return new AWSClientFactory(getAccessKeyId(), getSecretKey(), getRegion()).newClient();
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

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.AMAZONS3RESOLVER_BUCKET_NAME, getBucket());
        config.setProperty(Key.AMAZONS3RESOLVER_ACCESS_KEY_ID, getAccessKeyId());
        config.setProperty(Key.AMAZONS3RESOLVER_SECRET_KEY, getSecretKey());
        config.setProperty(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");

        instance = new AmazonS3Resolver();
        instance.setIdentifier(new Identifier(OBJECT_KEY));
    }

    @Test
    public void testNewStreamSourceWithBasicLookupStrategy() {
        // present, readable image
        try {
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testNewStreamSourceWithScriptLookupStrategy() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        // present image
        try {
            StreamSource source = instance.newStreamSource();
            assertNotNull(source.newInputStream());
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testGetSourceFormatWithBasicLookupStrategy() throws IOException {
        assertEquals(Format.JPG, instance.getSourceFormat());
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
        try {
            instance.setIdentifier(new Identifier("image"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    public void testGetSourceFormatWithScriptLookupStrategy() throws IOException {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        // present image
        assertEquals(Format.JPG, instance.getSourceFormat());
        // present image without extension TODO: write this

        // missing image with extension
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
        // missing image without extension
        try {
            instance.setIdentifier(new Identifier("image"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

}
