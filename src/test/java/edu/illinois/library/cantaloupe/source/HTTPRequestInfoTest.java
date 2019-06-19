package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class HTTPRequestInfoTest extends BaseTest {

    private HTTPRequestInfo instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Map<String,String> headers = new HashMap<>();
        headers.put("X-Animal", "cats");

        instance = new HTTPRequestInfo("http://example.org/cats",
                "user", "secret", headers);
    }

    @Test
    public void testGetBasicAuthTokenWithoutUserAndSecret() {
        instance = new HTTPRequestInfo("http://example.org/cats");
        assertNull(instance.getBasicAuthToken());
    }

    @Test
    public void testGetBasicAuthTokenWithUserAndSecret() {
        assertEquals("dXNlcjpzZWNyZXQ=", instance.getBasicAuthToken());
    }

}
