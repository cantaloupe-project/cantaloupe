package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Does not extend {@link AbstractConfigurationTest} because that requires
 * writability.
 */
public class EnvironmentConfigurationTest extends BaseTest {

    private EnvironmentConfiguration instance;

    @Before
    public void setUp() throws Exception {
        instance = new EnvironmentConfiguration();
    }

    @Test
    public void toEnvironmentKey() {
        assertEquals(EnvironmentConfiguration.ENVIRONMENT_KEY_PREFIX + "_ABCABC123__________________",
                EnvironmentConfiguration.toEnvironmentKey("ABCabc123_.-?=~`!@#$%^&*()+"));
    }

}
