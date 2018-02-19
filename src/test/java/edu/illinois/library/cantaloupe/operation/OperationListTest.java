package edu.illinois.library.cantaloupe.operation;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
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

public class OperationListTest extends BaseTest {

    private OperationList instance;

    private static OperationList newOperationList() {
        OperationList ops = new OperationList(
                new Identifier("identifier.jpg"), Format.JPG);

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
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        assertFalse(instance.iterator().hasNext());
        instance.add(new Rotate());
        assertTrue(instance.iterator().hasNext());
    }

    @Test(expected = IllegalStateException.class)
    public void addWhileFrozen() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.freeze();

        instance.add(new Rotate());
    }

    @Test
    public void addAfterWithExistingClass() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.add(new Rotate());
        instance.addAfter(new Scale(), Rotate.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test
    public void addAfterWithExistingSuperclass() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.add(new MockOverlay());

        class SubMockOverlay extends MockOverlay {}

        instance.addAfter(new SubMockOverlay(), Overlay.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof MockOverlay);
        assertTrue(it.next() instanceof SubMockOverlay);
    }

    @Test
    public void addAfterWithoutExistingClass() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.add(new Rotate());
        instance.addAfter(new Scale(), Crop.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test(expected = IllegalStateException.class)
    public void addAfterWhileFrozen() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.freeze();
        instance.addAfter(new Rotate(), Crop.class);
    }

    @Test
    public void addBeforeWithExistingClass() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.add(new Rotate());
        instance.addBefore(new Scale(), Rotate.class);
        assertTrue(instance.iterator().next() instanceof Scale);
    }

    @Test
    public void addBeforeWithExistingSuperclass() {
        class SubMockOverlay extends MockOverlay {}

        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.add(new MockOverlay());
        instance.addBefore(new SubMockOverlay(), MockOverlay.class);
        assertTrue(instance.iterator().next() instanceof SubMockOverlay);
    }

    @Test
    public void addBeforeWithoutExistingClass() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.add(new Rotate());
        instance.addBefore(new Scale(), Crop.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Scale);
    }

    @Test(expected = IllegalStateException.class)
    public void addBeforeWhileFrozen() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.freeze();
        instance.addBefore(new Rotate(), Crop.class);
    }

    @Test
    public void applyNonEndpointMutationsWithOrientationMutatesCrop()
            throws Exception {
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = new Info(fullSize);
        info.getImages().get(0).setOrientation(Orientation.ROTATE_90);

        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG,
                new Crop(0, 0, 70, 30));

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
        final Info info = new Info(fullSize);
        info.getImages().get(0).setOrientation(Orientation.ROTATE_90);

        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG,
                new Crop(0, 0, 70, 30), new Rotate(45));

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
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG, new Rotate(45));
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
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG);
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
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG);
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
        final Info info = new Info(fullSize);
        OperationList opList = new OperationList(
                new Identifier("cats"), Format.TIF,
                new Crop());
        RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);

        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Normalize);

        // Assert that Normalizations are inserted before upscales when no
        // Crop is present.
        opList = new OperationList(new Identifier("cats"), Format.TIF,
                new Scale(1.5f));
        context = new RequestContext();
        context.setOperationList(opList, fullSize);
        proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        it = opList.iterator();
        assertTrue(it.next() instanceof Normalize);

        // Assert that Normalizations are inserted after downscales when no
        // Crop is present.
        opList = new OperationList(
                new Identifier("cats"), Format.TIF, new Scale(0.5f));
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
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.TIF);
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
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.TIF);
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Redaction redaction = (Redaction) opList.getFirst(Redaction.class);
        assertEquals(new Rectangle(0, 10, 50, 70), redaction.getRegion());
    }

    @Test
    public void applyNonEndpointMutationsWithLimitTo8Bits() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_LIMIT_TO_8_BITS, true);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG);
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(8, encode.getMaxSampleSize().intValue());
    }

    @Test
    public void applyNonEndpointMutationsWithoutLimitTo8Bits()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_LIMIT_TO_8_BITS, false);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG);
        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertNull(encode.getMaxSampleSize());
    }

    @Test
    public void applyNonEndpointMutationsWithScaleFilters() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_DOWNSCALE_FILTER, "bicubic");
        config.setProperty(Key.PROCESSOR_UPSCALE_FILTER, "triangle");

        // Downscale
        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = new Info(fullSize);
        OperationList opList = new OperationList(
                new Identifier("cats"), Format.TIF, new Scale(0.5f));
        RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());

        // Upscale
        opList = new OperationList(new Identifier("cats"), Format.TIF,
                new Scale(1.5f));
        context = new RequestContext();
        context.setOperationList(opList, fullSize);
        proxy = service.newDelegateProxy(context);

        opList.applyNonEndpointMutations(info, proxy);

        it = opList.iterator();
        assertEquals(Scale.Filter.TRIANGLE, ((Scale) it.next()).getFilter());
    }

    @Test
    public void applyNonEndpointMutationsWithSharpening() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SHARPEN, 0.2f);

        final Dimension fullSize = new Dimension(2000, 1000);
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.TIF);
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
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.TIF);
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
        final Info info = new Info(fullSize);
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG, new Crop(0, 0, 70, 30));

        opList.freeze();

        final RequestContext context = new RequestContext();
        context.setOperationList(opList, fullSize);
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
        OperationList ops2 = new OperationList(
                new Identifier("identifier.jpg"), Format.JPG);

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
        OperationList ops1 = TestUtil.newOperationList();
        OperationList ops2 = TestUtil.newOperationList();
        ops2.add(new Rotate());
        assertTrue(ops1.equals(ops2));
    }

    @Test
    public void equalsWithUnequalOperationList() {
        OperationList ops1 = TestUtil.newOperationList();
        OperationList ops2 = TestUtil.newOperationList();
        ops2.add(new Rotate(1));
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
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        Rotate rotate = new Rotate();
        instance.add(crop);
        instance.add(scale);
        instance.add(rotate);
        assertEquals(fullSize, instance.getResultingSize(fullSize));

        instance = new OperationList(new Identifier("cats"), Format.JPG);
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
    public void hasEffect() {
        // same format
        instance = new OperationList(new Identifier("identifier.gif"),
                Format.GIF);
        assertFalse(instance.hasEffect(Format.GIF));

        // different formats
        instance = new OperationList(new Identifier("identifier.jpg"),
                Format.GIF);
        assertTrue(instance.hasEffect(Format.JPG));
    }

    @Test
    public void hasEffectWithPdfSourceAndPdfOutputAndOverlay() {
        instance = new OperationList(new Identifier("identifier.pdf"),
                Format.PDF);
        assertFalse(instance.hasEffect(Format.PDF));
    }

    @Test
    public void hasEffectWithEncodeAndSameOutputFormat() {
        instance = new OperationList(new Identifier("identifier.jpg"),
                Format.JPG);
        instance.add(new Encode(Format.JPG));
        assertFalse(instance.hasEffect(Format.JPG));
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

    @Test(expected = IllegalStateException.class)
    public void setOutputFormatWhileFrozen() {
        instance.freeze();
        instance.setOutputFormat(Format.GIF);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setOutputFormatWithIllegalFormat() {
        instance.setOutputFormat(Format.UNKNOWN);
    }

    @Test
    public void toFilename() {
        instance = new OperationList(new Identifier("identifier.jpg"),
                Format.JPG);
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
        instance.getOptions().put("animal", "cat");

        String expected = "50c63748527e634134449ae20b199cc0_1ca68d1b775cb386ddeba799a994b6af.jpg";
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
        instance = new OperationList(new Identifier("identifier.jpg"),
                Format.JPG);
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

        final Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals("identifier.jpg", map.get("identifier"));
        assertEquals(2, ((List<?>) map.get("operations")).size());
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
        instance = new OperationList(new Identifier("identifier.jpg"),
                Format.JPG);
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
        instance.getOptions().put("animal", "cat");

        String expected = "identifier.jpg_crop:5,6,20,22_scale:40%_rotate:15_colortransform:bitonal_animal:cat.jpg";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void validateWithValidInstance() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList(
                new Identifier("cats"),
                Format.JPG,
                new Crop(0, 0, 100, 100));
        ops.validate(fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithOutOfBoundsCrop() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList(new Identifier("cats"),
                Format.JPG,
                new Crop(1001, 1001, 100, 100));
        ops.validate(fullSize);
    }

}
