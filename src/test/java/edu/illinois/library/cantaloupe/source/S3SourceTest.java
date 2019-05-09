package edu.illinois.library.cantaloupe.source;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.S3Server;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
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

    private static final S3Server S3_SERVER = new S3Server();

    private S3Source instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        startS3ServerIfNecessary();
        createBucket();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        deleteBucket();
        S3_SERVER.stop();
    }

    private static void createBucket() {
        final AmazonS3 s3 = client();
        final String bucketName = getBucket();
        try {
            deleteBucket();
        } catch (AmazonS3Exception e) {
            // This probably means it doesn't exist. We'll find out shortly.
        }
        try {
            s3.createBucket(new CreateBucketRequest(bucketName));
        } catch (AmazonS3Exception e) {
            if (!e.getMessage().contains("you already own it")) {
                throw e;
            }
        }
    }

    private static void deleteBucket() throws AmazonS3Exception {
        final AmazonS3 s3 = client();
        final String bucketName = getBucket();
        s3.deleteBucket(bucketName);
    }

    private static void emptyBucket() {
        final AmazonS3 s3 = client();
        final String bucketName = getBucket();

        ObjectListing objectListing = s3.listObjects(bucketName);
        while (true) {
            for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                s3.deleteObject(bucketName, s3ObjectSummary.getKey());
            }
            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
    }

    private static void seedFixtures() throws IOException {
        final AmazonS3 s3 = client();
        Path fixture = TestUtil.getImage("jpg");

        for (final String key : new String[] {
                OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_CONTENT_TYPE_BUT_NO_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION}) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                Files.copy(fixture, os);
                byte[] imageBytes = os.toByteArray();

                final ObjectMetadata metadata = new ObjectMetadata();
                if (!OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION.equals(key) &&
                        !OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION.equals(key) &&
                        !OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION.equals(key) &&
                        !OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION.equals(key)) {
                    metadata.setContentType("image/jpeg");
                }
                metadata.setContentLength(imageBytes.length);

                try (ByteArrayInputStream s3Stream = new ByteArrayInputStream(imageBytes)) {
                    final PutObjectRequest request = new PutObjectRequest(
                            getBucket(), key, s3Stream, metadata);
                    s3.putObject(request);
                }
            }
        }

        // Add a non-image
        fixture = TestUtil.getImage("text.txt");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Files.copy(fixture, os);
            byte[] imageBytes = os.toByteArray();

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageBytes.length);

            try (ByteArrayInputStream s3Stream = new ByteArrayInputStream(imageBytes)) {
                final PutObjectRequest request = new PutObjectRequest(
                        getBucket(), NON_IMAGE_KEY, s3Stream, metadata);
                s3.putObject(request);
            }
        }

    }

    /**
     * Starts a mock S3 service if {@link #getAccessKeyId()} and
     * {@link #getSecretKey()} return an empty value.
     */
    private static void startS3ServerIfNecessary() throws IOException {
        if ("localhost".equals(getEndpoint().getHost())) {
            S3_SERVER.start();
        }
    }

    private static AmazonS3 client() {
        return new AWSClientBuilder()
                .endpointURI(getEndpoint())
                .accessKeyID(getAccessKeyId())
                .secretKey(getSecretKey())
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

    private static URI getEndpoint() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        String endpointStr = testConfig.getString(ConfigurationConstants.S3_ENDPOINT.getKey());
        if (endpointStr != null && !endpointStr.isEmpty()) {
            try {
                return new URI(endpointStr);
            } catch (URISyntaxException e) {
                return null;
            }
        }
        return S3_SERVER.getEndpoint();
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
        emptyBucket();
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
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb"));

            Identifier identifier = new Identifier(OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
            instance.setIdentifier(identifier);
            RequestContext context = new RequestContext();
            context.setIdentifier(identifier);
            DelegateProxyService service = DelegateProxyService.getInstance();
            DelegateProxy proxy = service.newDelegateProxy(context);
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
    void testCheckAccessUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bogus");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
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

        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);

        instance.checkAccess();
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithMissingKeyInReturnedHash()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("key:" + OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
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
        assertEquals(Format.PNG, it.next());     // object key
        assertEquals(Format.PNG, it.next());     // identifier extension
        assertEquals(Format.UNKNOWN, it.next()); // Content-Type is null
        assertEquals(Format.JPG, it.next());     // magic bytes
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
