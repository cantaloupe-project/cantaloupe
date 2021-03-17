package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SourceUsageTest extends BaseTest {

    private SourceUsage instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Source source = new SourceFactory().newSource("FilesystemSource");
        source.setIdentifier(new Identifier("cats"));
        instance = new SourceUsage(source);
    }

    @Test
    public void testEqualsWithEqualInstances() throws Exception {
        SourceUsage instance2 = new SourceUsage(
                new SourceFactory().newSource("FilesystemSource"));
        assertEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithDifferentSources() throws Exception {
        SourceUsage instance2 = new SourceUsage(
                new SourceFactory().newSource("HttpSource"));
        assertNotEquals(instance, instance2);
    }

    @Test
    public void testHashCodeWithEqualInstances() throws Exception {
        SourceUsage instance2 = new SourceUsage(
                new SourceFactory().newSource("FilesystemSource"));
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentSources() throws Exception {
        SourceUsage instance2 = new SourceUsage(
                new SourceFactory().newSource("HttpSource"));
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("cats -> edu.illinois.library.cantaloupe.source.FilesystemSource",
                instance.toString());
    }

}
