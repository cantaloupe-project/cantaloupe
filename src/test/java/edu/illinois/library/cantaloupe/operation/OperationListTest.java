package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.overlay.BasicStringOverlayServiceTest;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionServiceTest;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class OperationListTest extends BaseTest {

    private OperationList instance;

    private static OperationList newOperationList() {
        OperationList ops = new OperationList();

        Crop crop = new Crop();
        crop.setFull(true);
        ops.add(crop);

        Scale scale = new Scale();
        ops.add(scale);
        ops.add(new Rotate(0));

        return ops;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        instance = newOperationList();
        assertNotNull(instance.getOptions());
    }

    @Test
    public void add() {
        instance = new OperationList();
        assertFalse(instance.iterator().hasNext());

        instance.add(new Rotate());
        assertTrue(instance.iterator().hasNext());
    }

    @Test(expected = IllegalStateException.class)
    public void addWhileFrozen() {
        instance = new OperationList();
        instance.freeze();

        instance.add(new Rotate());
    }

    @Test
    public void addAfterWithExistingClass() {
        instance = new OperationList(new Rotate());
        instance.addAfter(new Scale(), Rotate.class);
        Iterator<Operation> it = instance.iterator();

        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test
    public void addAfterWithExistingSuperclass() {
        instance = new OperationList();
        instance.add(new MockOverlay());

        class SubMockOverlay extends MockOverlay {}

        instance.addAfter(new SubMockOverlay(), Overlay.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof MockOverlay);
        assertTrue(it.next() instanceof SubMockOverlay);
    }

    @Test
    public void addAfterWithoutExistingClass() {
        instance = new OperationList();
        instance.add(new Rotate());
        instance.addAfter(new Scale(), Crop.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test(expected = IllegalStateException.class)
    public void addAfterWhileFrozen() {
        instance = new OperationList();
        instance.freeze();
        instance.addAfter(new Rotate(), Crop.class);
    }

    @Test
    public void addBeforeWithExistingClass() {
        instance = new OperationList();
        instance.add(new Rotate());
        instance.addBefore(new Scale(), Rotate.class);
        assertTrue(instance.iterator().next() instanceof Scale);
    }

    @Test
    public void addBeforeWithExistingSuperclass() {
        class SubMockOverlay extends MockOverlay {}

        instance = new OperationList();
        instance.add(new MockOverlay());
        instance.addBefore(new SubMockOverlay(), MockOverlay.class);
        assertTrue(instance.iterator().next() instanceof SubMockOverlay);
    }

    @Test
    public void addBeforeWithoutExistingClass() {
        instance = new OperationList();
        instance.add(new Rotate());
        instance.addBefore(new Scale(), Crop.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test(expected = IllegalStateException.class)
    public void addBeforeWhileFrozen() {
        instance = new OperationList();
        instance.freeze();
        instance.addBefore(new Rotate(), Crop.class);
    }

    @Test
    public void applyNonEndpointMutationsWithOrientationMutatesCrop()
            throws Exception {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder()
                .withSize(fullSize)
                .withOrientation(Orientation.ROTATE_90)
                .build();
        final OperationList opList = new OperationList(
                new Identifier("cats"),
                new Crop(0, 0, 70, 30),
                new Encode(Format.JPG));

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Crop expectedCrop = new Crop(0, 0, 70, 30);
        expectedCrop.applyOrientation(Orientation.ROTATE_90, info.getSize());
        Crop actualCrop = (Crop) opList.getFirst(Crop.class);
        assertEquals(expectedCrop, actualCrop);
    }

    @Test
    public void applyNonEndpointMutationsWithOrientationMutatesRotate()
            throws Exception {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder()
                .withSize(fullSize)
                .withOrientation(Orientation.ROTATE_90)
                .build();
        final OperationList opList = new OperationList(
                new Identifier("cats"),
                new Crop(0, 0, 70, 30),
                new Rotate(45),
                new Encode(Format.JPG));

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
    public void applyNonEndpointMutationsWithBackgroundColor()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_BACKGROUND_COLOR, "white");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"),
                new Rotate(45),
                new Encode(Format.JPG));

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(Color.fromString("#FFFFFF"), encode.getBackgroundColor());
    }

    @Test
    public void applyNonEndpointMutationsWithJPEGOutputFormat()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_JPG_QUALITY, 50);
        config.setProperty(Key.PROCESSOR_JPG_PROGRESSIVE, true);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));

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
    public void applyNonEndpointMutationsWithMetadataCopies()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof MetadataCopy);
    }

    @Test
    public void applyNonEndpointMutationsWithNormalization() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_NORMALIZE, true);

        // Assert that Normalizations are inserted before Crops.
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        OperationList opList = new OperationList(
                new Identifier("cats"),
                new Crop(),
                new Encode(Format.JPG));
        RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);

        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Normalize);

        // Assert that Normalizations are inserted before upscales when no
        // Crop is present.
        opList = new OperationList(
                new Identifier("cats"),
                new Scale(1.5f),
                new Encode(Format.JPG));
        context = new RequestContext();
        context.setOperationList(opList, fullSize);
        proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(
                Info.builder().withSize(2000, 1000).build(),
                proxy);

        it = opList.iterator();
        assertTrue(it.next() instanceof Normalize);

        // Assert that Normalizations are inserted after downscales when no
        // Crop is present.
        opList = new OperationList(
                new Identifier("cats"),
                new Scale(0.5f),
                new Encode(Format.JPG));
        context = new RequestContext();
        context.setOperationList(opList, fullSize);
        proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        it = opList.iterator();
        assertTrue(it.next() instanceof Scale);
        assertTrue(it.next() instanceof Normalize);
    }

    @Test
    public void applyNonEndpointMutationsWithOverlay() throws Exception {
        BasicStringOverlayServiceTest.setUpConfiguration();

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"), new Encode(Format.TIF));

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Overlay overlay = (Overlay) opList.getFirst(Overlay.class);
        assertEquals(10, overlay.getInset());
    }

    @Test
    public void applyNonEndpointMutationsWithRedactions() throws Exception {
        RedactionServiceTest.setUpConfiguration();

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Redaction redaction = (Redaction) opList.getFirst(Redaction.class);
        assertEquals(new Rectangle(0, 10, 50, 70), redaction.getRegion());
    }

    @Test
    public void applyNonEndpointMutationsWithDownscaleFilter() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_DOWNSCALE_FILTER, "bicubic");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"),
                new Scale(0.5f),
                new Encode(Format.JPG));
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());
    }

    @Test
    public void applyNonEndpointMutationsWithUpscaleFilter() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_UPSCALE_FILTER, "triangle");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"),
                new Scale(1.5f),
                new Encode(Format.JPG));
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.TRIANGLE, ((Scale) it.next()).getFilter());
    }

    @Test
    public void applyNonEndpointMutationsWithSharpening() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SHARPEN, 0.2f);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"),
                new Encode(Format.TIF));
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Sharpen);

        Sharpen sharpen = (Sharpen) opList.getFirst(Sharpen.class);
        assertEquals(0.2f, sharpen.getAmount(), 0.00001f);
    }

    @Test
    public void applyNonEndpointMutationsWithTIFFOutputFormat()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_TIF_COMPRESSION, "LZW");

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(
                new Identifier("cats"), new Encode(Format.TIF));
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

    @Test(expected = IllegalStateException.class)
    public void applyNonEndpointMutationsWhileFrozen() throws Exception {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = Info.builder().withSize(fullSize).build();
        final OperationList opList = new OperationList(new Crop(0, 0, 70, 30));

        opList.freeze();

        final RequestContext context = new RequestContext();
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);
    }

    @Test
    public void clear() {
        int opCount = 0;
        Iterator<Operation> it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(3, opCount);
        instance.clear();

        opCount = 0;
        it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(0, opCount);
    }

    @Test(expected = IllegalStateException.class)
    public void clearWhileFrozen() {
        instance.freeze();
        instance.clear();
    }

    @Test
    public void compareTo() {
        OperationList ops2 = new OperationList();

        Crop crop = new Crop();
        crop.setFull(true);
        ops2.add(crop);

        Scale scale = new Scale();
        ops2.add(scale);
        ops2.add(new Rotate(0));

        assertEquals(0, ops2.compareTo(this.instance));
    }

    @Test
    public void equalsWithEqualOperationList() {
        OperationList ops1 = new OperationList(new Rotate(1));
        OperationList ops2 = new OperationList(new Rotate(1));
        assertTrue(ops1.equals(ops2));
    }

    @Test
    public void equalsWithUnequalOperationList() {
        OperationList ops1 = new OperationList();
        OperationList ops2 = new OperationList(new Rotate(1));
        assertFalse(ops1.equals(ops2));
    }

    @Test(expected = IllegalStateException.class)
    public void freezeFreezesOperations() {
        instance.freeze();
        ((Crop) instance.getFirst(Crop.class)).setHeight(300);
    }

    @Test
    public void getFirst() {
        assertNull(instance.getFirst(MetadataCopy.class));
        assertNotNull(instance.getFirst(Scale.class));
    }

    @Test
    public void getFirstWithSuperclass() {
        instance.add(new MockOverlay());

        Overlay overlay = (Overlay) instance.getFirst(Overlay.class);
        assertNotNull(overlay);
        assertTrue(overlay instanceof MockOverlay);
    }

    @Test
    public void getOptions() {
        assertNotNull(instance.getOptions());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getOptionsWhenFrozen() {
        instance.freeze();
        instance.getOptions().put("test", "test");
    }

    @Test
    public void getResultingSize() {
        Dimension fullSize = new Dimension(300, 200);
        instance = new OperationList();
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        Rotate rotate = new Rotate();
        instance.add(crop);
        instance.add(scale);
        instance.add(rotate);
        assertEquals(fullSize, instance.getResultingSize(fullSize));

        instance = new OperationList();
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        scale = new Scale(0.5f);
        instance.add(crop);
        instance.add(scale);
        assertEquals(new Dimension(75, 50), instance.getResultingSize(fullSize));
    }

    @Test
    public void hasEffectWithSameFormat() {
        instance = new OperationList(new Encode(Format.GIF));
        assertFalse(instance.hasEffect(new Dimension(100, 100), Format.GIF));
    }

    @Test
    public void hasEffectWithDifferentFormats() {
        instance = new OperationList(new Encode(Format.GIF));
        assertTrue(instance.hasEffect(new Dimension(100, 100), Format.JPG));
    }

    @Test
    public void hasEffectWithPDFSourceAndPDFOutputAndOverlay() {
        instance = new OperationList(new Encode(Format.PDF));
        assertFalse(instance.hasEffect(new Dimension(100, 100), Format.PDF));
    }

    @Test
    public void hasEffectWithEncodeAndSameOutputFormat() {
        instance = new OperationList(new Encode(Format.JPG));
        assertFalse(instance.hasEffect(new Dimension(100, 100), Format.JPG));
    }

    @Test
    public void iterator() {
        int count = 0;
        Iterator<Operation> it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void iteratorCannotRemoveWhileFrozen() {
        instance.freeze();
        Iterator<Operation> it = instance.iterator();
        it.next();
        it.remove();
    }

    @Test(expected = IllegalStateException.class)
    public void setIdentifierWhileFrozen() {
        instance.freeze();
        instance.setIdentifier(new Identifier("alpaca"));
    }

    @Test
    public void toFilename() {
        instance = new OperationList(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setX(5f);
        crop.setY(6f);
        crop.setWidth(20f);
        crop.setHeight(22f);
        instance.add(crop);
        Scale scale = new Scale(0.4f);
        instance.add(scale);
        instance.add(new Rotate(15));
        instance.add(ColorTransform.BITONAL);
        instance.add(new Encode(Format.JPG));
        instance.getOptions().put("animal", "cat");

        String expected = "50c63748527e634134449ae20b199cc0_caea6c57cfd6acccd67252c327666517.jpg";
        assertEquals(expected, instance.toFilename());

        // Assert that changing an operation changes the filename
        crop.setX(10f);
        assertNotEquals(expected, instance.toFilename());

        // Assert that changing an option changes the filename
        crop.setX(5f);
        assertEquals(expected, instance.toFilename());
        instance.getOptions().put("animal", "dog");
        assertNotEquals(expected, instance.toFilename());
    }

    @Test
    public void toMap() {
        instance = new OperationList(new Identifier("identifier.jpg"));
        // crop
        Crop crop = new Crop();
        crop.setX(2);
        crop.setY(4);
        crop.setWidth(50);
        crop.setHeight(50);
        instance.add(crop);
        // no-op scale
        Scale scale = new Scale();
        instance.add(scale);
        instance.add(new Rotate(0));
        // transpose
        instance.add(Transpose.HORIZONTAL);
        // encode
        instance.add(new Encode(Format.JPG));

        final Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals("identifier.jpg", map.get("identifier"));
        assertEquals(3, ((List<?>) map.get("operations")).size());
        assertEquals(0, ((Map<?, ?>) map.get("options")).size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        map.put("test", "test");
    }

    @Test
    public void testToString() {
        instance = new OperationList(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setX(5f);
        crop.setY(6f);
        crop.setWidth(20f);
        crop.setHeight(22f);
        instance.add(crop);
        Scale scale = new Scale(0.4f);
        instance.add(scale);
        instance.add(new Rotate(15));
        instance.add(ColorTransform.BITONAL);
        instance.add(new Encode(Format.JPG));
        instance.getOptions().put("animal", "cat");

        String expected = "identifier.jpg_crop:5,6,20,22_scale:40%_rotate:15_colortransform:bitonal_encode:jpg_UNDEFINED_8_animal:cat";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void validateWithValidInstance() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList(
                new Identifier("cats"),
                new Crop(0, 0, 100, 100),
                new Encode(Format.JPG));
        ops.validate(fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingIdentifier() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList(
                new Crop(0, 0, 100, 100), new Encode(Format.JPG));
        ops.validate(fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithMissingEncodeOperation() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList(
                new Identifier("cats"), new Crop(0, 0, 100, 100));
        ops.validate(fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithOutOfBoundsCrop() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList(new Crop(1001, 1001, 100, 100),
                new Encode(Format.JPG));
        ops.validate(fullSize);
    }

    @Test
    public void validateWithValidPageArgument() {
        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        ops.getOptions().put("page", "2");
        ops.validate(new Dimension(100, 88));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithZeroPageArgument() {
        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        ops.getOptions().put("page", "0");
        ops.validate(new Dimension(100, 88));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithNegativePageArgument() {
        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        ops.getOptions().put("page", "-1");
        ops.validate(new Dimension(100, 88));
    }

}
