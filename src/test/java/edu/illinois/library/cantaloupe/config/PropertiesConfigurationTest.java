package edu.illinois.library.cantaloupe.config;

import org.junit.Before;

public class PropertiesConfigurationTest extends FileConfigurationTest {

    private PropertiesConfiguration instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new PropertiesConfiguration();
    }

    protected Configuration getInstance() {
        return instance;
    }

}
