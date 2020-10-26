package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StandardMetaIdentifierTransformerTest extends BaseTest {

    private StandardMetaIdentifierTransformer instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new StandardMetaIdentifierTransformer();
    }

    /* deserialize() */

    @Test
    void deserializeWithIdentifier() {
        MetaIdentifier metaID = instance.deserialize("cats cats;1;2:3;cats");
        assertEquals(new Identifier("cats cats;1;2:3;cats"),
                metaID.getIdentifier());
        assertNull(metaID.getPageNumber());
        assertNull(metaID.getScaleConstraint());
    }

    @Test
    void deserializeWithIdentifierAndPageNumber() {
        MetaIdentifier metaID = instance.deserialize("cats;cats;3");
        assertEquals(new Identifier("cats;cats"), metaID.getIdentifier());
        assertEquals(3, metaID.getPageNumber());
        assertNull(metaID.getScaleConstraint());
    }

    @Test
    void deserializeWithIdentifierAndScaleConstraint() {
        MetaIdentifier metaID = instance.deserialize("cats;cats;1:2");
        assertEquals(new Identifier("cats;cats"), metaID.getIdentifier());
        assertNull(metaID.getPageNumber());
        assertEquals(new ScaleConstraint(1, 2), metaID.getScaleConstraint());
    }

    @Test
    void deserializeWithIdentifierAndPageNumberAndScaleConstraint() {
        MetaIdentifier metaID = instance.deserialize("cats;cats;3;1:2");
        assertEquals(new Identifier("cats;cats"), metaID.getIdentifier());
        assertEquals(3, metaID.getPageNumber());
        assertEquals(new ScaleConstraint(1, 2), metaID.getScaleConstraint());
    }

    @Test
    void deserializeRespectsMetaIdentifierDelimiter() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER, "CATS");
        MetaIdentifier metaID = instance.deserialize("catsCATS2CATS2:3");
        assertEquals(new Identifier("cats"), metaID.getIdentifier());
        assertEquals(2, metaID.getPageNumber());
        assertEquals(new ScaleConstraint(2, 3), metaID.getScaleConstraint());
    }

    /* serialize(MetaIdentifier) */

    @Test
    void serialize1WithIdentifier() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .build();
        assertEquals("cats;cats", instance.serialize(meta));
    }

    @Test
    void serialize1WithIdentifierAndPageNumber() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(3)
                .build();
        assertEquals("cats;cats;3", instance.serialize(meta));
    }

    @Test
    void serialize1OmitsPage1() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(1)
                .build();
        assertEquals("cats;cats", instance.serialize(meta));
    }

    @Test
    void serialize1WithIdentifierAndScaleConstraint() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withScaleConstraint(2, 3)
                .build();
        assertEquals("cats;cats;2:3", instance.serialize(meta));
    }

    @Test
    void serialize1OmitsNoOpScaleConstraint() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withScaleConstraint(2, 2)
                .build();
        assertEquals("cats;cats", instance.serialize(meta));
    }

    @Test
    void serialize1WithIdentifierAndPageNumberAndScaleConstraint() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(3)
                .withScaleConstraint(2, 3)
                .build();
        assertEquals("cats;cats;3;2:3", instance.serialize(meta));
    }

    @Test
    void serialize1RespectsMetaIdentifierDelimiter() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER, "DOGS");
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(3)
                .withScaleConstraint(2, 3)
                .build();
        assertEquals("cats;catsDOGS3DOGS2:3", instance.serialize(meta));
    }

    /* serialize(MetaIdentifier, boolean) */

    @Test
    void serialize2IncludesPage1WhenNotNormalizing() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withPageNumber(1)
                .build();
        assertEquals("cats;cats;1", instance.serialize(meta, false));
    }

    @Test
    void serialize2IncludesNoOpScaleConstraintWhenNotNormalizing() {
        MetaIdentifier meta = MetaIdentifier.builder()
                .withIdentifier("cats;cats")
                .withScaleConstraint(2, 2)
                .build();
        assertEquals("cats;cats;2:2", instance.serialize(meta, false));
    }

}