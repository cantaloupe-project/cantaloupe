package edu.illinois.library.cantaloupe.operation.redaction;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class RedactionServiceTest extends BaseTest {

    private RedactionService instance;

    public static void setUpConfiguration() throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());
        config.setProperty(Key.REDACTION_ENABLED, true);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setUpConfiguration();
        instance = new RedactionService();
    }

    @Test
    public void testIsEnabledWhenEnabled() {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.REDACTION_ENABLED, true);
        assertTrue(instance.isEnabled());
    }

    @Test
    public void testIsEnabledWhenDisabled() {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.REDACTION_ENABLED, false);
        assertFalse(instance.isEnabled());
    }

    @Test
    public void testRedactionsForWithRedactions() throws Exception {
        RequestContext context = new RequestContext();
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        List<Redaction> redactions = instance.redactionsFor(proxy);

        assertEquals(1, redactions.size());
        assertEquals(new Rectangle(0, 10, 50, 70),
                redactions.get(0).getRegion());
    }

    @Test
    public void testRedactionsForWithNoRedactions() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        List<Redaction> redactions = instance.redactionsFor(proxy);

        assertTrue(redactions.isEmpty());
    }

}
