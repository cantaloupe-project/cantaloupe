package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.overlay.BasicStringOverlayServiceTest;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OperationListTest extends BaseTest {

    @Nested
    class BuilderTest extends BaseTest {

        private OperationList.Builder instance;

        @BeforeEach
        public void setUp() throws Exception {
            super.setUp();
            instance = OperationList.builder();
        }

        @Test
        void testBuildWithNoPropertiesSet() {
            OperationList opList = instance.build();
            assertNull(opList.getIdentifier());
            assertTrue(opList.getOperations().isEmpty());
            assertTrue(opList.getOptions().isEmpty());
            assertEquals(0, opList.getPageIndex());
            assertEquals(new ScaleConstraint(1, 1), opList.getScaleConstraint());
        }

        @Test
        void testBuildWithAllPropertiesSet() {
            // identifier
            Identifier identifier = new Identifier("cats");
            // meta-identifier
            MetaIdentifier metaIdentifier = new MetaIdentifier(identifier);
            // operations
            List<Operation> operations = List.of(
                    new ScaleByPercent(0.5),
                    new ScaleByPercent(0.4));
            // options
            Map<String,String> options = Map.of("key", "value");
            // page index
            int pageIndex = 3;

            OperationList opList = instance
                    .withIdentifier(identifier)
                    .withMetaIdentifier(metaIdentifier)
                    .withOperations(operations.toArray(Operation[]::new))
                    .withOptions(options)
                    .withPageIndex(3)
                    .build();
            assertEquals(identifier, opList.getIdentifier());
            assertEquals(metaIdentifier, opList.getMetaIdentifier());
            assertEquals(operations, opList.getOperations());
            assertEquals(options, opList.getOptions());
            assertEquals(pageIndex, opList.getPageIndex());
        }

    }

    private static final double DELTA = 0.00000001;

    private OperationList instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new OperationList();
    }

    @Test
    void noOpConstructor() {
        assertNotNull(instance.getOptions());
        assertFalse(instance.getScaleConstraint().hasEffect());
    }

    @Test
    void identifierConstructor() {
        instance = new OperationList(new Identifier("cats"));
        assertEquals("cats", instance.getIdentifier().toString());
    }

    @Test
    void metaIdentifierConstructor() {
        instance = new OperationList(new MetaIdentifier("cats"));
        assertEquals("cats", instance.getMetaIdentifier().toString());
    }

    @Test
    void add() {
        assertFalse(instance.iterator().hasNext());

        instance.add(new Rotate());
        assertTrue(instance.iterator().hasNext());
    }

    @Test
    void addWithNullArgument() {
        instance.add(null);
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void addWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.add(new Rotate()));
    }

    @Test
    void addAfterWithExistingClass() {
        instance = OperationList.builder()
                .withOperations(new Rotate())
                .build();
        instance.addAfter(new ScaleByPercent(), Rotate.class);
        Iterator<Operation> it = instance.iterator();

        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test
    void addAfterWithExistingSuperclass() {
        instance.add(new MockOverlay());

        class SubMockOverlay extends MockOverlay {}

        instance.addAfter(new SubMockOverlay(), Overlay.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof MockOverlay);
        assertTrue(it.next() instanceof SubMockOverlay);
    }

    @Test
    void addAfterWithoutExistingClass() {
        instance.add(new Rotate());
        instance.addAfter(new ScaleByPercent(), Crop.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test
    void addAfterWithNullArgument() {
        instance.addAfter(null, Scale.class);
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void addAfterWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.addAfter(new Rotate(), Crop.class));
    }

    @Test
    void addBeforeWithExistingClass() {
        instance.add(new Rotate());
        instance.addBefore(new ScaleByPercent(), Rotate.class);
        assertTrue(instance.iterator().next() instanceof Scale);
    }

    @Test
    void addBeforeWithExistingSuperclass() {
        class SubMockOverlay extends MockOverlay {}

        instance.add(new MockOverlay());
        instance.addBefore(new SubMockOverlay(), MockOverlay.class);
        assertTrue(instance.iterator().next() instanceof SubMockOverlay);
    }

    @Test
    void addBeforeWithoutExistingClass() {
        instance.add(new Rotate());
        instance.addBefore(new ScaleByPercent(), Crop.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test
    void addBeforeWithNullArgument() {
        instance.addBefore(null, Scale.class);
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void addBeforeWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.addBefore(new Rotate(), Crop.class));
    }

    @Test
    void applyNonEndpointMutationsWithScaleConstraintAndNoScaleOperationAddsOne() {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder()
                .withSize(fullSize)
                .build();
        final Identifier identifier = new Identifier("cats");
        final OperationList opList = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new CropByPixels(0, 0, 70, 30),
                        new Encode(Format.get("jpg")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Scale expectedScale = new ScaleByPercent();
        Scale actualScale = (Scale) opList.getFirst(Scale.class);
        assertEquals(expectedScale, actualScale);
    }

    @Test
    void applyNonEndpointMutationsWithOrientationMutatesCrop() {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder()
                .withSize(fullSize)
                .withMetadata(new Metadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(0, 0, 70, 30),
                        new Encode(Format.get("jpg")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Crop expectedCrop = new CropByPixels(0, 0, 70, 30);
        expectedCrop.setOrientation(Orientation.ROTATE_90);
        Crop actualCrop = (Crop) opList.getFirst(Crop.class);
        assertEquals(expectedCrop, actualCrop);
    }

    @Test
    void applyNonEndpointMutationsWithOrientationMutatesRotate() {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder()
                .withSize(fullSize)
                .withMetadata(new Metadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(0, 0, 70, 30),
                        new Rotate(45),
                        new Encode(Format.get("jpg")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Rotate expectedRotate = new Rotate(45);
        expectedRotate.addDegrees(Orientation.ROTATE_90.getDegrees());
        Rotate actualRotate = (Rotate) opList.getFirst(Rotate.class);
        assertEquals(expectedRotate, actualRotate);
    }

    @Test
    void applyNonEndpointMutationsWithBackgroundColor() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_BACKGROUND_COLOR, "white");

        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Rotate(45), new Encode(Format.get("jpg")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(Color.fromString("#FFFFFF"), encode.getBackgroundColor());
    }

    @Test
    void applyNonEndpointMutationsWithJPEGOutputFormat() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_JPG_QUALITY, 50);
        config.setProperty(Key.PROCESSOR_JPG_PROGRESSIVE, true);

        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Encode);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(50, encode.getQuality());
        assertTrue(encode.isInterlacing());
    }

    @Test
    void applyNonEndpointMutationsWithOverlay() {
        BasicStringOverlayServiceTest.setUpConfiguration();

        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Overlay overlay = (Overlay) opList.getFirst(Overlay.class);
        assertEquals(10, overlay.getInset());
    }

    @Test
    void applyNonEndpointMutationsWithRedactions() {
        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("redacted"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Redaction redaction = (Redaction) opList.getFirst(Redaction.class);
        assertEquals(new Rectangle(0, 10, 50, 70), redaction.getRegion());
    }

    @Test
    void applyNonEndpointMutationsWithDownscaleFilter() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_DOWNSCALE_FILTER, "bicubic");

        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new ScaleByPercent(0.5),
                        new Encode(Format.get("jpg")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());
    }

    @Test
    void applyNonEndpointMutationsWithUpscaleFilter() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_UPSCALE_FILTER, "triangle");

        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new ScaleByPercent(1.5),
                        new Encode(Format.get("jpg")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.TRIANGLE, ((Scale) it.next()).getFilter());
    }

    @Test
    void applyNonEndpointMutationsWithSharpening() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SHARPEN, 0.2f);

        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Sharpen);

        Sharpen sharpen = (Sharpen) opList.getFirst(Sharpen.class);
        assertEquals(0.2, sharpen.getAmount(), DELTA);
    }

    @Test
    void applyNonEndpointMutationsWithTIFFOutputFormat() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_TIF_COMPRESSION, "LZW");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Encode);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(Compression.LZW, encode.getCompression());
    }

    @Test
    void applyNonEndpointMutationsWithMetadata() {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final Encode encode = new Encode(Format.get("jpg"));
        final Metadata metadata = new Metadata();
        metadata.setXMP("<rdf:RDF>source metadata</rdf:RDF>");
        encode.setMetadata(metadata);
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("metadata"))
                .withOperations(encode)
                .build();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setOperationList(opList, fullSize);

        opList.applyNonEndpointMutations(info, proxy);

        assertEquals("<rdf:RDF>derivative metadata</rdf:RDF>",
                encode.getMetadata().getXMP().orElseThrow());
    }

    @Test
    void applyNonEndpointMutationsWhileFrozen() {
        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withOperations(new CropByPixels(0, 0, 70, 30))
                .build();

        opList.freeze();
        DelegateProxy proxy = TestUtil.newDelegateProxy();

        assertThrows(IllegalStateException.class,
                () -> opList.applyNonEndpointMutations(info, proxy));
    }

    @Test
    void clear() {
        instance.add(new CropByPixels(10, 10, 10, 10));
        instance.add(new ScaleByPercent(0.5));

        int opCount = 0;
        Iterator<Operation> it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(2, opCount);
        instance.clear();

        opCount = 0;
        it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(0, opCount);
    }

    @Test
    void clearWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.clear());
    }

    @Test
    void equalsWithEqualInstance() {
        OperationList ops1 = OperationList.builder()
                .withOperations(new Rotate(1)).build();
        OperationList ops2 = OperationList.builder()
                .withOperations(new Rotate(1)).build();
        assertEquals(ops1, ops2);
    }

    @Test
    void equalsWithUnequalInstance() {
        OperationList ops1 = OperationList.builder()
                .withOperations(new Rotate(1)).build();
        OperationList ops2 = OperationList.builder()
                .withOperations(new Rotate(2)).build();
        assertNotEquals(ops1, ops2);
    }

    @Test
    void freezeFreezesOperations() {
        instance.add(new CropByPixels(0, 0, 10, 10));
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> ((CropByPixels) instance.getFirst(CropByPixels.class)).setHeight(300));
    }

    @Test
    void getFirst() {
        instance.add(new ScaleByPercent(0.5));

        assertNull(instance.getFirst(Crop.class));
        assertNotNull(instance.getFirst(Scale.class));
    }

    @Test
    void getFirstWithSuperclass() {
        instance.add(new MockOverlay());

        Overlay overlay = (Overlay) instance.getFirst(Overlay.class);
        assertNotNull(overlay);
        assertTrue(overlay instanceof MockOverlay);
    }

    @Test
    void getIdentifierReturnsIdentifierIfSet() {
        final Identifier identifier = new Identifier("cats");
        instance.setIdentifier(identifier);
        instance.setMetaIdentifier(null);
        assertEquals(identifier, instance.getIdentifier());
    }

    @Test
    void getIdentifierFallsBacktoMetaIdentifierIdentifier() {
        instance.setIdentifier(null);
        instance.setMetaIdentifier(new MetaIdentifier("cats"));
        assertEquals(new Identifier("cats"), instance.getIdentifier());
    }

    @Test
    void getOptions() {
        assertNotNull(instance.getOptions());
    }

    @Test
    void getOptionsWhenFrozen() {
        instance.freeze();
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getOptions().put("test", "test"));
    }

    @Test
    void getOutputFormatReturnsEncodeFormatWhenPresent() {
        Format format = Format.get("jpg");
        instance.add(new Encode(format));
        assertEquals(format, instance.getOutputFormat());
    }

    @Test
    void getOutputFormatReturnsNullWhenEncodeNotPresent() {
        assertNull(instance.getOutputFormat());
    }

    @Test
    void getPageIndexDefaultsToZero() {
        assertEquals(0, instance.getPageIndex());
    }

    @Test
    void getResultingSize() {
        Dimension fullSize   = new Dimension(300, 200);
        ScaleByPercent scale = new ScaleByPercent();
        Rotate rotate        = new Rotate();
        instance.add(scale);
        instance.add(rotate);
        assertEquals(fullSize, instance.getResultingSize(fullSize));

        instance  = new OperationList();
        Crop crop = new CropByPercent(0, 0, 0.5, 0.5);
        scale     = new ScaleByPercent(0.5);
        instance.add(crop);
        instance.add(scale);
        assertEquals(new Dimension(75, 50), instance.getResultingSize(fullSize));
    }

    @Test
    void getScaleConstraintReturnsScaleConstraintWhenSet() {
        instance.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(1, 2)
                .build());
        assertEquals(new ScaleConstraint(1, 2), instance.getScaleConstraint());
    }

    @Test
    void getScaleConstraintDefaultsToOne() {
        assertEquals(new ScaleConstraint(1, 1), instance.getScaleConstraint());
    }

    @Test
    void hasEffectWithScaleConstraint() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("gif")))
                .build();
        Dimension fullSize = new Dimension(100, 100);
        assertFalse(instance.hasEffect(fullSize, Format.get("gif")));
        instance.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(1, 2)
                .build());
        assertTrue(instance.hasEffect(fullSize, Format.get("gif")));
    }

    @Test
    void hasEffectWithSameFormat() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("gif")))
                .build();
        assertFalse(instance.hasEffect(new Dimension(100, 100), Format.get("gif")));
    }

    @Test
    void hasEffectWithDifferentFormats() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("gif")))
                .build();
        assertTrue(instance.hasEffect(new Dimension(100, 100), Format.get("jpg")));
    }

    @Test
    void hasEffectWithPDFSourceAndPDFOutputAndOverlay() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("pdf")))
                .build();
        assertFalse(instance.hasEffect(new Dimension(100, 100), Format.get("pdf")));
    }

    @Test
    void hasEffectWithEncodeAndSameOutputFormat() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        assertFalse(instance.hasEffect(new Dimension(100, 100), Format.get("jpg")));
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void iterator() {
        instance.add(new CropByPixels(10, 10, 10, 10));
        instance.add(new ScaleByPercent(0.5));

        int count = 0;
        Iterator<Operation> it = instance.iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void iteratorCannotRemoveWhileFrozen() {
        instance.add(new ScaleByPercent(50.5));
        instance.freeze();
        Iterator<Operation> it = instance.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void setIdentifierWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setIdentifier(new Identifier("alpaca")));
    }

    @Test
    void setMetaIdentifier() {
        MetaIdentifier metaIdentifier = new MetaIdentifier("cats");
        instance.setMetaIdentifier(metaIdentifier);
        assertEquals(metaIdentifier, instance.getMetaIdentifier());

        instance.setMetaIdentifier(null);
        assertNull(instance.getMetaIdentifier());
    }

    @Test
    void setMetaIdentifierWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setMetaIdentifier(new MetaIdentifier("cats")));
    }

    @Test
    void setPageIndexWithIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPageIndex(-1));
    }

    @Test
    void setPageIndexWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setPageIndex(3));
    }

    @Test
    void toFilename() {
        final Identifier identifier = new Identifier("identifier.jpg");
        instance = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new CropByPixels(5, 6, 20, 22),
                        new ScaleByPercent(0.4),
                        new Rotate(15),
                        ColorTransform.BITONAL,
                        new Encode(Format.get("jpg")))
                .withOptions(Map.of("animal", "cat"))
                .withPageIndex(3)
                .build();

        String expected = "50c63748527e634134449ae20b199cc0_08592737b5ff7370bc0a70517fcb0b23.jpg";
        assertEquals(expected, instance.toFilename());

        // Assert that changing an operation changes the filename
        CropByPixels crop = (CropByPixels) instance.getFirst(CropByPixels.class);
        crop.setX(12);
        assertNotEquals(expected, instance.toFilename());

        // Assert that changing an option changes the filename
        crop.setX(10);
        instance.getOptions().put("animal", "dog");
        assertNotEquals(expected, instance.toFilename());
    }

    @Test
    @SuppressWarnings("unchecked")
    void toMap() {
        final Identifier identifier = new Identifier("identifier.jpg");
        instance = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new CropByPixels(2, 4, 50, 50),
                        new ScaleByPercent(),
                        new Rotate(0),
                        Transpose.HORIZONTAL,
                        new Encode(Format.get("jpg")))
                .withPageIndex(3)
                .build();

        final Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals("identifier.jpg", map.get("identifier"));
        assertEquals(3, map.get("page_index"));
        assertEquals(4, ((List<?>) map.get("operations")).size());
        assertEquals(0, ((Map<?, ?>) map.get("options")).size());
        assertEquals(1, (long) ((Map<String,Long>) map.get("scale_constraint")).get("numerator"));
        assertEquals(2, (long) ((Map<String,Long>) map.get("scale_constraint")).get("denominator"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        final Identifier identifier = new Identifier("identifier.jpg");
        instance = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2).build())
                .withOperations(
                        new CropByPixels(5, 6, 20, 22),
                        new ScaleByPercent(0.4),
                        new Rotate(15),
                        ColorTransform.BITONAL,
                        new Encode(Format.get("jpg")))
                .withOptions(Map.of("animal", "cat"))
                .build();
        String expected = "identifier.jpg_1:2_cropbypixels:5,6,20,22_scalebypercent:40%_rotate:15_colortransform:bitonal_encode:jpg_UNDEFINED_8_animal:cat";
        assertEquals(expected, instance.toString());
    }

    @Test
    void validateWithValidInstance() throws Exception {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(0, 0, 100, 100),
                        new Encode(Format.get("jpg")))
                .build();
        ops.validate(fullSize, Format.get("png"));
    }

    @Test
    void validateWithMissingIdentifier() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = OperationList.builder()
                .withOperations(
                        new CropByPixels(0, 0, 100, 100),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(ValidationException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithMissingEncodeOperation() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new CropByPixels(0, 0, 100, 100))
                .build();
        assertThrows(ValidationException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithOutOfBoundsCrop() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = OperationList.builder()
                .withOperations(
                        new CropByPixels(1001, 1001, 100, 100),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(ValidationException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithZeroResultingArea() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(0, 0, 10, 10),
                        new ScaleByPercent(0.0001),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(ValidationException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithScaleGreaterThanMaxAllowed() {
        Dimension fullSize = new Dimension(1000, 1000);
        Identifier identifier = new Identifier("cats");
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 8)
                        .build())
                .withOperations(
                        new ScaleByPercent(4),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(IllegalScaleException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithScaleGreaterThanMaxAllowedBy1Pixel() throws Exception {
        Dimension fullSize = new Dimension(639, 343);
        Identifier identifier = new Identifier("cats");
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new ScaleByPixels(320, 172, ScaleByPixels.Mode.NON_ASPECT_FILL),
                        new Encode(Format.get("png")))
                .build();
        ops.validate(fullSize, Format.get("png"));
    }

    @Test
    void validateWithScaleGreaterThanMaxAllowedBy2Pixels() {
        Dimension fullSize = new Dimension(639, 343);
        Identifier identifier = new Identifier("cats");
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new ScaleByPixels(321, 173, ScaleByPixels.Mode.NON_ASPECT_FILL),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(IllegalScaleException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithAreaGreaterThanMaxAllowed() {
        Configuration.getInstance().setProperty(Key.MAX_PIXELS, 100);
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        assertThrows(IllegalSizeException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

}
