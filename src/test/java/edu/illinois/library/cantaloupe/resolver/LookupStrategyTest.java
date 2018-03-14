package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class LookupStrategyTest extends BaseTest {

    @Test
    public void testFromWithBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY;
        config.setProperty(key, "BasicLookupStrategy");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.BASIC, strategy);
    }

    @Test
    public void testFromWithDelegateScriptStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY;
        config.setProperty(key, "ScriptLookupStrategy");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.DELEGATE_SCRIPT, strategy);
    }

    @Test
    public void testFromWithIllegalStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY;
        config.setProperty(key, "bogus");

        LookupStrategy strategy = LookupStrategy.from(key);
        assertEquals(LookupStrategy.UNDEFINED, strategy);
    }

}
