package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.S3ClientBuilder;
import edu.illinois.library.cantaloupe.util.S3Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class S3SourceTest extends AbstractSourceTest {

    private static final String OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION      = "jpeg.jpg";
    private static final String OBJECT_KEY_WITH_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION    = "jpeg.unknown";
    private static final String OBJECT_KEY_WITH_CONTENT_TYPE_BUT_NO_EXTENSION              = "jpg";
    private static final String OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION   = "jpeg.jpg";
    private static final String OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION = "jpeg.unknown";
    private static final String OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION    = "jpeg.png";
    private static final String OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION               = "jpg";
    private static final String NON_IMAGE_KEY                                              = "NotAnImage";

    private static S3Client client;

    private S3Source instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        S3Utils.createBucket(client(), getBucket());
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        S3Utils.deleteBucket(client(), getBucket());
        if (client != null) {
            client.close();
        }
    }

    private static void seedFixtures() {
        final S3Client client = client();
        Path fixture          = TestUtil.getImage("jpg");

        for (final String key : new String[] {
                OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_CONTENT_TYPE_BUT_NO_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION}) {
            String contentType = null;
            if (!OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION.equals(key)) {
                contentType = "image/jpeg";
            }
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();
            client.putObject(request, fixture);
        }

        // Add a non-image
        fixture = TestUtil.getImage("text.txt");
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(getBucket())
                .key(NON_IMAGE_KEY)
                .build();
        client.putObject(request, fixture);
    }

    private static S3Client client() {
        if (client == null) {
            client = new S3ClientBuilder()
                    .endpointURI(getEndpoint())
                    .accessKeyID(getAccessKeyId())
                    .secretKey(getSecretKey())
                    .build();
        }
        return client;
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
        if (endpointStr != null && !endpointStr.isEmpty()) {
            try {
                return new URI(endpointStr);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return null;
    }

    private static String getSecretKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.S3_SECRET_KEY.getKey());
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        seedFixtures();
        instance = newInstance();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        S3Utils.emptyBucket(client(), getBucket());
    }

    @Override
    void destroyEndpoint() {
        // will be done in @AfterAll
    }

    @Override
    void initializeEndpoint() {
        // will be done in @BeforeAll
    }

    @Override
    S3Source newInstance() {
        S3Source instance = new S3Source();
        instance.setIdentifier(new Identifier(OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION));
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_BUCKET_NAME, getBucket());
        config.setProperty(Key.S3SOURCE_ENDPOINT, getEndpoint());
        config.setProperty(Key.S3SOURCE_ACCESS_KEY_ID, getAccessKeyId());
        config.setProperty(Key.S3SOURCE_SECRET_KEY, getSecretKey());
        config.setProperty(Key.S3SOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    @Override
    void useScriptLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.S3SOURCE_LOOKUP_STRATEGY,
                    "ScriptLookupStrategy");

            Identifier identifier = new Identifier(OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
            instance.setIdentifier(identifier);
            DelegateProxy proxy = TestUtil.newDelegateProxy();
            proxy.getRequestContext().setIdentifier(identifier);
            instance.setDelegateProxy(proxy);
        } catch (Exception e) {
            fail();
        }
    }

    /* awsRegionFromURL() */

    @Test
    void testAWSRegionFromURLWithAPSouth1() {
        assertEquals("ap-south-1", S3Source.awsRegionFromURL("s3.ap-south-1.amazonaws.com"));
        assertEquals("ap-south-1", S3Source.awsRegionFromURL("s3-ap-south-1.amazonaws.com"));
        assertEquals("ap-south-1", S3Source.awsRegionFromURL("s3.dualstack.ap-south-1.amazonaws.com"));
        assertEquals("ap-south-1", S3Source.awsRegionFromURL("account-id.s3-control.ap-south-1.amazonaws.com"));
        assertEquals("ap-south-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.ap-south-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithAPNortheast1() {
        assertEquals("ap-northeast-1", S3Source.awsRegionFromURL("s3.ap-northeast-1.amazonaws.com"));
        assertEquals("ap-northeast-1", S3Source.awsRegionFromURL("s3-ap-northeast-1.amazonaws.com"));
        assertEquals("ap-northeast-1", S3Source.awsRegionFromURL("s3.dualstack.ap-northeast-1.amazonaws.com"));
        assertEquals("ap-northeast-1", S3Source.awsRegionFromURL("account-id.s3-control.ap-northeast-1.amazonaws.com"));
        assertEquals("ap-northeast-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.ap-northeast-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithAPNortheast2() {
        assertEquals("ap-northeast-2", S3Source.awsRegionFromURL("s3.ap-northeast-2.amazonaws.com"));
        assertEquals("ap-northeast-2", S3Source.awsRegionFromURL("s3-ap-northeast-2.amazonaws.com"));
        assertEquals("ap-northeast-2", S3Source.awsRegionFromURL("s3.dualstack.ap-northeast-2.amazonaws.com"));
        assertEquals("ap-northeast-2", S3Source.awsRegionFromURL("account-id.s3-control.ap-northeast-2.amazonaws.com"));
        assertEquals("ap-northeast-2", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.ap-northeast-2.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithAPNortheast3() {
        assertEquals("ap-northeast-3", S3Source.awsRegionFromURL("s3.ap-northeast-3.amazonaws.com"));
        assertEquals("ap-northeast-3", S3Source.awsRegionFromURL("s3-ap-northeast-3.amazonaws.com"));
        assertEquals("ap-northeast-3", S3Source.awsRegionFromURL("s3.dualstack.ap-northeast-3.amazonaws.com"));
        assertEquals("ap-northeast-3", S3Source.awsRegionFromURL("account-id.s3-control.ap-northeast-3.amazonaws.com"));
        assertEquals("ap-northeast-3", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.ap-northeast-3.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithAPSoutheast1() {
        assertEquals("ap-southeast-1", S3Source.awsRegionFromURL("s3.ap-southeast-1.amazonaws.com"));
        assertEquals("ap-southeast-1", S3Source.awsRegionFromURL("s3-ap-southeast-1.amazonaws.com"));
        assertEquals("ap-southeast-1", S3Source.awsRegionFromURL("s3.dualstack.ap-southeast-1.amazonaws.com"));
        assertEquals("ap-southeast-1", S3Source.awsRegionFromURL("account-id.s3-control.ap-southeast-1.amazonaws.com"));
        assertEquals("ap-southeast-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.ap-southeast-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithAPSoutheast2() {
        assertEquals("ap-southeast-2", S3Source.awsRegionFromURL("s3.ap-southeast-2.amazonaws.com"));
        assertEquals("ap-southeast-2", S3Source.awsRegionFromURL("s3-ap-southeast-2.amazonaws.com"));
        assertEquals("ap-southeast-2", S3Source.awsRegionFromURL("s3.dualstack.ap-southeast-2.amazonaws.com"));
        assertEquals("ap-southeast-2", S3Source.awsRegionFromURL("account-id.s3-control.ap-southeast-2.amazonaws.com"));
        assertEquals("ap-southeast-2", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.ap-southeast-2.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithCACentral1() {
        assertEquals("ca-central-1", S3Source.awsRegionFromURL("s3.ca-central-1.amazonaws.com"));
        assertEquals("ca-central-1", S3Source.awsRegionFromURL("s3-ca-central-1.amazonaws.com"));
        assertEquals("ca-central-1", S3Source.awsRegionFromURL("s3.dualstack.ca-central-1.amazonaws.com"));
        assertEquals("ca-central-1", S3Source.awsRegionFromURL("account-id.s3-control.ca-central-1.amazonaws.com"));
        assertEquals("ca-central-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.ca-central-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithCNNorth1() {
        assertEquals("cn-north-1", S3Source.awsRegionFromURL("s3.cn-north-1.amazonaws.com"));
        assertEquals("cn-north-1", S3Source.awsRegionFromURL("s3-cn-north-1.amazonaws.com"));
        assertEquals("cn-north-1", S3Source.awsRegionFromURL("s3.dualstack.cn-north-1.amazonaws.com"));
        assertEquals("cn-north-1", S3Source.awsRegionFromURL("account-id.s3-control.cn-north-1.amazonaws.com"));
        assertEquals("cn-north-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.cn-north-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithCNNorthwest1() {
        assertEquals("cn-northwest-1", S3Source.awsRegionFromURL("s3.cn-northwest-1.amazonaws.com"));
        assertEquals("cn-northwest-1", S3Source.awsRegionFromURL("s3-cn-northwest-1.amazonaws.com"));
        assertEquals("cn-northwest-1", S3Source.awsRegionFromURL("s3.dualstack.cn-northwest-1.amazonaws.com"));
        assertEquals("cn-northwest-1", S3Source.awsRegionFromURL("account-id.s3-control.cn-northwest-1.amazonaws.com"));
        assertEquals("cn-northwest-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.cn-northwest-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithEUCentral1() {
        assertEquals("eu-central-1", S3Source.awsRegionFromURL("s3.eu-central-1.amazonaws.com"));
        assertEquals("eu-central-1", S3Source.awsRegionFromURL("s3-eu-central-1.amazonaws.com"));
        assertEquals("eu-central-1", S3Source.awsRegionFromURL("s3.dualstack.eu-central-1.amazonaws.com"));
        assertEquals("eu-central-1", S3Source.awsRegionFromURL("account-id.s3-control.eu-central-1.amazonaws.com"));
        assertEquals("eu-central-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.eu-central-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithEUWest1() {
        assertEquals("eu-west-1", S3Source.awsRegionFromURL("s3.eu-west-1.amazonaws.com"));
        assertEquals("eu-west-1", S3Source.awsRegionFromURL("s3-eu-west-1.amazonaws.com"));
        assertEquals("eu-west-1", S3Source.awsRegionFromURL("s3.dualstack.eu-west-1.amazonaws.com"));
        assertEquals("eu-west-1", S3Source.awsRegionFromURL("account-id.s3-control.eu-west-1.amazonaws.com"));
        assertEquals("eu-west-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.eu-west-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithEUWest2() {
        assertEquals("eu-west-2", S3Source.awsRegionFromURL("s3.eu-west-2.amazonaws.com"));
        assertEquals("eu-west-2", S3Source.awsRegionFromURL("s3-eu-west-2.amazonaws.com"));
        assertEquals("eu-west-2", S3Source.awsRegionFromURL("s3.dualstack.eu-west-2.amazonaws.com"));
        assertEquals("eu-west-2", S3Source.awsRegionFromURL("account-id.s3-control.eu-west-2.amazonaws.com"));
        assertEquals("eu-west-2", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.eu-west-2.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithEUWest3() {
        assertEquals("eu-west-3", S3Source.awsRegionFromURL("s3.eu-west-3.amazonaws.com"));
        assertEquals("eu-west-3", S3Source.awsRegionFromURL("s3-eu-west-3.amazonaws.com"));
        assertEquals("eu-west-3", S3Source.awsRegionFromURL("s3.dualstack.eu-west-3.amazonaws.com"));
        assertEquals("eu-west-3", S3Source.awsRegionFromURL("account-id.s3-control.eu-west-3.amazonaws.com"));
        assertEquals("eu-west-3", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.eu-west-3.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithEUNorth1() {
        assertEquals("eu-north-1", S3Source.awsRegionFromURL("s3.eu-north-1.amazonaws.com"));
        assertEquals("eu-north-1", S3Source.awsRegionFromURL("s3-eu-north-1.amazonaws.com"));
        assertEquals("eu-north-1", S3Source.awsRegionFromURL("s3.dualstack.eu-north-1.amazonaws.com"));
        assertEquals("eu-north-1", S3Source.awsRegionFromURL("account-id.s3-control.eu-north-1.amazonaws.com"));
        assertEquals("eu-north-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.eu-north-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithSAEast1() {
        assertEquals("sa-east-1", S3Source.awsRegionFromURL("s3.sa-east-1.amazonaws.com"));
        assertEquals("sa-east-1", S3Source.awsRegionFromURL("s3-sa-east-1.amazonaws.com"));
        assertEquals("sa-east-1", S3Source.awsRegionFromURL("s3.dualstack.sa-east-1.amazonaws.com"));
        assertEquals("sa-east-1", S3Source.awsRegionFromURL("account-id.s3-control.sa-east-1.amazonaws.com"));
        assertEquals("sa-east-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.sa-east-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithUSEast1() {
        assertEquals("us-east-1", S3Source.awsRegionFromURL("s3.amazonaws.com"));
        assertEquals("us-east-1", S3Source.awsRegionFromURL("s3.us-east-1.amazonaws.com"));
        assertEquals("us-east-1", S3Source.awsRegionFromURL("s3-external-1.amazonaws.com"));
        assertEquals("us-east-1", S3Source.awsRegionFromURL("s3.dualstack.us-east-1.amazonaws.com"));
        assertEquals("us-east-1", S3Source.awsRegionFromURL("account-id.s3-control.us-east-1.amazonaws.com"));
        assertEquals("us-east-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.us-east-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithUSEast2() {
        assertEquals("us-east-2", S3Source.awsRegionFromURL("s3.us-east-2.amazonaws.com"));
        assertEquals("us-east-2", S3Source.awsRegionFromURL("s3-us-east-2.amazonaws.com"));
        assertEquals("us-east-2", S3Source.awsRegionFromURL("s3.dualstack.us-east-2.amazonaws.com"));
        assertEquals("us-east-2", S3Source.awsRegionFromURL("account-id.s3-control.us-east-2.amazonaws.com"));
        assertEquals("us-east-2", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.us-east-2.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithUSWest1() {
        assertEquals("us-west-1", S3Source.awsRegionFromURL("s3.us-west-1.amazonaws.com"));
        assertEquals("us-west-1", S3Source.awsRegionFromURL("s3-us-west-1.amazonaws.com"));
        assertEquals("us-west-1", S3Source.awsRegionFromURL("s3.dualstack.us-west-1.amazonaws.com"));
        assertEquals("us-west-1", S3Source.awsRegionFromURL("account-id.s3-control.us-west-1.amazonaws.com"));
        assertEquals("us-west-1", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.us-west-1.amazonaws.com"));
    }

    @Test
    void testAWSRegionFromURLWithUSWest2() {
        assertEquals("us-west-2", S3Source.awsRegionFromURL("s3.us-west-2.amazonaws.com"));
        assertEquals("us-west-2", S3Source.awsRegionFromURL("s3-us-west-2.amazonaws.com"));
        assertEquals("us-west-2", S3Source.awsRegionFromURL("s3.dualstack.us-west-2.amazonaws.com"));
        assertEquals("us-west-2", S3Source.awsRegionFromURL("account-id.s3-control.us-west-2.amazonaws.com"));
        assertEquals("us-west-2", S3Source.awsRegionFromURL("account-id.s3-control.dualstack.us-west-2.amazonaws.com"));
    }

    /* checkAccess() */

    @Test
    void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        instance.checkAccess();
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithMissingImage() {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bogus");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertThrows(NoSuchFileException.class, () -> instance.checkAccess());
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyReturningHash()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bucket:" + getBucket() +
                ";key:" + OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
        instance.setIdentifier(identifier);

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);

        instance.checkAccess();
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithMissingKeyInReturnedHash() {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("key:" + OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertThrows(IllegalArgumentException.class, () -> instance.checkAccess());
    }

    /* getFormatIterator() */

    @Test
    void testGetFormatIteratorHasNext() {
        S3Source source = newInstance();
        source.setIdentifier(new Identifier(OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION));
        S3Source.FormatIterator<Format> it = source.getFormatIterator();

        assertTrue(it.hasNext());
        it.next(); // object key
        assertTrue(it.hasNext());
        it.next(); // identifier extension
        assertTrue(it.hasNext());
        it.next(); // Content-Type is null
        assertTrue(it.hasNext());
        it.next(); // magic bytes
        assertFalse(it.hasNext());
    }

    @Test
    void testGetFormatIteratorNext() {
        S3Source source = newInstance();
        source.setIdentifier(new Identifier(OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION));

        S3Source.FormatIterator<Format> it = source.getFormatIterator();
        assertEquals(Format.get("png"), it.next());     // object key
        assertEquals(Format.get("png"), it.next());     // identifier extension
        assertEquals(Format.UNKNOWN, it.next()); // Content-Type is null
        assertEquals(Format.get("jpg"), it.next());     // magic bytes
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* getObjectInfo() */

    @Test
    void testGetObjectInfo() throws Exception {
        assertNotNull(instance.getObjectInfo());
    }

    @Test
    void testGetObjectInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.S3SOURCE_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id/suffix", instance.getObjectInfo().getKey());
    }

    @Test
    void testGetObjectInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_PATH_PREFIX, "");
        config.setProperty(Key.S3SOURCE_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("id", instance.getObjectInfo().getKey());
    }

    /* newStreamFactory() */

    @Test
    void testNewStreamFactoryUsingBasicLookupStrategy() throws Exception {
        assertNotNull(instance.newStreamFactory());
    }

    @Test
    void testNewStreamFactoryUsingScriptLookupStrategy()
            throws Exception {
        useScriptLookupStrategy();
        assertNotNull(instance.newStreamFactory());
    }

}
