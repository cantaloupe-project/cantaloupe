package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
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
        class ConfigFileCredentials implements AWSCredentials {
            @Override
            public String getAWSAccessKeyId() {
                return getAccessKeyId();
            }

            @Override
            public String getAWSSecretKey() {
                return getSecretKey();
            }
        }
        AWSCredentials credentials = new ConfigFileCredentials();
        return new AmazonS3Client(credentials);
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

    private static String getSecretKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(AmazonS3Resolver.BUCKET_NAME_CONFIG_KEY, getBucket());
        config.setProperty(AmazonS3Resolver.ACCESS_KEY_ID_CONFIG_KEY, getAccessKeyId());
        config.setProperty(AmazonS3Resolver.SECRET_KEY_CONFIG_KEY, getSecretKey());
        config.setProperty(AmazonS3Resolver.LOOKUP_STRATEGY_CONFIG_KEY,
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
        config.setProperty(AmazonS3Resolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
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
        config.setProperty(AmazonS3Resolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
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
