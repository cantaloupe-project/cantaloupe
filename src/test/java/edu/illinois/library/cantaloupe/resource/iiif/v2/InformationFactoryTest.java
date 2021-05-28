package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class InformationFactoryTest extends BaseTest {

    private static final Set<Format> PROCESSOR_FORMATS = Set.of(
            Format.get("gif"), Format.get("jpg"), Format.get("png"));

    private InformationFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new InformationFactory();
    }

    private Information<String,Object> invokeNewImageInfo() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder().withSize(1500, 1200).build();
        return instance.newImageInfo(PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 1));
    }

    @Test
    void testNewImageInfoContext() {
        Information<String,Object> info = invokeNewImageInfo();
        assertEquals("http://iiif.io/api/image/2/context.json",
                info.get("@context"));
    }

    @Test
    void testNewImageInfoID() {
        Information<String,Object> info = invokeNewImageInfo();
        assertEquals("http://example.org/bla", info.get("@id"));
    }

    @Test
    void testNewImageInfoProtocol() {
        Information<String,Object> info = invokeNewImageInfo();
        assertEquals("http://iiif.io/api/image", info.get("protocol"));
    }

    @Test
    void testNewImageInfoWidth() {
        Information<String,Object> info = invokeNewImageInfo();
        assertEquals(1500, info.get("width"));
    }

    @Test
    void testNewImageInfoWidthWithRotatedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1500, 1200)
                .withMetadata(new Metadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 1));

        assertEquals(1200, iiifInfo.get("width"));
    }

    @Test
    void testNewImageInfoWidthWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1499, 1199) // test rounding
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 2));

        assertEquals(750, iiifInfo.get("width"));
    }

    @Test
    void testNewImageInfoHeight() {
        Information<String,Object> info = invokeNewImageInfo();
        assertEquals(1200, info.get("height"));
    }

    @Test
    void testNewImageInfoHeightWithRotatedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1500, 1200)
                .withMetadata(new Metadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 1));

        assertEquals(1500, iiifInfo.get("height"));
    }

    @Test
    void testNewImageInfoHeightWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1499, 1199) // test rounding
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 2));

        assertEquals(600, iiifInfo.get("height"));
    }

    @Test
    void testNewImageInfoSizes() {
        Information<String, Object> iiifInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<Information.Size> sizes =
                (List<Information.Size>) iiifInfo.get("sizes");
        assertEquals(5, sizes.size());
        assertEquals(94, (int) sizes.get(0).width);
        assertEquals(75, (int) sizes.get(0).height);
        assertEquals(188, (int) sizes.get(1).width);
        assertEquals(150, (int) sizes.get(1).height);
        assertEquals(375, (int) sizes.get(2).width);
        assertEquals(300, (int) sizes.get(2).height);
        assertEquals(750, (int) sizes.get(3).width);
        assertEquals(600, (int) sizes.get(3).height);
        assertEquals(1500, (int) sizes.get(4).width);
        assertEquals(1200, (int) sizes.get(4).height);
    }

    @Test
    void testNewImageInfoSizesMinSize() {
        instance.setMinSize(500);
        Information<String, Object> iiifInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<Information.Size> sizes =
                (List<Information.Size>) iiifInfo.get("sizes");
        assertEquals(2, sizes.size());
        assertEquals(750, (int) sizes.get(0).width);
        assertEquals(600, (int) sizes.get(0).height);
        assertEquals(1500, (int) sizes.get(1).width);
        assertEquals(1200, (int) sizes.get(1).height);
    }

    @Test
    void testNewImageInfoSizesMaxSize() {
        instance.setMaxPixels(10000);
        Information<String, Object> iiifInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<Information.Size> sizes =
                (List<Information.Size>) iiifInfo.get("sizes");
        assertEquals(1, sizes.size());
        assertEquals(94, (int) sizes.get(0).width);
        assertEquals(75, (int) sizes.get(0).height);
    }

    @Test
    void testNewImageInfoSizesWithRotatedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1500, 1200)
                .withMetadata(new Metadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<Information.Size> sizes =
                (List<Information.Size>) iiifInfo.get("sizes");
        assertEquals(5, sizes.size());
        assertEquals(75, (int) sizes.get(0).width);
        assertEquals(94, (int) sizes.get(0).height);
        assertEquals(150, (int) sizes.get(1).width);
        assertEquals(188, (int) sizes.get(1).height);
        assertEquals(300, (int) sizes.get(2).width);
        assertEquals(375, (int) sizes.get(2).height);
        assertEquals(600, (int) sizes.get(3).width);
        assertEquals(750, (int) sizes.get(3).height);
        assertEquals(1200, (int) sizes.get(4).width);
        assertEquals(1500, (int) sizes.get(4).height);
    }

    @Test
    void testNewImageInfoSizesWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1500, 1200)
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 2));

        @SuppressWarnings("unchecked")
        List<Information.Size> sizes =
                (List<Information.Size>) iiifInfo.get("sizes");
        assertEquals(4, sizes.size());
        assertEquals(94, (int) sizes.get(0).width);
        assertEquals(75, (int) sizes.get(0).height);
        assertEquals(188, (int) sizes.get(1).width);
        assertEquals(150, (int) sizes.get(1).height);
        assertEquals(375, (int) sizes.get(2).width);
        assertEquals(300, (int) sizes.get(2).height);
        assertEquals(750, (int) sizes.get(3).width);
        assertEquals(600, (int) sizes.get(3).height);
    }

    @Test
    void testNewImageInfoTilesWithUntiledMonoResolutionImage() {
        Information<String, Object> iiifInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<Information.Tile> tiles =
                (List<Information.Tile>) iiifInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(512, (int) tiles.get(0).width);
        assertEquals(512, (int) tiles.get(0).height);

        assertEquals(5, tiles.get(0).scaleFactors.size());
        assertEquals(1, (int) tiles.get(0).scaleFactors.get(0));
        assertEquals(2, (int) tiles.get(0).scaleFactors.get(1));
        assertEquals(4, (int) tiles.get(0).scaleFactors.get(2));
        assertEquals(8, (int) tiles.get(0).scaleFactors.get(3));
        assertEquals(16, (int) tiles.get(0).scaleFactors.get(4));
    }

    @Test
    void testNewImageInfoTilesWithUntiledMultiResolutionImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(3000, 2000)
                .withNumResolutions(3)
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<Information.Tile> tiles =
                (List<Information.Tile>) iiifInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(512, (int) tiles.get(0).width);
        assertEquals(512, (int) tiles.get(0).height);

        assertEquals(5, tiles.get(0).scaleFactors.size());
        assertEquals(1, (int) tiles.get(0).scaleFactors.get(0));
        assertEquals(2, (int) tiles.get(0).scaleFactors.get(1));
        assertEquals(4, (int) tiles.get(0).scaleFactors.get(2));
        assertEquals(8, (int) tiles.get(0).scaleFactors.get(3));
        assertEquals(16, (int) tiles.get(0).scaleFactors.get(4));
    }


    @Test
    void testNewImageInfoMinTileSize() {
        final String imageURI = "http://example.org/bla";
        Info info = Info.builder()
                .withSize(2000, 2000)
                .withTileSize(1000, 1000)
                .build();
        instance.setMinTileSize(1000);
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<Information.Tile> tiles =
                (List<Information.Tile>) iiifInfo.get("tiles");
        assertEquals(1000, (int) tiles.get(0).width);
        assertEquals(1000, (int) tiles.get(0).height);
    }

    @Test
    void testNewImageInfoTilesWithRotatedImage() {
        final String imageURI = "http://example.org/bla";
        Info info = Info.builder()
                .withSize(64, 56)
                .withMetadata(new Metadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .withTileSize(64, 56)
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<Information.Tile> tiles =
                (List<Information.Tile>) iiifInfo.get("tiles");
        assertEquals(56, (int) tiles.get(0).width);
        assertEquals(64, (int) tiles.get(0).height);
    }

    @Test
    void testNewImageInfoTilesWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        Info info = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 2));

        @SuppressWarnings("unchecked")
        List<Information.Tile> tiles =
                (List<Information.Tile>) iiifInfo.get("tiles");
        assertEquals(32, (int) tiles.get(0).width);
        assertEquals(28, (int) tiles.get(0).height);
    }

    @Test
    void testNewImageInfoTilesWithTiledImage() {
        final String imageURI = "http://example.org/bla";
        Info info = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<Information.Tile> tiles =
                (List<Information.Tile>) iiifInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(64, (int) tiles.get(0).width);
        assertEquals(56, (int) tiles.get(0).height);

        assertEquals(1, tiles.get(0).scaleFactors.size());
        assertEquals(1, (int) tiles.get(0).scaleFactors.get(0));
    }

    @Test
    void testNewImageInfoProfile() {
        Information<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        assertEquals("http://iiif.io/api/image/2/level2.json", profile.get(0));
    }

    @Test
    void testNewImageInfoFormats() {
        Information<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent.)
        assertTrue(((Set<?>) ((Map<?, ?>) profile.get(1)).get("formats")).contains("gif"));
    }

    @Test
    void testNewImageInfoQualities() {
        Information<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent.)
        assertTrue(((Set<?>) ((Map<?, ?>) profile.get(1)).get("qualities")).contains("color"));
    }

    @Test
    void testNewImageInfoMaxAreaWithPositiveMaxPixels() {
        final int maxPixels = 100;
        instance.setMaxPixels(maxPixels);

        Information<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        assertEquals(maxPixels, ((Map<?, ?>) profile.get(1)).get("maxArea"));
    }

    @Test
    void testNewImageInfoMaxAreaWithZeroMaxPixels() {
        final int maxPixels = 0;
        instance.setMaxPixels(maxPixels);

        Information<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        assertFalse(((Map<?, ?>) profile.get(1)).containsKey("maxArea"));
    }

    @Test
    void testNewImageInfoMaxAreaWithAllowUpscalingDisabled() {
        final int maxPixels = 2000000;
        instance.setMaxPixels(maxPixels);
        instance.setMaxScale(1.0);

        Information<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        assertEquals(1500 * 1200, ((Map<?, ?>) profile.get(1)).get("maxArea"));
    }

    @Test
    void testNewImageInfoSupports() {
        Information<String, Object> iiifInfo = invokeNewImageInfo();

        List<?> profile = (List<?>) iiifInfo.get("profile");
        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
        assertTrue(supportsSet.contains("baseUriRedirect"));
        assertTrue(supportsSet.contains("canonicalLinkHeader"));
        assertTrue(supportsSet.contains("cors"));
        assertTrue(supportsSet.contains("jsonldMediaType"));
        assertTrue(supportsSet.contains("profileLinkHeader"));
        assertTrue(supportsSet.contains("sizeByConfinedWh"));
        assertTrue(supportsSet.contains("sizeByWhListed"));
    }

    @Test
    void testNewImageInfoSupportsWhenUpscalingIsAllowed() {
        instance.setMaxScale(9.0);
        Information<String, Object> iiifInfo = invokeNewImageInfo();

        List<?> profile = (List<?>) iiifInfo.get("profile");
        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
        assertTrue(supportsSet.contains("sizeAboveFull"));
    }

    @Test
    void testNewImageInfoSupportsWhenUpscalingIsDisallowed() {
        instance.setMaxScale(1.0);
        Information<String, Object> iiifInfo = invokeNewImageInfo();

        List<?> profile = (List<?>) iiifInfo.get("profile");
        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
        assertFalse(supportsSet.contains("sizeAboveFull"));
    }

    @Test
    void testNewImageInfoSupportsWithScaleConstraint() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder().withSize(1500, 1200).build();
        Information<String, Object> iiifInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0, new ScaleConstraint(1, 4));

        List<?> profile = (List<?>) iiifInfo.get("profile");
        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
        assertFalse(supportsSet.contains("sizeAboveFull"));
    }

    @Test
    void testNewImageInfoDelegateKeys() {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        instance.setDelegateProxy(proxy);

        Information<String, Object> iiifInfo = invokeNewImageInfo();

        assertEquals("Copyright My Great Organization. All rights reserved.",
                iiifInfo.get("attribution"));
        assertEquals("http://example.org/license.html",
                iiifInfo.get("license"));
    }

}
