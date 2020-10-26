package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelegateMetaIdentifierTransformerTest extends BaseTest {

    private DelegateMetaIdentifierTransformer instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        instance = new DelegateMetaIdentifierTransformer();
        instance.setDelegateProxy(proxy);
    }

    /* deserialize() */

    @Test
    void testDeserializeWithIdentifier() {
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats")
                .build();
        MetaIdentifier actual = instance.deserialize("cats");
        assertEquals(expected, actual);
    }

    @Test
    void testDeserializeWithIdentifierAndPageNumber() {
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(3)
                .build();
        MetaIdentifier actual = instance.deserialize("cats;3");
        assertEquals(expected, actual);
    }

    @Test
    void testDeserializeWithIdentifierAndScaleConstraint() {
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(3, 4)
                .build();
        MetaIdentifier actual = instance.deserialize("cats;3:4");
        assertEquals(expected, actual);
    }

    @Test
    void testDeserializeWithAllComponents() {
        MetaIdentifier expected = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(3)
                .withScaleConstraint(3, 4)
                .build();
        MetaIdentifier actual = instance.deserialize("cats;3;3:4");
        assertEquals(expected, actual);
    }

    /* serialize() */

    @Test
    void testSerializeWithIdentifier() {
        MetaIdentifier metaID = MetaIdentifier.builder()
                .withIdentifier("cats")
                .build();
        assertEquals("cats", instance.serialize(metaID));
    }

    @Test
    void testSerializeWithIdentifierAndPageNumber() {
        MetaIdentifier metaID = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(3)
                .build();
        assertEquals("cats;3", instance.serialize(metaID));
    }

    @Test
    void testSerializeWithIdentifierAndScaleConstraint() {
        MetaIdentifier metaID = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(3, 4)
                .build();
        assertEquals("cats;3:4", instance.serialize(metaID));
    }

    @Test
    void testSerializeWithAllComponents() {
        MetaIdentifier metaID = MetaIdentifier.builder()
                .withIdentifier("cats")
                .withPageNumber(3)
                .withScaleConstraint(3, 4)
                .build();
        assertEquals("cats;3;3:4", instance.serialize(metaID));
    }

}