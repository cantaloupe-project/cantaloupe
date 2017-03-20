package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.test.BaseTest;

import org.junit.Test;

import static org.junit.Assert.*;

public class AbstractResourceTest extends BaseTest {

    private static class TestResource extends AbstractResource {
    }

    private TestResource resource = new TestResource();

    @Test
    public void testTemplateWithValidTemplate() {
        assertNotNull(resource.template("/error.vm"));
    }

    @Test
    public void testTemplateWithInvalidTemplate() {
        assertNotNull(resource.template("/bogus.vm"));
    }

}
