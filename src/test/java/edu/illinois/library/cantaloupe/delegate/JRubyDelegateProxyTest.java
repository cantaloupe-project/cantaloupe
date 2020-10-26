package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JRubyDelegateProxyTest extends BaseTest {

    private JRubyDelegateProxy instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("cats"));

        Path scriptFile = TestUtil.getFixture("delegates.rb");
        String code = Files.readString(scriptFile);
        JRubyDelegateProxy.load(code);

        instance = new JRubyDelegateProxy();
        instance.setRequestContext(context);
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
        assertEquals("http://example.org/", result.get("location"));
        assertEquals(303, (long) result.get("status_code"));
    }

    /* deserializeMetaIdentifier() */

    @Test
    void testDeserializeMetaIdentifier() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("whatever"));
        context.setPageNumber(3);
        context.setScaleConstraint(new ScaleConstraint(2, 3));

        Map<String,Object> result = instance.deserializeMetaIdentifier("whatever;3;2:3");
        assertEquals("whatever", result.get("identifier"));
        assertEquals(3L, result.get("page_number"));
        assertEquals(List.of(2L, 3L), result.get("scale_constraint"));
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

    /* getExtraIIIF2InformationResponseKeys() */

    @Test
    void testGetExtraIIIF2InformationResponseKeys() throws Exception {
        Map<String, Object> result =
                instance.getExtraIIIF2InformationResponseKeys();
        assertEquals(3, result.size());
    }

    @Test
    void testGetExtraIIIF2InformationResponseKeysReturningEmptyHash()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("empty"));
        instance.setRequestContext(context);

        Map<String, Object> result =
                instance.getExtraIIIF2InformationResponseKeys();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetExtraIIIF2InformationResponseKeysReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        Map<String, Object> result =
                instance.getExtraIIIF2InformationResponseKeys();
        assertTrue(result.isEmpty());
    }

    /* getExtraIIIF3InformationResponseKeys() */

    @Test
    void testGetExtraIIIF3InformationResponseKeys() throws Exception {
        Map<String, Object> result =
                instance.getExtraIIIF3InformationResponseKeys();
        assertEquals(3, result.size());
    }

    @Test
    void testGetExtraIIIF3InformationResponseKeysReturningEmptyHash()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("empty"));
        instance.setRequestContext(context);

        Map<String, Object> result =
                instance.getExtraIIIF3InformationResponseKeys();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetExtraIIIF3InformationResponseKeysReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        Map<String, Object> result =
                instance.getExtraIIIF3InformationResponseKeys();
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
        instance.getRequestContext().setIdentifier(new Identifier("redacted"));
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

    /* preAuthorize() */

    @Test
    void testPreAuthorizeReturningTrue() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("whatever"));
        instance.setRequestContext(context);

        assertTrue((boolean) instance.preAuthorize());
    }

    @Test
    void testPreAuthorizeReturningFalse() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden-boolean.jpg"));
        instance.setRequestContext(context);

        assertFalse((boolean) instance.preAuthorize());
    }

    @Test
    void testPreAuthorizeReturningMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("redirect.jpg"));
        instance.setRequestContext(context);

        @SuppressWarnings("unchecked")
        Map<String,Object> result = (Map<String,Object>) instance.preAuthorize();
        assertEquals("http://example.org/", result.get("location"));
        assertEquals(303, (long) result.get("status_code"));
    }

    /* serializeMetaIdentifier() */

    @Test
    void testSerializeMetaIdentifier() throws Exception {
        Map<String,Object> map = Map.of(
                "identifier", "whatever",
                "page_number", 3,
                "scale_constraint", List.of(2, 3));
        String result = instance.serializeMetaIdentifier(map);
        assertEquals("whatever;3;2:3", result);
    }

}
