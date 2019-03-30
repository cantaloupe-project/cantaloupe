package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DelegateProxyTest extends BaseTest {

    private DelegateProxy instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("cats"));

        Path scriptFile = TestUtil.getFixture("delegates.rb");
        String code = Files.readString(scriptFile);
        DelegateProxy.load(code);

        instance = new DelegateProxy(context);
    }

    /* authorize() */

    @Test
    public void testAuthorizeReturningTrue() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("whatever"));
        instance.setRequestContext(context);

        assertTrue((boolean) instance.authorize());
    }

    @Test
    public void testAuthorizeReturningFalse() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden-boolean.jpg"));
        instance.setRequestContext(context);

        assertFalse((boolean) instance.authorize());
    }

    @Test
    public void testAuthorizeReturningMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("redirect.jpg"));
        instance.setRequestContext(context);

        @SuppressWarnings("unchecked")
        Map<String,Object> result = (Map<String,Object>) instance.authorize();
        assertEquals("http://example.org/", result.get("location"));
        assertEquals(303, (long) result.get("status_code"));
    }

    /* getAzureStorageSourceBlobKey() */

    @Test
    public void testGetAzureStorageSourceBlobKeyReturningString()
            throws Exception {
        String result = instance.getAzureStorageSourceBlobKey();
        assertEquals("cats", result);
    }

    @Test
    public void testGetAzureStorageSourceBlobKeyReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("missing"));
        instance.setRequestContext(context);

        assertNull(instance.getAzureStorageSourceBlobKey());
    }

    /* getExtraIIIFInformationResponseKeys() */

    @Test
    public void testGetExtraIIIFInformationResponseKeys() throws Exception {
        Map<String, Object> result =
                instance.getExtraIIIFInformationResponseKeys();
        assertEquals(3, result.size());
    }

    @Test
    public void testGetExtraIIIFInformationResponseKeysReturningEmptyHash()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("empty"));
        instance.setRequestContext(context);

        Map<String, Object> result =
                instance.getExtraIIIFInformationResponseKeys();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetExtraIIIFInformationResponseKeysReturningNil()
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
    public void testGetFilesystemSourcePathnameReturningString()
            throws Exception {
        String result = instance.getFilesystemSourcePathname();
        assertEquals("cats", result);
    }

    @Test
    public void testGetFilesystemSourcePathnameReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("missing"));
        instance.setRequestContext(context);

        assertNull(instance.getFilesystemSourcePathname());
    }

    /* getHttpSourceResourceInfo() */

    @Test
    public void testGetHttpSourceResourceInfoReturningString()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("DelegateProxyTest-String"));
        instance.setRequestContext(context);

        Map<String,?> result = instance.getHttpSourceResourceInfo();
        assertEquals(1, result.size());
        assertEquals("http://example.org/foxes", result.get("uri"));
    }

    @Test
    public void testGetHttpSourceResourceInfoReturningHash()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("DelegateProxyTest-Hash"));
        instance.setRequestContext(context);

        Map<String,?> result = instance.getHttpSourceResourceInfo();
        assertEquals(1, result.size());
        assertEquals("http://example.org/birds", result.get("uri"));
    }

    @Test
    public void testGetHttpSourceResourceInfoReturningNil() throws Exception {
        Map<String,?> result = instance.getHttpSourceResourceInfo();
        assertTrue(result.isEmpty());
    }

    /* getJdbcSourceDatabaseIdentifier() */

    @Test
    public void testGetJdbcSourceDatabaseIdentifier() throws Exception {
        String result = instance.getJdbcSourceDatabaseIdentifier();
        assertEquals("cats", result);
    }

    /* getJdbcSourceMediaType() */

    @Test
    public void testGetJdbcSourceMediaType() throws Exception {
        String result = instance.getJdbcSourceMediaType();
        assertEquals("SELECT media_type FROM items WHERE filename = ?", result);
    }

    /* getJdbcSourceLookupSQL() */

    @Test
    public void testGetJdbcSourceLookupSQL() throws Exception {
        String result = instance.getJdbcSourceLookupSQL();
        assertEquals("SELECT image FROM items WHERE filename = ?", result);
    }

    /* getOverlayProperties() */

    @Test
    public void testGetOverlayPropertiesReturningHash() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("image"));
        instance.setRequestContext(context);

        Map<String,Object> result = instance.getOverlayProperties();
        assertEquals(3, result.size());
    }

    @Test
    public void testGetOverlayPropertiesReturningNil() throws Exception {
        assertEquals(0, instance.getOverlayProperties().size());
    }

    /* getRedactions() */

    @Test
    public void testGetRedactions() throws Exception {
        List<Map<String,Long>> result = instance.getRedactions();
        assertEquals(1, result.size());
    }

    @Test
    public void testGetRedactionsReturningEmptyArray() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("empty"));
        instance.setRequestContext(context);

        List<Map<String,Long>> result = instance.getRedactions();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetRedactionsReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        List<Map<String,Long>> result = instance.getRedactions();
        assertTrue(result.isEmpty());
    }

    /* getSource() */

    @Test
    public void testGetSource() throws Exception {
        assertEquals("FilesystemSource", instance.getSource());
    }

    @Test
    public void testGetSourceReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        assertNull(instance.getSource());
    }

    /* getS3SourceObjectInfo() */

    @Test
    public void testGetS3SourceObjectInfo() throws Exception {
        Map<String,String> result = instance.getS3SourceObjectInfo();
        assertEquals("cats", result.get("key"));
        assertEquals("test.cantaloupe.library.illinois.edu",
                result.get("bucket"));
    }

    @Test
    public void testGetS3SourceObjectInfoReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        Map<String,String> result = instance.getS3SourceObjectInfo();
        assertTrue(result.isEmpty());
    }

}
