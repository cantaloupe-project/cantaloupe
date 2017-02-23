package edu.illinois.library.cantaloupe.config;

import org.junit.Before;

public class MemoryConfigurationTest extends AbstractConfigurationTest {

    private MemoryConfiguration instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new MemoryConfiguration();
    }

    protected Configuration getInstance() {
        return instance;
    }

}
