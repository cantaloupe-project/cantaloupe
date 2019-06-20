package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HTTPRequestInfoTest extends BaseTest {

    private HTTPRequestInfo instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Map<String,String> headers = new HashMap<>();
        headers.put("X-Animal", "cats");

        instance = new HTTPRequestInfo("http://example.org/cats",
                "user", "secret", headers);
    }

    @Test
    void testGetBasicAuthTokenWithoutUserAndSecret() {
        instance = new HTTPRequestInfo("http://example.org/cats");
        assertNull(instance.getBasicAuthToken());
    }

    @Test
    void testGetBasicAuthTokenWithUserAndSecret() {
        assertEquals("dXNlcjpzZWNyZXQ=", instance.getBasicAuthToken());
    }

}
