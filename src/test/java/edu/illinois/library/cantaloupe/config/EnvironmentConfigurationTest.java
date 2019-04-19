package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Does not extend {@link AbstractConfigurationTest} because that requires
 * writability.
 */
public class EnvironmentConfigurationTest extends BaseTest {

    private EnvironmentConfiguration instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new EnvironmentConfiguration();
    }

    @Test
    void toEnvironmentKey() {
        assertEquals("ABCABC123__________________",
                EnvironmentConfiguration.toEnvironmentKey("ABCabc123_.-?=~`!@#$%^&*()+"));
    }

}
