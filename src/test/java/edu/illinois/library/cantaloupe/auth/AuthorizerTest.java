package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AuthorizerTest extends BaseTest {

    private Authorizer instance;

    @Before
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        RequestContext context = new RequestContext();
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        instance = new Authorizer(proxy);
    }

    /* authorize() */

    @Test
    public void testAuthorizeWithNullDelegateProxy() throws Exception {
        instance = new Authorizer();

        AuthInfo info = instance.authorize();

        assertTrue(info.isAuthorized());
    }

    @Test
    public void testAuthorizeWithDelegateProxyReturningTrue() throws Exception {
        AuthInfo info = instance.authorize();
        assertTrue(info.isAuthorized());
    }

    @Test
    public void testAuthorizeWithDelegateProxyReturningFalse() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden.jpg"));
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance = new Authorizer(proxy);

        AuthInfo info = instance.authorize();
        assertFalse(info.isAuthorized());
    }

    /* redirect() */

    @Test
    public void testRedirectWithNullDelegateProxy() throws Exception {
        instance = new Authorizer();

        RedirectInfo info = instance.redirect();

        assertNull(info);
    }

    @Test
    public void testRedirectWithDelegateProxyReturningNull() throws Exception {
        RedirectInfo info = instance.redirect();
        assertNull(info);
    }

    @Test
    public void testRedirectWithDelegateProxyReturningMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("redirect.jpg"));
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance = new Authorizer(proxy);

        RedirectInfo info = instance.redirect();

        assertEquals(303, info.getRedirectStatus());
        assertEquals("http://example.org/", info.getRedirectURI().toString());
    }

}
