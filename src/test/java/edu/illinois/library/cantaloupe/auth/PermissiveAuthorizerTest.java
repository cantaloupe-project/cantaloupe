package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PermissiveAuthorizerTest extends BaseTest {

    private PermissiveAuthorizer instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new PermissiveAuthorizer();
    }

    @Test
    void testAuthorize() {
        AuthInfo info = instance.authorize();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

}
