package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
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

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        RequestContext context = new RequestContext();
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        instance = new DelegateAuthorizer(proxy);
    }

    @Test
    void testConstructorWithEmptyArgument() {
        Assertions.assertThrows(NullPointerException.class,
                DelegateAuthorizer::new);
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningTrue() throws Exception {
        AuthInfo info = instance.authorize();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningFalse() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden-boolean.jpg"));
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.authorize();
        assertEquals(403, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningUnauthorizedMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden-code.jpg"));
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.authorize();
        assertEquals(401, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningRedirectMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("redirect.jpg"));
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.authorize();
        assertEquals(303, info.getResponseStatus());
        assertEquals("http://example.org/", info.getRedirectURI());
    }

    @Test
    void testAuthorizeWithDelegateProxyReturningScaleConstraintMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("reduce.jpg"));
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance = new DelegateAuthorizer(proxy);

        AuthInfo info = instance.authorize();
        assertEquals(302, info.getResponseStatus());
        assertEquals(new ScaleConstraint(1, 2), info.getScaleConstraint());
    }

}
