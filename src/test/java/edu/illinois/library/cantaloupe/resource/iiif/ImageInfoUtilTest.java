package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.awt.Dimension;

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

    @Test
    public void testSmallestTileSize() {
        Dimension fullSize = new Dimension(1024, 1024);
        int minTileSize = 100;
        assertEquals(new Dimension(128, 128),
                ImageInfoUtil.smallestTileSize(fullSize, minTileSize));

        fullSize = new Dimension(300, 200);
        minTileSize = 100;
        assertEquals(new Dimension(150, 100),
                ImageInfoUtil.smallestTileSize(fullSize, minTileSize));

        fullSize = new Dimension(60, 50);
        minTileSize = 100;
        assertEquals(new Dimension(60, 50),
                ImageInfoUtil.smallestTileSize(fullSize, minTileSize));

        // Test rounding up with odd dimensions
        fullSize = new Dimension(2001, 2001);
        minTileSize = 1000;
        assertEquals(new Dimension(1001, 1001),
                ImageInfoUtil.smallestTileSize(fullSize, minTileSize));

        // Test rounding up with odd dimensions
        fullSize = new Dimension(2001, 2001);
        minTileSize = 100;
        assertEquals(new Dimension(126, 126),
                ImageInfoUtil.smallestTileSize(fullSize, minTileSize));
    }

    @Test
    public void testSmallestTileSizeWithNativeTileSize() {
        // full size > tile size > min tile size
        Dimension fullSize = new Dimension(1024, 1024);
        Dimension tileSize = new Dimension(512, 512);
        int minTileSize = 128;
        assertEquals(new Dimension(512, 512),
                ImageInfoUtil.smallestTileSize(fullSize, tileSize, minTileSize));

        // full size > min tile size > tile size
        fullSize = new Dimension(1024, 1024);
        tileSize = new Dimension(128, 100);
        minTileSize = 512;
        assertEquals(new Dimension(1024, 800),
                ImageInfoUtil.smallestTileSize(fullSize, tileSize, minTileSize));

        // min tile size > full size > tile size
        fullSize = new Dimension(512, 512);
        tileSize = new Dimension(128, 100);
        minTileSize = 768;
        assertEquals(new Dimension(512, 512),
                ImageInfoUtil.smallestTileSize(fullSize, tileSize, minTileSize));

        // null native size
        fullSize = new Dimension(512, 512);
        tileSize = null;
        minTileSize = 512;
        assertEquals(new Dimension(512, 512),
                ImageInfoUtil.smallestTileSize(fullSize, tileSize, minTileSize));
    }

}
