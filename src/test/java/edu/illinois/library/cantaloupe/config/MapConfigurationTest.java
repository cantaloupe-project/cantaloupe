package edu.illinois.library.cantaloupe.config;

import org.junit.jupiter.api.BeforeEach;

public class MapConfigurationTest extends AbstractConfigurationTest {

    private MapConfiguration instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new MapConfiguration();
    }

    protected Configuration getInstance() {
        return instance;
    }

}
