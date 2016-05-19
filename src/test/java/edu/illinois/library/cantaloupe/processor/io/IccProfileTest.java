package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IccProfileTest {

    private IccProfile instance;

    @Before
    public void setUp() throws Exception {
        instance = new IccProfile("test",
                TestUtil.getFixture("AdobeRGB1998.icc"));
    }

    @Test
    public void testGetFile() throws Exception {
        assertEquals(TestUtil.getFixture("AdobeRGB1998.icc"),
                instance.getFile());
    }

    @Test
    public void testGetName() throws Exception {
        assertEquals("test", instance.getName());
    }

    @Test
    public void testGetProfile() throws Exception {
        assertNotNull(instance.getProfile());
    }

}