package edu.illinois.library.cantaloupe.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;

public class SourceUsageTest extends BaseTest {

    private SourceUsage instance;
    private Configuration config = Configuration.getInstance();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Source source = new SourceFactory(config).newSource("FilesystemSource");
        source.setIdentifier(new Identifier("cats"));
        instance = new SourceUsage(source);
    }

    @Test
    void equalsWithEqualInstances() throws Exception {
        SourceUsage instance2 = new SourceUsage(
                new SourceFactory(config).newSource("FilesystemSource"));
        assertEquals(instance, instance2);
    }

    @Test
    void equalsWithSameSourceClassesButDifferentIdentifiers() throws Exception {
        Source source2 = new SourceFactory(config).newSource("FilesystemSource");
        source2.setIdentifier(new Identifier("different"));
        SourceUsage instance2 = new SourceUsage(source2);
        assertEquals(instance, instance2);
    }

    @Test
    void equalsWithDifferentSourceClasses() throws Exception {
        Source source2 = new SourceFactory(config).newSource("HttpSource");
        source2.setIdentifier(new Identifier("cats"));
        SourceUsage instance2 = new SourceUsage(source2);
        assertNotEquals(instance, instance2);
    }

    @Test
    void hashCodeWithEqualInstances() throws Exception {
        SourceUsage instance2 = new SourceUsage(
                new SourceFactory(config).newSource("FilesystemSource"));
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void hashCodeWithSameSourceClassesButDifferentIdentifiers()
            throws Exception {
        Source source2 = new SourceFactory(config).newSource("FilesystemSource");
        source2.setIdentifier(new Identifier("different"));
        SourceUsage instance2 = new SourceUsage(source2);
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void hashCodeWithDifferentSources() throws Exception {
        Source source2 = new SourceFactory(config).newSource("HttpSource");
        source2.setIdentifier(new Identifier("cats"));
        SourceUsage instance2 = new SourceUsage(source2);
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("cats -> edu.illinois.library.cantaloupe.source.FilesystemSource",
                instance.toString());
    }

}