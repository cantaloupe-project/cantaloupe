package edu.illinois.library.cantaloupe.operation.redaction;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RedactionServiceTest extends BaseTest {

    public static void setUpConfiguration() throws IOException {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(RedactionService.REDACTION_ENABLED_CONFIG_KEY, true);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setUpConfiguration();
    }

    @Test
    public void testRedactionsFor() throws Exception {
        final Identifier identifier = new Identifier("cats");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        List<Redaction> redactions = RedactionService.redactionsFor(
                identifier, requestHeaders, clientIp, cookies);
        assertEquals(1, redactions.size());
        assertEquals(0, redactions.get(0).getRegion().x);
        assertEquals(10, redactions.get(0).getRegion().y);
        assertEquals(50, redactions.get(0).getRegion().width);
        assertEquals(70, redactions.get(0).getRegion().height);
    }

    @Test
    public void testIsEnabled() {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        // false
        config.setProperty(RedactionService.REDACTION_ENABLED_CONFIG_KEY, false);
        assertFalse(RedactionService.isEnabled());
        // true
        config.setProperty(RedactionService.REDACTION_ENABLED_CONFIG_KEY, true);
        assertTrue(RedactionService.isEnabled());
    }

}
