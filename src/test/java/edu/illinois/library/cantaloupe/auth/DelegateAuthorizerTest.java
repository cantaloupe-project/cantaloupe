package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DelegateAuthorizerTest extends BaseTest {

    private DelegateAuthorizer instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        instance = new DelegateAuthorizer(proxy);
    }

    @Test
    void testConstructorWithEmptyArgument() {
        Assertions.assertThrows(IllegalArgumentException.class,
                DelegateAuthorizer::new);
    }

    /* authorize() */

    @Test
    void testAuthorizeWithDelegateProxyReturningTrue() throws Exception {
        AuthInfo info = instance.authorize();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningFalse() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("forbidden-boolean.jpg"));
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.authorize();
        assertEquals(403, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningUnauthorizedMap() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("forbidden-code.jpg"));
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.authorize();
        assertEquals(401, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningRedirectMap() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("redirect.jpg"));
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.authorize();
        assertEquals(303, info.getResponseStatus());
        assertEquals("http://example.org/", info.getRedirectURI());
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningScaleConstraintMap() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("reduce.jpg"));
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.authorize();
        assertEquals(302, info.getResponseStatus());
        assertEquals(new ScaleConstraint(1, 2), info.getScaleConstraint());
    }

    /* preAuthorize() */

    @Test
    void testPreAuthorizeWithDelegateProxyReturningTrue() throws Exception {
        AuthInfo info = instance.preAuthorize();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testPreAuthorizeWithDelegateProxyReturningFalse() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("forbidden-boolean.jpg"));
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.preAuthorize();
        assertEquals(403, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testPreAuthorizeWithDelegateProxyReturningUnauthorizedMap() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("forbidden-code.jpg"));
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.preAuthorize();
        assertEquals(401, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testPreAuthorizeWithDelegateProxyReturningRedirectMap() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("redirect.jpg"));
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.preAuthorize();
        assertEquals(303, info.getResponseStatus());
        assertEquals("http://example.org/", info.getRedirectURI());
    }

    @Test
    void testPreAuthorizeWithDelegateProxyReturningScaleConstraintMap()
            throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("reduce.jpg"));
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.preAuthorize();
        assertEquals(302, info.getResponseStatus());
        assertEquals(new ScaleConstraint(1, 2), info.getScaleConstraint());
    }

}
