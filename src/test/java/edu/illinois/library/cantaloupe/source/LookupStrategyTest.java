package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LookupStrategyTest extends BaseTest {

    @Test
    void testFromWithBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY;
        config.setProperty(key, "BasicLookupStrategy");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.BASIC, strategy);
    }

    @Test
    void testFromWithDelegateScriptStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY;
        config.setProperty(key, "ScriptLookupStrategy");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.DELEGATE_SCRIPT, strategy);
    }

    @Test
    void testFromWithIllegalStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY;
        config.setProperty(key, "bogus");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.UNDEFINED, strategy);
    }

}
