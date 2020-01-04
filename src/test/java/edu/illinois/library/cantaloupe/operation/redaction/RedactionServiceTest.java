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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RedactionServiceTest extends BaseTest {

    private RedactionService instance;

    public static void setUpConfiguration() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates-truffle.rb").toString());
        config.setProperty(Key.REDACTION_ENABLED, true);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        setUpConfiguration();
        instance = new RedactionService();
    }

    @Test
    void testIsEnabledWhenEnabled() {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.REDACTION_ENABLED, true);
        assertTrue(instance.isEnabled());
    }

    @Test
    void testIsEnabledWhenDisabled() {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.REDACTION_ENABLED, false);
        assertFalse(instance.isEnabled());
    }

    @Test
    void testRedactionsForWithRedactions() throws Exception {
        RequestContext context = new RequestContext();
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        List<Redaction> redactions = instance.redactionsFor(proxy);

        assertEquals(1, redactions.size());
        assertEquals(new Rectangle(0, 10, 50, 70),
                redactions.get(0).getRegion());
    }

    @Test
    void testRedactionsForWithNoRedactions() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        List<Redaction> redactions = instance.redactionsFor(proxy);

        assertTrue(redactions.isEmpty());
    }

}
