package edu.illinois.library.cantaloupe.operation;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.overlay.BasicStringOverlayServiceTest;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionServiceTest;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OperationListTest extends BaseTest {

    class DummyOverlay extends Overlay {
        DummyOverlay() {
            super(Position.TOP_LEFT, 0);
        }
        @Override
        public Map<String, Object> toMap(Dimension fullSize) {
            return new HashMap<>();
        }
    }

    private OperationList instance;

    private static OperationList newOperationList() {
        OperationList ops = new OperationList(new Identifier("identifier.jpg"),
                Format.JPG);

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

    @Test
    public void addWhileFrozen() {
        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.freeze();
        try {
            instance.add(new Rotate());
            fail("Expected exception");
        } catch (IllegalStateException e) {
            // pass
        }
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
        instance.add(new DummyOverlay());

        class SubDummyOverlay extends DummyOverlay {}

        instance.addAfter(new SubDummyOverlay(), Overlay.class);
        Iterator<Operation> it = instance.iterator();
        assertTrue(it.next() instanceof DummyOverlay);
        assertTrue(it.next() instanceof SubDummyOverlay);
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
        class SubDummyOverlay extends DummyOverlay {}

        instance = new OperationList(new Identifier("cats"), Format.JPG);
        instance.add(new DummyOverlay());
        instance.addBefore(new SubDummyOverlay(), DummyOverlay.class);
        assertTrue(instance.iterator().next() instanceof SubDummyOverlay);
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
        final Dimension fullSize = new Dimension(2000,1000);
        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG, new Crop(0, 0, 70, 30));

        opList.applyNonEndpointMutations(
                fullSize,
                Orientation.ROTATE_90,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Crop expectedCrop = new Crop(0, 0, 70, 30);
        expectedCrop.applyOrientation(Orientation.ROTATE_90, fullSize);
        Crop actualCrop = (Crop) opList.getFirst(Crop.class);
        assertEquals(expectedCrop, actualCrop);
    }

    @Test
    public void applyNonEndpointMutationsWithOrientationMutatesRotate()
            throws Exception {
        final Dimension fullSize = new Dimension(2000,1000);
        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG, new Crop(0, 0, 70, 30), new Rotate(45));

        opList.applyNonEndpointMutations(
                fullSize,
                Orientation.ROTATE_90,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

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

        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG, new Rotate(45));

        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(Color.fromString("#FFFFFF"), encode.getBackgroundColor());
    }

    @Test
    public void applyNonEndpointMutationsWithJPEGOutputFormat()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_JPG_QUALITY, 50);
        config.setProperty(Key.PROCESSOR_JPG_PROGRESSIVE, true);

        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG);
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

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

        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG);
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof MetadataCopy);
    }

    @Test
    public void applyNonEndpointMutationsWithNormalization() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_NORMALIZE, true);

        // Assert that Normalizations are inserted before Crops.
        OperationList opList = new OperationList(new Identifier("cats"),
                Format.TIF, new Crop());
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Normalize);

        // Assert that Normalizations are inserted before upscales when no
        // Crop is present.
        opList = new OperationList(new Identifier("cats"), Format.TIF,
                new Scale(1.5f));
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        it = opList.iterator();
        assertTrue(it.next() instanceof Normalize);

        // Assert that Normalizations are inserted after downscales when no
        // Crop is present.
        opList = new OperationList(new Identifier("cats"), Format.TIF,
                new Scale(0.5f));
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        it = opList.iterator();
        assertTrue(it.next() instanceof Scale);
        assertTrue(it.next() instanceof Normalize);
    }

    @Test
    public void applyNonEndpointMutationsWithOverlay() throws Exception {
        BasicStringOverlayServiceTest.setUpConfiguration();

        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.TIF);
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Overlay overlay = (Overlay) opList.getFirst(Overlay.class);
        assertEquals(10, overlay.getInset());
    }

    @Test
    public void applyNonEndpointMutationsWithRedactions() throws Exception {
        RedactionServiceTest.setUpConfiguration();

        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.TIF);
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Redaction redaction = (Redaction) opList.getFirst(Redaction.class);
        assertEquals(new Rectangle(0, 10, 50, 70), redaction.getRegion());
    }

    @Test
    public void applyNonEndpointMutationsWithLimitTo8Bits() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_LIMIT_TO_8_BITS, true);

        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG);
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(8, encode.getMaxSampleSize().intValue());
    }

    @Test
    public void applyNonEndpointMutationsWithoutLimitTo8Bits()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_LIMIT_TO_8_BITS, false);

        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG);
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertNull(encode.getMaxSampleSize());
    }

    @Test
    public void applyNonEndpointMutationsWithScaleFilters() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_DOWNSCALE_FILTER, "bicubic");
        config.setProperty(Key.PROCESSOR_UPSCALE_FILTER, "triangle");

        // Downscale
        OperationList opList = new OperationList(new Identifier("cats"),
                Format.TIF, new Scale(0.5f));
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());

        // Upscale
        opList = new OperationList(new Identifier("cats"), Format.TIF,
                new Scale(1.5f));
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        it = opList.iterator();
        assertEquals(Scale.Filter.TRIANGLE, ((Scale) it.next()).getFilter());
    }

    @Test
    public void applyNonEndpointMutationsWithSharpening() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SHARPEN, 0.2f);

        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.TIF);
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

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

        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.TIF);
        opList.applyNonEndpointMutations(
                new Dimension(2000,1000),
                Orientation.ROTATE_0,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());

        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Encode);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(Compression.LZW, encode.getCompression());
    }

    @Test(expected = IllegalStateException.class)
    public void applyNonEndpointMutationsWhileFrozen()
            throws Exception {
        final Dimension fullSize = new Dimension(2000,1000);
        final OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG, new Crop(0, 0, 70, 30));

        opList.freeze();

        opList.applyNonEndpointMutations(
                fullSize,
                Orientation.ROTATE_90,
                "127.0.0.1",
                new URI("http://example.org/"),
                new HashMap<>(),
                new HashMap<>());
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
        OperationList ops2 = new OperationList(new Identifier("identifier.jpg"),
                Format.JPG);

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
        instance.add(new DummyOverlay());

        Overlay overlay = (Overlay) instance.getFirst(Overlay.class);
        assertNotNull(overlay);
        assertTrue(overlay instanceof DummyOverlay);
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
        assertFalse(instance.hasEffect(new Dimension(100, 100), Format.GIF));

        // different formats
        instance = new OperationList(new Identifier("identifier.jpg"),
                Format.GIF);
        assertTrue(instance.hasEffect(new Dimension(100, 100), Format.JPG));
    }

    @Test
    public void hasEffectWithPDFSourceAndPDFOutputAndOverlay() {
        instance = new OperationList(new Identifier("identifier.pdf"),
                Format.PDF);
        assertFalse(instance.hasEffect(new Dimension(100, 100), Format.PDF));
    }

    @Test
    public void hasEffectWithEncodeAndSameOutputFormat() {
        instance = new OperationList(new Identifier("identifier.jpg"),
                Format.JPG);
        instance.add(new Encode(Format.JPG));
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
    public void validateWithValidInstance() throws ValidationException {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList(
                new Identifier("cats"),
                Format.JPG,
                new Crop(0, 0, 100, 100));
        ops.validate(fullSize);
    }

    @Test(expected = ValidationException.class)
    public void validateWithOutOfBoundsCrop() throws ValidationException {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList(new Identifier("cats"),
                Format.JPG,
                new Crop(1001, 1001, 100, 100));
        ops.validate(fullSize);
    }

}
