package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HTTPRequestInfoTest extends BaseTest {

    private HTTPRequestInfo instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new HTTPRequestInfo();
        instance.setURI("http://example.org/cats");
        instance.setUsername("user");
        instance.setSecret("secret");
        instance.setHeaders(Map.of("X-Animal", "cats"));
        instance.setSendingHeadRequest(true);
    }

    @Test
    void getBasicAuthTokenWithoutUserAndSecret() {
        instance = new HTTPRequestInfo();
        instance.setURI("http://example.org/cats");
        assertNull(instance.getBasicAuthToken());
    }

    @Test
    void getBasicAuthTokenWithUserAndSecret() {
        assertEquals("dXNlcjpzZWNyZXQ=", instance.getBasicAuthToken());
    }

    @Test
    void setHeaders() {
        assertEquals(1, instance.getHeaders().size());

        instance.setHeaders(Map.of("X-Cats", "yes"));
        assertEquals(2, instance.getHeaders().size());
        assertEquals("yes", instance.getHeaders().getFirstValue("X-Cats"));
    }

    @Test
    void setHeadersWithNullArgument() {
        instance.setHeaders(null);
        assertEquals(0, instance.getHeaders().size());
    }

}
