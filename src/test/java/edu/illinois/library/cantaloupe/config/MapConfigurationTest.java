package edu.illinois.library.cantaloupe.config;

import org.junit.Before;

public class MapConfigurationTest extends AbstractConfigurationTest {

    private MapConfiguration instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new MapConfiguration();
    }

    protected Configuration getInstance() {
        return instance;
    }

}
