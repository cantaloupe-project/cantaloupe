package edu.illinois.library.cantaloupe.cache;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CacheWorkerRunnerTest {

    @Before
    public void setUp() {
        CacheWorkerRunner.clearInstance();
    }

    @Test
    public void testGetInstance() {
        assertNotNull(CacheWorkerRunner.getInstance());
    }

}