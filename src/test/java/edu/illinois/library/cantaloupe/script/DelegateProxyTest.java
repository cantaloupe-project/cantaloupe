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
        String code = new String(Files.readAllBytes(scriptFile), "UTF-8");
        DelegateProxy.load(code);

        instance = new DelegateProxy(context);
    }

    /* getAzureStorageResolverBlobKey() */

    @Test
    public void testGetAzureStorageResolverBlobKeyReturningString()
            throws Exception {
        String result = instance.getAzureStorageResolverBlobKey();
        assertEquals("cats", result);
    }

    @Test
    public void testGetAzureStorageResolverBlobKeyReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("missing"));
        instance.setRequestContext(context);

        assertNull(instance.getAzureStorageResolverBlobKey());
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

    /* getFilesystemResolverPathname() */

    @Test
    public void testGetFilesystemResolverPathnameReturningString()
            throws Exception {
        String result = instance.getFilesystemResolverPathname();
        assertEquals("cats", result);
    }

    @Test
    public void testGetFilesystemResolverPathnameReturningNil()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("missing"));
        instance.setRequestContext(context);

        assertNull(instance.getFilesystemResolverPathname());
    }

    /* getHttpResolverResourceInfo() */

    @Test
    public void testGetHttpResolverResourceInfoReturningString()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("DelegateProxyTest-String"));
        instance.setRequestContext(context);

        Map<String,String> result = instance.getHttpResolverResourceInfo();
        assertEquals(1, result.size());
        assertEquals("http://example.org/foxes", result.get("uri"));
    }

    @Test
    public void testGetHttpResolverResourceInfoReturningHash()
            throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("DelegateProxyTest-Hash"));
        instance.setRequestContext(context);

        Map<String,String> result = instance.getHttpResolverResourceInfo();
        assertEquals(1, result.size());
        assertEquals("http://example.org/birds", result.get("uri"));
    }

    @Test
    public void testGetHttpResolverResourceInfoReturningNil() throws Exception {
        Map<String,String> result = instance.getHttpResolverResourceInfo();
        assertTrue(result.isEmpty());
    }

    /* getJdbcResolverDatabaseIdentifier() */

    @Test
    public void testGetJdbcResolverDatabaseIdentifier() throws Exception {
        String result = instance.getJdbcResolverDatabaseIdentifier();
        assertEquals("cats", result);
    }

    /* getJdbcResolverMediaType() */

    @Test
    public void testGetJdbcResolverMediaType() throws Exception {
        String result = instance.getJdbcResolverMediaType();
        assertEquals("SELECT media_type FROM items WHERE filename = ?", result);
    }

    /* getJdbcResolverLookupSQL() */

    @Test
    public void testGetJdbcResolverLookupSQL() throws Exception {
        String result = instance.getJdbcResolverLookupSQL();
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

    /* getRedirect() */

    @Test
    public void testGetRedirect() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("redirect.jpg"));
        instance.setRequestContext(context);

        Map<String,Object> result = instance.getRedirect();
        assertEquals("http://example.org/", result.get("location"));
        assertEquals(303, (long) result.get("status_code"));
    }

    @Test
    public void testGetRedirectReturningNil() throws Exception {
        Map<String,Object> result = instance.getRedirect();
        assertTrue(result.isEmpty());
    }

    /* getResolver() */

    @Test
    public void testGetResolver() throws Exception {
        assertEquals("FilesystemResolver", instance.getResolver());
    }

    @Test
    public void testGetResolverReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        assertNull(instance.getResolver());
    }

    /* getS3ResolverObjectInfo() */

    @Test
    public void testGetS3ResolverObjectInfo() throws Exception {
        Map<String,String> result = instance.getS3ResolverObjectInfo();
        assertEquals("cats", result.get("key"));
        assertEquals("test.cantaloupe.library.illinois.edu",
                result.get("bucket"));
    }

    @Test
    public void testGetS3ResolverObjectInfoReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        Map<String,String> result = instance.getS3ResolverObjectInfo();
        assertTrue(result.isEmpty());
    }

    /* isAuthorized() */

    @Test
    public void testIsAuthorizedReturningTrue() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("whatever"));
        instance.setRequestContext(context);

        assertTrue(instance.isAuthorized());
    }

    @Test
    public void testIsAuthorizedReturningFalse() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden.jpg"));
        instance.setRequestContext(context);

        assertFalse(instance.isAuthorized());
    }

}
