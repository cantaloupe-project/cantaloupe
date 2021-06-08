package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetaIdentifierTest extends BaseTest {

    @Nested
    class BuilderTest extends BaseTest {

        @Test
        void testBuildWithoutIdentifierThrowsException() {
            MetaIdentifier.Builder builder = MetaIdentifier.builder()
                    .withPageNumber(2)
                    .withScaleConstraint(1, 2);
            assertThrows(IllegalArgumentException.class, builder::build);
        }

        @Test
        void testBuildWithInvalidPageNumberThrowsException() {
            MetaIdentifier.Builder builder = MetaIdentifier.builder()
                    .withIdentifier("cats")
                    .withPageNumber(-1);
            assertThrows(IllegalArgumentException.class, builder::build);
        }

        @Test
        void testBuild() {
            MetaIdentifier instance = MetaIdentifier.builder()
                    .withIdentifier("cats")
                    .withPageNumber(2)
                    .withScaleConstraint(1, 2)
                    .build();
            assertEquals(new Identifier("cats"), instance.getIdentifier());
            assertEquals(2, instance.getPageNumber());
            assertEquals(new ScaleConstraint(1, 2), instance.getScaleConstraint());
        }

    }

    private MetaIdentifier instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(2)
                .withScaleConstraint(1, 2)
                .build();
    }

    /* fromString() */

    @Test
    void testFromString() {
        final Configuration config =  Configuration.getInstance();
        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());

        DelegateProxy delegateProxy = TestUtil.newDelegateProxy();
        String string               = "cats dogs;2;2:3";
        MetaIdentifier actual       = MetaIdentifier.fromString(string, delegateProxy);
        MetaIdentifier expected     = MetaIdentifier.builder()
                .withIdentifier("cats dogs")
                .withPageNumber(2)
                .withScaleConstraint(2, 3)
                .build();
        assertEquals(expected, actual);
    }

    /* fromURIPathComponent() */

    @Test
    void testFromURIPathComponent() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.SLASH_SUBSTITUTE, "BUG");
        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());

        DelegateProxy delegateProxy = TestUtil.newDelegateProxy();
        String pathComponent        = "catsBUG%3Adogs;2;2:3";
        MetaIdentifier actual       = MetaIdentifier
                .fromURIPathComponent(pathComponent, delegateProxy);
        MetaIdentifier expected     = MetaIdentifier.builder()
                .withIdentifier("cats/:dogs")
                .withPageNumber(2)
                .withScaleConstraint(2, 3)
                .build();
        assertEquals(expected, actual);
    }

    /* MetaIdentifier(MetaIdentifier) */

    @Test
    void testCopyConstructor() {
        MetaIdentifier copy = new MetaIdentifier(instance);
        assertNotSame(copy, instance);
        assertEquals(new Identifier("cats"), copy.getIdentifier());
        assertEquals(2, copy.getPageNumber());
        assertEquals(new ScaleConstraint(1, 2), copy.getScaleConstraint());
    }

    /* equals() */

    @Test
    void testEqualsWithUnequalIdentifiers() {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withPageNumber(2)
                .withScaleConstraint(1, 2);
        MetaIdentifier id1 = builder.withIdentifier("cats").build();
        MetaIdentifier id2 = builder.withIdentifier("dogs").build();
        assertNotEquals(id1, id2);
    }

    @Test
    void testEqualsWithUnequalPageNumbers() {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(1, 2);
        MetaIdentifier id1 = builder.withPageNumber(2).build();
        MetaIdentifier id2 = builder.withPageNumber(3).build();
        assertNotEquals(id1, id2);
    }

    @Test
    void testEqualsWithUnequalScaleConstraints() {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(2);
        MetaIdentifier id1 = builder.withScaleConstraint(1, 2).build();
        MetaIdentifier id2 = builder.withScaleConstraint(1, 3).build();
        assertNotEquals(id1, id2);
    }

    @Test
    void testEqualsWithEqualInstances() {
        MetaIdentifier id1 = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(2)
                .withScaleConstraint(1, 2)
                .build();
        MetaIdentifier id2 = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(2)
                .withScaleConstraint(1, 2)
                .build();
        assertEquals(id1, id2);
    }

    /* hashCode() */

    @Test
    void testHashCodeWithUnequalIdentifiers() {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withPageNumber(2)
                .withScaleConstraint(1, 2);
        MetaIdentifier id1 = builder.withIdentifier("cats").build();
        MetaIdentifier id2 = builder.withIdentifier("dogs").build();
        assertNotEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testHashCodeWithUnequalPageNumbers() {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(1, 2);
        MetaIdentifier id1 = builder.withPageNumber(2).build();
        MetaIdentifier id2 = builder.withPageNumber(3).build();
        assertNotEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testHashCodeWithUnequalScaleConstraints() {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(2);
        MetaIdentifier id1 = builder.withScaleConstraint(1, 2).build();
        MetaIdentifier id2 = builder.withScaleConstraint(1, 3).build();
        assertNotEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testHashCodeWithEqualInstances() {
        MetaIdentifier id1 = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(2)
                .withScaleConstraint(1, 2)
                .build();
        MetaIdentifier id2 = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(2)
                .withScaleConstraint(1, 2)
                .build();
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    /* setIdentifier() */

    @Test
    void testSetIdentifier() {
        instance.setIdentifier(new Identifier("cats"));
        assertEquals(new Identifier("cats"), instance.getIdentifier());
    }

    @Test
    void testSetIdentifierWithNullArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setIdentifier(null));
    }

    /* setPageNumber() */

    @Test
    void testSetPageNumber() {
        instance.setPageNumber(2);
        assertEquals(2, instance.getPageNumber());
    }

    @Test
    void testSetPageNumberWithIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPageNumber(-1));
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPageNumber(0));
    }

    /* setScaleConstraint() */

    @Test
    void testSetScaleConstraint() {
        ScaleConstraint scaleConstraint = new ScaleConstraint(5, 6);
        instance.setScaleConstraint(scaleConstraint);
        assertEquals(scaleConstraint, instance.getScaleConstraint());
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("cats;2;1:2", instance.toString());
    }

}
