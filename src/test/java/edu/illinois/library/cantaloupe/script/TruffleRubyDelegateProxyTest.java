package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class TruffleRubyDelegateProxyTest extends BaseTest {

    private TruffleRubyDelegateProxy instance;

    @BeforeAll
    public static void beforeAll() {
        assumeTrue(DelegateProxyService.isGraalVM(),
                "GraalVM is not available");
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("cats"));

        Path scriptFile = TestUtil.getFixture("delegates-truffle.rb");
        String code = Files.readString(scriptFile);
        TruffleRubyDelegateProxy.load(code);

        instance = new TruffleRubyDelegateProxy(context);
    }

    /* authorize() */

    @Test
    void testAuthorizeReturningTrue() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("whatever"));
        instance.setRequestContext(context);

        assertTrue((boolean) instance.authorize());
    }

    @Test
    void testAuthorizeReturningFalse() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden-boolean.jpg"));
        instance.setRequestContext(context);

        assertFalse((boolean) instance.authorize());
    }

    @Test
    void testAuthorizeReturningMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("redirect.jpg"));
        instance.setRequestContext(context);

        @SuppressWarnings("unchecked")
        Map<String,Object> result = (Map<String,Object>) instance.authorize();
        assertEquals(2, result.size());
        assertEquals("http://example.org/", result.get("location"));
        assertEquals(303, (long) result.get("status_code"));
    }

    /* getAzureStorageSourceBlobKey() */

    @Test
    void testGetAzureStorageSourceBlobKeyReturningString()
            throws Exception {
        String result = instance.getAzureStorageSourceBlobKey();
        assertEquals("cats", result);
    }

    @Test
    void testGetAzureStorageSourceBlobKeyReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("missing"));
        instance.setRequestContext(context);

        assertNull(instance.getAzureStorageSourceBlobKey());
    }

    /* getExtraIIIFInformationResponseKeys() */

    @Test
    void testGetExtraIIIFInformationResponseKeys() throws Exception {
        Map<String, Object> result =
                instance.getExtraIIIFInformationResponseKeys();
        assertEquals(3, result.size());
    }

    @Test
    void testGetExtraIIIFInformationResponseKeysReturningEmptyHash()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("empty"));
        instance.setRequestContext(context);

        Map<String, Object> result =
                instance.getExtraIIIFInformationResponseKeys();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetExtraIIIFInformationResponseKeysReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        Map<String, Object> result =
                instance.getExtraIIIFInformationResponseKeys();
        assertTrue(result.isEmpty());
    }

    /* getFilesystemSourcePathname() */

    @Test
    void testGetFilesystemSourcePathnameReturningString()
            throws Exception {
        String result = instance.getFilesystemSourcePathname();
        assertEquals("cats", result);
    }

    @Test
    void testGetFilesystemSourcePathnameReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("missing"));
        instance.setRequestContext(context);

        assertNull(instance.getFilesystemSourcePathname());
    }

    /* getHttpSourceResourceInfo() */

    @Test
    void testGetHttpSourceResourceInfoReturningString()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("DelegateProxyTest-String"));
        instance.setRequestContext(context);

        Map<String,?> result = instance.getHttpSourceResourceInfo();
        assertEquals(1, result.size());
        assertEquals("http://example.org/foxes", result.get("uri"));
    }

    @Test
    void testGetHttpSourceResourceInfoReturningHash()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("DelegateProxyTest-Hash"));
        instance.setRequestContext(context);

        Map<String,?> result = instance.getHttpSourceResourceInfo();
        assertEquals(1, result.size());
        assertEquals("http://example.org/birds", result.get("uri"));
    }

    @Test
    void testGetHttpSourceResourceInfoReturningNil() throws Exception {
        Map<String,?> result = instance.getHttpSourceResourceInfo();
        assertTrue(result.isEmpty());
    }

    /* getMetadata() */

    @Test
    void testGetMetadata() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("metadata"));
        instance.setRequestContext(context);

        String result = instance.getMetadata();
        assertEquals("<rdf:RDF>derivative metadata</rdf:RDF>", result);
    }

    /* getJdbcSourceDatabaseIdentifier() */

    @Test
    void testGetJdbcSourceDatabaseIdentifier() throws Exception {
        String result = instance.getJdbcSourceDatabaseIdentifier();
        assertEquals("cats", result);
    }

    /* getJdbcSourceMediaType() */

    @Test
    void testGetJdbcSourceMediaType() throws Exception {
        String result = instance.getJdbcSourceMediaType();
        assertEquals("SELECT media_type FROM items WHERE filename = ?", result);
    }

    /* getJdbcSourceLookupSQL() */

    @Test
    void testGetJdbcSourceLookupSQL() throws Exception {
        String result = instance.getJdbcSourceLookupSQL();
        assertEquals("SELECT image FROM items WHERE filename = ?", result);
    }

    /* getOverlayProperties() */

    @Test
    void testGetOverlayPropertiesReturningHash() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("image"));
        instance.setRequestContext(context);

        Map<String,Object> result = instance.getOverlayProperties();
        assertEquals(3, result.size());
    }

    @Test
    void testGetOverlayPropertiesReturningNil() throws Exception {
        assertEquals(0, instance.getOverlayProperties().size());
    }

    /* getRedactions() */

    @Test
    void testGetRedactions() throws Exception {
        List<Map<String,Long>> result = instance.getRedactions();
        assertEquals(1, result.size());
    }

    @Test
    void testGetRedactionsReturningEmptyArray() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("empty"));
        instance.setRequestContext(context);

        List<Map<String,Long>> result = instance.getRedactions();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRedactionsReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        List<Map<String,Long>> result = instance.getRedactions();
        assertTrue(result.isEmpty());
    }

    /* getSource() */

    @Test
    void testGetSource() throws Exception {
        assertEquals("FilesystemSource", instance.getSource());
    }

    @Test
    void testGetSourceReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        assertNull(instance.getSource());
    }

    /* getS3SourceObjectInfo() */

    @Test
    void testGetS3SourceObjectInfo() throws Exception {
        Map<String,String> result = instance.getS3SourceObjectInfo();
        assertEquals("cats", result.get("key"));
        assertEquals("test.cantaloupe.library.illinois.edu",
                result.get("bucket"));
    }

    @Test
    void testGetS3SourceObjectInfoReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        Map<String,String> result = instance.getS3SourceObjectInfo();
        assertTrue(result.isEmpty());
    }

}
