package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractImageWriterTest extends BaseTest {

    protected ImageWriter instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        if (instance != null) {
            instance.dispose();
        }
    }

    abstract protected ImageWriter newInstance() throws IOException;

    @Test
    void testGetPreferredIIOImplementationsWithNoUserPreference() {
        String[] impls = ((AbstractIIOImageWriter) instance).
                getPreferredIIOImplementations();
        assertArrayEquals(((AbstractIIOImageWriter) instance).
                getApplicationPreferredIIOImplementations(), impls);
    }

}
