package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CacheWorkerRunnerTest extends BaseTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        CacheWorkerRunner.clearInstance();
    }

    @Test
    void testGetInstance() {
        assertNotNull(CacheWorkerRunner.getInstance());
    }

}