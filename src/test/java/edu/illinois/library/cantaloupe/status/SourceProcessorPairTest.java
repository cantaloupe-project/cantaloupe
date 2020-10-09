package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SourceProcessorPairTest extends BaseTest {

    private SourceProcessorPair instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Source source = new SourceFactory().newSource("FilesystemSource");
        source.setIdentifier(new Identifier("cats"));
        instance = new SourceProcessorPair(
                source,
                "Java2dProcessor",
                new OperationList());
    }

    @Test
    public void testEqualsWithEqualInstances() throws Exception {
        SourceProcessorPair instance2 = new SourceProcessorPair(
                new SourceFactory().newSource("FilesystemSource"),
                "Java2dProcessor",
                new OperationList());
        assertEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithDifferentSources() throws Exception {
        SourceProcessorPair instance2 = new SourceProcessorPair(
                new SourceFactory().newSource("HttpSource"),
                "Java2dProcessor",
                new OperationList());
        assertNotEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithDifferentProcessorNames() throws Exception {
        SourceProcessorPair instance2 = new SourceProcessorPair(
                new SourceFactory().newSource("FilesystemSource"),
                "PdfBoxProcessor",
                new OperationList());
        assertNotEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithDifferentOperationLists() throws Exception {
        SourceProcessorPair instance2 = new SourceProcessorPair(
                new SourceFactory().newSource("FilesystemSource"),
                "Java2dProcessor",
                OperationList.builder().withOperations(new ScaleByPercent()).build());
        assertEquals(instance, instance2);
    }

    @Test
    public void testHashCodeWithEqualInstances() throws Exception {
        SourceProcessorPair instance2 = new SourceProcessorPair(
                new SourceFactory().newSource("FilesystemSource"),
                "Java2dProcessor",
                new OperationList());
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentSources() throws Exception {
        SourceProcessorPair instance2 = new SourceProcessorPair(
                new SourceFactory().newSource("HttpSource"),
                "Java2dProcessor",
                new OperationList());
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentProcessorNames() throws Exception {
        SourceProcessorPair instance2 = new SourceProcessorPair(
                new SourceFactory().newSource("FilesystemSource"),
                "PdfBoxProcessor",
                new OperationList());
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentOperationLists() throws Exception {
        SourceProcessorPair instance2 = new SourceProcessorPair(
                new SourceFactory().newSource("FilesystemSource"),
                "Java2dProcessor",
                OperationList.builder().withOperations(new ScaleByPercent()).build());
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("cats -> " +
                        "edu.illinois.library.cantaloupe.source.FilesystemSource -> " +
                        "Java2dProcessor",
                instance.toString());
    }

}