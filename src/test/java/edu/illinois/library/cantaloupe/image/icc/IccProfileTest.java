package edu.illinois.library.cantaloupe.image.icc;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

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

    @Test
    public void testGetProfileWithNonExistingFile() throws Exception {
        instance.setFile(new File("/bogus/bogus.icc"));
        try {
            assertNotNull(instance.getProfile());
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(500, 500);
        assertEquals(fullSize, instance.getResultingSize(fullSize));
    }

    @Test
    public void testIsNoOp() {
        assertFalse(instance.isNoOp());
    }

    @Test
    public void testToMap() throws Exception {
        Dimension fullSize = new Dimension(500, 500);
        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals("icc_profile", map.get("operation"));
        assertEquals("test", map.get("name"));
        assertEquals(TestUtil.getFixture("AdobeRGB1998.icc").getAbsolutePath(),
                map.get("pathname"));
    }

    @Test
    public void testToString() {
        assertEquals("AdobeRGB1998.icc", instance.toString());
    }

}