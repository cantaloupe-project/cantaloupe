package edu.illinois.library.cantaloupe.source;

import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.test.AzureStorageTestUtil;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class AzureStorageHTTPImageInputStreamClientTest extends BaseTest {

    private AzureStorageHTTPImageInputStreamClient instance;

    @BeforeClass
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        AzureStorageTestUtil.uploadFixtures();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        AzureStorageTestUtil.deleteFixtures();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        CloudBlockBlob blob = AzureStorageTestUtil.client()
                .getContainerReference(AzureStorageTestUtil.getContainer())
                .getBlockBlobReference("jpg");
        instance = new AzureStorageHTTPImageInputStreamClient(blob);
    }

    @Test
    public void testSendHEADRequest() throws Exception {
        Response actual = instance.sendHEADRequest();
        assertEquals(200, actual.getStatus());
        assertEquals("bytes", actual.getHeaders().getFirstValue("Accept-Ranges"));
        assertEquals("5439", actual.getHeaders().getFirstValue("Content-Length"));
    }

    @Test
    public void testSendGETRequest() throws Exception {
        Response actual = instance.sendGETRequest(new Range(10, 50, 5439));
        assertEquals(206, actual.getStatus());
        assertEquals(41, actual.getBody().length);
    }

}
