package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.overlay.BasicStringOverlayServiceTest;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionServiceTest;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
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
            // operations
            List<Operation> operations = List.of(
                    new ScaleByPercent(0.5),
                    new ScaleByPercent(0.4));
            // options
            Map<String,String> options = Map.of("key", "value");
            // page index
            int pageIndex = 3;
            // scale constraint
            ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);

            OperationList opList = instance
                    .withIdentifier(identifier)
                    .withOperations(operations.toArray(Operation[]::new))
                    .withOptions(options)
                    .withPageIndex(3)
                    .withScaleConstraint(scaleConstraint)
                    .build();
            assertEquals(identifier, opList.getIdentifier());
            assertEquals(operations, opList.getOperations());
            assertEquals(options, opList.getOptions());
            assertEquals(pageIndex, opList.getPageIndex());
            assertEquals(scaleConstraint, opList.getScaleConstraint());
        }

    }

    private static final double DELTA = 0.00000001;

    private OperationList instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        var config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        instance = new OperationList();
    }

    @Test
    void noOpConstructor() {
        assertNotNull(instance.getOptions());
        assertFalse(instance.getScaleConstraint().hasEffect());
    }

    @Test
    void constructor2() {
        instance = new OperationList(new Identifier("cats"));
        assertEquals("cats", instance.getIdentifier().toString());
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
    void applyNonEndpointMutationsWithScaleConstraintAndNoScaleOperationAddsOne()
            throws Exception {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder()
                .withSize(fullSize)
                .build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(0, 0, 70, 30),
                        new Encode(Format.get("jpg")))
                .withScaleConstraint(new ScaleConstraint(1, 2))
                .build();

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Scale expectedScale = new ScaleByPercent();
        Scale actualScale = (Scale) opList.getFirst(Scale.class);
        assertEquals(expectedScale, actualScale);
    }

    @Test
    void applyNonEndpointMutationsWithOrientationMutatesCrop()
            throws Exception {
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

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Crop expectedCrop = new CropByPixels(0, 0, 70, 30);
        expectedCrop.setOrientation(Orientation.ROTATE_90);
        Crop actualCrop = (Crop) opList.getFirst(Crop.class);
        assertEquals(expectedCrop, actualCrop);
    }

    @Test
    void applyNonEndpointMutationsWithOrientationMutatesRotate()
            throws Exception {
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

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Rotate expectedRotate = new Rotate(45);
        expectedRotate.addDegrees(Orientation.ROTATE_90.getDegrees());
        Rotate actualRotate = (Rotate) opList.getFirst(Rotate.class);
        assertEquals(expectedRotate, actualRotate);
    }

    @Test
    void applyNonEndpointMutationsWithBackgroundColor() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_BACKGROUND_COLOR, "white");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Rotate(45), new Encode(Format.get("jpg")))
                .build();

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(Color.fromString("#FFFFFF"), encode.getBackgroundColor());
    }

    @Test
    void applyNonEndpointMutationsWithJPEGOutputFormat() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_JPG_QUALITY, 50);
        config.setProperty(Key.PROCESSOR_JPG_PROGRESSIVE, true);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Encode);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(50, encode.getQuality());
        assertTrue(encode.isInterlacing());
    }

    @Test
    void applyNonEndpointMutationsWithOverlay() throws Exception {
        BasicStringOverlayServiceTest.setUpConfiguration();

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Overlay overlay = (Overlay) opList.getFirst(Overlay.class);
        assertEquals(10, overlay.getInset());
    }

    @Test
    void applyNonEndpointMutationsWithRedactions() throws Exception {
        RedactionServiceTest.setUpConfiguration();

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("redacted"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Redaction redaction = (Redaction) opList.getFirst(Redaction.class);
        assertEquals(new Rectangle(0, 10, 50, 70), redaction.getRegion());
    }

    @Test
    void applyNonEndpointMutationsWithMaxScale() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 1);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new ScaleByPercent(1.5),
                        new Encode(Format.get("jpg")))
                .build();
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertEquals(1.0, ((Scale) it.next()).getMaxScale(), DELTA);
    }

    @Test
    void applyNonEndpointMutationsWithDownscaleFilter() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_DOWNSCALE_FILTER, "bicubic");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new ScaleByPercent(0.5),
                        new Encode(Format.get("jpg")))
                .build();
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());
    }

    @Test
    void applyNonEndpointMutationsWithUpscaleFilter() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_UPSCALE_FILTER, "triangle");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new ScaleByPercent(1.5),
                        new Encode(Format.get("jpg")))
                .build();
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.TRIANGLE, ((Scale) it.next()).getFilter());
    }

    @Test
    void applyNonEndpointMutationsWithSharpening() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SHARPEN, 0.2f);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Sharpen);

        Sharpen sharpen = (Sharpen) opList.getFirst(Sharpen.class);
        assertEquals(0.2, sharpen.getAmount(), DELTA);
    }

    @Test
    void applyNonEndpointMutationsWithTIFFOutputFormat() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_TIF_COMPRESSION, "LZW");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Encode);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(Compression.LZW, encode.getCompression());
    }

    @Test
    void applyNonEndpointMutationsWithMetadata() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

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

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        assertEquals("<rdf:RDF>derivative metadata</rdf:RDF>",
                encode.getMetadata().getXMP().orElseThrow());
    }

    @Test
    void applyNonEndpointMutationsWhileFrozen() throws Exception {
        final Dimension fullSize   = new Dimension(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withOperations(new CropByPixels(0, 0, 70, 30))
                .build();

        opList.freeze();

        final RequestContext context = new RequestContext();
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy          = service.newDelegateProxy(context);

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
    void equalsWithEqualOperationList() {
        OperationList ops1 = OperationList.builder()
                .withOperations(new Rotate(1)).build();
        OperationList ops2 = OperationList.builder()
                .withOperations(new Rotate(1)).build();
        assertEquals(ops1, ops2);
    }

    @Test
    void equalsWithUnequalOperationList() {
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
    void hasEffectWithScaleConstraint() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("gif")))
                .build();
        Dimension fullSize = new Dimension(100, 100);
        assertFalse(instance.hasEffect(fullSize, Format.get("gif")));
        instance.setScaleConstraint(new ScaleConstraint(1, 2));
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
    void setScaleConstraint() {
        instance.setScaleConstraint(new ScaleConstraint(1, 3));
        assertEquals(1, instance.getScaleConstraint().getRational().getNumerator());
        assertEquals(3, instance.getScaleConstraint().getRational().getDenominator());

        instance.setScaleConstraint(null);
        assertEquals(1, instance.getScaleConstraint().getRational().getNumerator());
        assertEquals(1, instance.getScaleConstraint().getRational().getDenominator());
    }

    @Test
    void setScaleConstraintWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setScaleConstraint(new ScaleConstraint(1, 2)));
    }

    @Test
    void toFilename() {
        instance = new OperationList(new Identifier("identifier.jpg"));
        instance.setPageIndex(3);
        CropByPixels crop = new CropByPixels(5, 6, 20, 22);
        instance.add(crop);
        Scale scale = new ScaleByPercent(0.4);
        instance.add(scale);
        instance.add(new Rotate(15));
        instance.add(ColorTransform.BITONAL);
        instance.add(new Encode(Format.get("jpg")));
        instance.getOptions().put("animal", "cat");
        instance.setScaleConstraint(new ScaleConstraint(1, 2));

        String expected = "50c63748527e634134449ae20b199cc0_08592737b5ff7370bc0a70517fcb0b23.jpg";
        assertEquals(expected, instance.toFilename());

        // Assert that changing an operation changes the filename
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
        instance = new OperationList(new Identifier("identifier.jpg"));
        // page index
        instance.setPageIndex(3);
        // crop
        Crop crop = new CropByPixels(2, 4, 50, 50);
        instance.add(crop);
        // no-op scale
        Scale scale = new ScaleByPercent();
        instance.add(scale);
        // rotate
        instance.add(new Rotate(0));
        // transpose
        instance.add(Transpose.HORIZONTAL);
        // encode
        instance.add(new Encode(Format.get("jpg")));
        instance.setScaleConstraint(new ScaleConstraint(1, 2));

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
        instance = new OperationList(new Identifier("identifier.jpg"));
        Crop crop = new CropByPixels(5, 6, 20, 22);
        instance.add(crop);
        Scale scale = new ScaleByPercent(0.4);
        instance.add(scale);
        instance.add(new Rotate(15));
        instance.add(ColorTransform.BITONAL);
        instance.add(new Encode(Format.get("jpg")));
        instance.getOptions().put("animal", "cat");
        instance.setScaleConstraint(new ScaleConstraint(1, 2));

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
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new ScaleByPercent(4), new Encode(Format.get("jpg")))
                .withScaleConstraint(new ScaleConstraint(1, 8))
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
