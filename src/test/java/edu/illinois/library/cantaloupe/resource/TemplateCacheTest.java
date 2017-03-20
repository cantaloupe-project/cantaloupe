package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TemplateCacheTest extends BaseTest {

    private TemplateCache instance;

    @Before
    public void setUp() {
        instance = new TemplateCache();
    }

    @Test
    public void testGetWithValidTemplate() {
        String template = instance.get("/error.vm");
        assertTrue(template.startsWith("<!DOCTYPE html>"));
    }

    @Test
    public void testGetWithInvalidTemplate() {
        assertNull(instance.get("/bogus.vm"));
    }

    @Test
    public void testSize() {
        assertEquals(0, instance.size());
        instance.get("/error.vm");
        assertEquals(1, instance.size());
    }

}
