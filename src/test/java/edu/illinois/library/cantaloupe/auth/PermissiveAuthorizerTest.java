package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PermissiveAuthorizerTest extends BaseTest {

    private PermissiveAuthorizer instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new PermissiveAuthorizer();
    }

    @Test
    public void testAuthorize() {
        AuthInfo info = instance.authorize();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

}
