package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

abstract class AbstractImageWriterTest extends BaseTest {

    ImageWriter instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @After
    public void tearDown() throws Exception {
        if (instance != null) {
            instance.dispose();
        }
    }

    abstract ImageWriter newInstance() throws IOException;

    @Test
    public void testGetPreferredIIOImplementationsWithNoUserPreference() {
        String[] impls = ((AbstractIIOImageWriter) instance).
                getPreferredIIOImplementations();
        assertArrayEquals(((AbstractIIOImageWriter) instance).
                getApplicationPreferredIIOImplementations(), impls);
    }

}
