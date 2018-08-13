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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DelegateAuthorizerTest extends BaseTest {

    private DelegateAuthorizer instance;

    @Before
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

    @Test(expected = NullPointerException.class)
    public void testConstructorWithEmptyArgument() {
        new DelegateAuthorizer();
    }

    @Test
    public void testAuthorizeWithDelegateProxyReturningTrue() throws Exception {
        AuthInfo info = instance.authorize();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    public void testAuthorizeWithDelegateProxyReturningFalse() throws Exception {
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
    public void testAuthorizeWithDelegateProxyReturningUnauthorizedMap() throws Exception {
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
    public void testAuthorizeWithDelegateProxyReturningRedirectMap() throws Exception {
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
    public void testAuthorizeWithDelegateProxyReturningScaleConstraintMap() throws Exception {
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
