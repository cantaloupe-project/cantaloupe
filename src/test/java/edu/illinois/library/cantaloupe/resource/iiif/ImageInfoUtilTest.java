package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ImageInfoUtilTest extends BaseTest {

    @Test
    public void testMaxReductionFactor() {
        Dimension fullSize = new Dimension(1024, 1024);
        int minDimension = 100;
        assertEquals(3, ImageInfoUtil.maxReductionFactor(fullSize, minDimension));

        fullSize = new Dimension(1024, 512);
        minDimension = 100;
        assertEquals(2, ImageInfoUtil.maxReductionFactor(fullSize, minDimension));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxReductionFactorWithZeroMinDimension() {
        Dimension fullSize = new Dimension(1024, 1024);
        int minDimension = 0;
        ImageInfoUtil.maxReductionFactor(fullSize, minDimension);
    }

    @Test
    public void testMinReductionFactor() {
        Dimension fullSize = new Dimension(50, 50);
        int maxPixels = 10000;
        assertEquals(0, ImageInfoUtil.minReductionFactor(fullSize, maxPixels));

        fullSize = new Dimension(100, 100);
        maxPixels = 10000;
        assertEquals(0, ImageInfoUtil.minReductionFactor(fullSize, maxPixels));

        fullSize = new Dimension(200, 100);
        maxPixels = 10000;
        assertEquals(1, ImageInfoUtil.minReductionFactor(fullSize, maxPixels));

        fullSize = new Dimension(300, 300);
        maxPixels = 10000;
        assertEquals(2, ImageInfoUtil.minReductionFactor(fullSize, maxPixels));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinReductionFactorWithZeroMaxPixels() {
        Dimension fullSize = new Dimension(50, 50);
        int maxPixels = 0;
        ImageInfoUtil.minReductionFactor(fullSize, maxPixels);
    }

    @Test
    public void testGetTileSizeWithTiledImage() {
        // full size > tile size > min tile size
        Dimension fullSize = new Dimension(1024, 1024);
        Dimension tileSize = new Dimension(512, 512);
        int minTileSize = 128;
        assertEquals(new Dimension(512, 512),
                ImageInfoUtil.getTileSize(fullSize, tileSize, minTileSize));

        // full size > min tile size > tile size
        fullSize = new Dimension(1024, 1024);
        tileSize = new Dimension(128, 100);
        minTileSize = 512;
        assertEquals(new Dimension(1024, 800),
                ImageInfoUtil.getTileSize(fullSize, tileSize, minTileSize));

        // min tile size > full size > tile size
        fullSize = new Dimension(512, 512);
        tileSize = new Dimension(128, 128);
        minTileSize = 768;
        assertEquals(new Dimension(512, 512),
                ImageInfoUtil.getTileSize(fullSize, tileSize, minTileSize));
    }

    @Test
    public void testGetTileSizeWithUntiledImage() {
        // full size > min tile size
        Dimension fullSize = new Dimension(1024, 1024);
        Dimension tileSize = new Dimension(fullSize);
        int minTileSize = 128;
        assertEquals(new Dimension(minTileSize, minTileSize),
                ImageInfoUtil.getTileSize(fullSize, tileSize, minTileSize));

        // full size < min tile size
        fullSize = new Dimension(512, 512);
        tileSize = new Dimension(fullSize);
        minTileSize = 768;
        assertEquals(new Dimension(512, 512),
                ImageInfoUtil.getTileSize(fullSize, tileSize, minTileSize));
    }

}
