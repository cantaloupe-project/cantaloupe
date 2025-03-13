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
                    .region(getRegion())
                    .accessKeyID(getAccessKeyId())
                    .secretAccessKey(getSecretKey())
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

    /* getClientInstance() */

    @Test
    void getClientInstanceReturnsDefaultClient() {
        assertNotNull(S3Source.getClientInstance(new S3ObjectInfo()));
    }

    @Test
    void getClientInstanceReturnsUniqueClientsPerEndpoint() {
        S3ObjectInfo info1 = new S3ObjectInfo();
        S3ObjectInfo info2 = new S3ObjectInfo();
        info2.setEndpoint("http://example.org/endpoint");
        S3Client client1 = S3Source.getClientInstance(info1);
        S3Client client2 = S3Source.getClientInstance(info2);
        assertNotSame(client1, client2);
    }

    @Test
    void getClientInstanceCachesReturnedClients() {
        S3ObjectInfo info1 = new S3ObjectInfo();
        S3ObjectInfo info2 = new S3ObjectInfo();
        info1.setEndpoint("http://example.org/endpoint");
        info2.setEndpoint(info1.getEndpoint());
        S3Client client1 = S3Source.getClientInstance(info1);
        S3Client client2 = S3Source.getClientInstance(info2);
        assertSame(client1, client2);
    }

    /* checkAccess() */

    @Test
    void checkAccessUsingBasicLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test
    void checkAccessUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        instance.stat();
    }

    @Test
    void checkAccessUsingScriptLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test
    void checkAccessUsingScriptLookupStrategyWithMissingImage() {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bogus");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertThrows(NoSuchFileException.class, () -> instance.stat());
    }

    @Test
    void checkAccessUsingScriptLookupStrategyReturningHash() throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bucket:" + getBucket() +
                ";key:" + OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
        instance.setIdentifier(identifier);

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);

        instance.stat();
    }

    @Test
    void checkAccessUsingScriptLookupStrategyWithMissingKeyInReturnedHash() {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("key:" + OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertThrows(IllegalArgumentException.class, () -> instance.stat());
    }

    /* getFormatIterator() */

    @Test
    void getFormatIteratorHasNext() {
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
    void getFormatIteratorNext() {
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
    void getObjectInfo() throws Exception {
        assertNotNull(instance.getObjectInfo());
    }

    @Test
    void getObjectInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.S3SOURCE_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id/suffix", instance.getObjectInfo().getKey());
    }

    @Test
    void getObjectInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.S3SOURCE_PATH_PREFIX, "");
        config.setProperty(Key.S3SOURCE_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("id", instance.getObjectInfo().getKey());
    }

    /* newStreamFactory() */

    @Test
    void newStreamFactoryUsingBasicLookupStrategy() throws Exception {
        assertNotNull(instance.newStreamFactory());
    }

    @Test
    void newStreamFactoryUsingScriptLookupStrategy() throws Exception {
        useScriptLookupStrategy();
        assertNotNull(instance.newStreamFactory());
    }

}
