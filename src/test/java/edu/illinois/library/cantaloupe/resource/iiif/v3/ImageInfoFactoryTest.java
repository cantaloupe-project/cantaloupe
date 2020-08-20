package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ImageInfoFactoryTest extends BaseTest {

    private static final Set<Format> PROCESSOR_FORMATS =
            Set.of(Format.get("gif"), Format.get("jpg"), Format.get("png"));

    private ImageInfoFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new ImageInfoFactory();
    }

    private ImageInfo<String,Object> invokeNewImageInfo() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder().withSize(1500, 1200).build();
        return instance.newImageInfo(PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 1));
    }

    @Test
    void testNewImageInfoContext() {
        ImageInfo<String,Object> info = invokeNewImageInfo();
        assertEquals("http://iiif.io/api/image/3/context.json",
                info.get("@context"));
    }

    @Test
    void testNewImageInfoID() {
        ImageInfo<String,Object> info = invokeNewImageInfo();
        assertEquals("http://example.org/bla", info.get("id"));
    }

    @Test
    void testNewImageInfoType() {
        ImageInfo<String,Object> info = invokeNewImageInfo();
        assertEquals("ImageService3", info.get("type"));
    }

    @Test
    void testNewImageInfoProtocol() {
        ImageInfo<String,Object> info = invokeNewImageInfo();
        assertEquals("http://iiif.io/api/image", info.get("protocol"));
    }

    @Test
    void testNewImageInfoProfile() {
        ImageInfo<String,Object> info = invokeNewImageInfo();
        assertEquals("level2", info.get("profile"));
    }

    @Test
    void testNewImageInfoWidth() {
        ImageInfo<String,Object> info = invokeNewImageInfo();
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info,
                0, new ScaleConstraint(1, 1));

        assertEquals(1200, imageInfo.get("width"));
    }

    @Test
    void testNewImageInfoWidthWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1499, 1199) // test rounding
                .build();
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 2));

        assertEquals(750, imageInfo.get("width"));
    }

    @Test
    void testNewImageInfoHeight() {
        ImageInfo<String,Object> info = invokeNewImageInfo();
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 1));

        assertEquals(1500, imageInfo.get("height"));
    }

    @Test
    void testNewImageInfoHeightWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1499, 1199) // test rounding
                .build();
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 2));

        assertEquals(600, imageInfo.get("height"));
    }

    @Test
    void testNewImageInfoMaxAreaWithPositiveMaxPixels() {
        final int maxPixels = 100;
        instance.setMaxPixels(maxPixels);

        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();
        assertEquals(maxPixels, imageInfo.get("maxArea"));
    }

    @Test
    void testNewImageInfoMaxAreaWithZeroMaxPixels() {
        final int maxPixels = 0;
        instance.setMaxPixels(maxPixels);

        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();
        assertFalse(imageInfo.containsKey("maxArea"));
    }

    @Test
    void testNewImageInfoMaxAreaWithAllowUpscalingDisabled() {
        final int maxPixels = 2000000;
        instance.setMaxPixels(maxPixels);
        instance.setMaxScale(1.0);

        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();
        assertEquals(1500 * 1200, imageInfo.get("maxArea"));
    }

    @Test
    void testNewImageInfoSizes() {
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<ImageInfo.Size> sizes =
                (List<ImageInfo.Size>) imageInfo.get("sizes");
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
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<ImageInfo.Size> sizes =
                (List<ImageInfo.Size>) imageInfo.get("sizes");
        assertEquals(2, sizes.size());
        assertEquals(750, (int) sizes.get(0).width);
        assertEquals(600, (int) sizes.get(0).height);
        assertEquals(1500, (int) sizes.get(1).width);
        assertEquals(1200, (int) sizes.get(1).height);
    }

    @Test
    void testNewImageInfoSizesMaxSize() {
        instance.setMaxPixels(10000);
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<ImageInfo.Size> sizes =
                (List<ImageInfo.Size>) imageInfo.get("sizes");
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<ImageInfo.Size> sizes =
                (List<ImageInfo.Size>) imageInfo.get("sizes");
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 2));

        @SuppressWarnings("unchecked")
        List<ImageInfo.Size> sizes =
                (List<ImageInfo.Size>) imageInfo.get("sizes");
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
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 2));

        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
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
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(64, (int) tiles.get(0).width);
        assertEquals(56, (int) tiles.get(0).height);

        assertEquals(1, tiles.get(0).scaleFactors.size());
        assertEquals(1, (int) tiles.get(0).scaleFactors.get(0));
    }

    @Test
    void testNewImageInfoExtraQualities() {
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();
        List<?> qualities = (List<?>) imageInfo.get("extraQualities");
        assertEquals(3, qualities.size());
        assertTrue(qualities.contains("color"));
        assertTrue(qualities.contains("gray"));
        assertTrue(qualities.contains("bitonal"));
    }

    @Test
    void testNewImageInfoExtraFormats() {
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();
        List<?> formats = (List<?>) imageInfo.get("extraFormats");
        assertEquals(1, formats.size());
        assertTrue(formats.contains("gif"));
    }

    @Test
    void testNewImageInfoExtraFeatures() {
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();
        List<?> features = (List<?>) imageInfo.get("extraFeatures");
        assertEquals(17, features.size());
    }

    @Test
    void testNewImageInfoExtraFeaturesOmitsSizeUpscalingWhenMaxScaleIsLessThanOrEqualTo1() {
        instance.setMaxScale(1.0);
        ImageInfo<String,Object> imageInfo = invokeNewImageInfo();

        List<?> features = (List<?>) imageInfo.get("extraFeatures");
        assertEquals(16, features.size());
        assertFalse(features.contains("sizeUpscaling"));
    }

    @Test
    void testNewImageInfoExtraFeaturesWhenUpscalingIsAllowed() {
        instance.setMaxScale(9.0);
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();

        List<?> features = (List<?>) imageInfo.get("extraFeatures");
        assertTrue(features.contains("sizeUpscaling"));
    }

    @Test
    void testNewImageInfoExtraFeaturesWhenUpscalingIsDisallowed() {
        instance.setMaxScale(1.0);
        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();

        List<?> features = (List<?>) imageInfo.get("extraFeatures");
        assertFalse(features.contains("sizeUpscaling"));
    }

    @Test
    void testNewImageInfoExtraFeaturesWithScaleConstraint() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder().withSize(1500, 1200).build();
        ImageInfo<String, Object> imageInfo = instance.newImageInfo(
                PROCESSOR_FORMATS, imageURI, info, 0,
                new ScaleConstraint(1, 4));

        List<?> features = (List<?>) imageInfo.get("extraFeatures");
        assertFalse(features.contains("sizeUpscaling"));
    }

    @Test
    void testNewImageInfoDelegateKeys() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        RequestContext context = new RequestContext();
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);

        ImageInfo<String, Object> imageInfo = invokeNewImageInfo();

        assertEquals("Copyright My Great Organization. All rights reserved.",
                imageInfo.get("attribution"));
        assertEquals("http://example.org/license.html",
                imageInfo.get("license"));
    }

}
