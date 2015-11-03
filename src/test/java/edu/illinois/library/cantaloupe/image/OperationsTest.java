package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class OperationsTest extends CantaloupeTestCase {

    private Operations ops;

    public void setUp() {
        Identifier identifier = new Identifier("identifier");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation();
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
    }

    public void testCompareTo() {
        // TODO: write this
    }

    public void testIsRequestingUnmodifiedSource() {
        Identifier identifier = new Identifier("identifier");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertFalse(ops.isRequestingUnmodifiedSource());

        identifier = new Identifier("identifier.jpg");
        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        rotation = new Rotation(0);
        quality = Quality.DEFAULT;
        format = OutputFormat.JPG;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertTrue(ops.isRequestingUnmodifiedSource());

        identifier = new Identifier("identifier.gif");
        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        rotation = new Rotation(0);
        quality = Quality.DEFAULT;
        format = OutputFormat.GIF;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertTrue(ops.isRequestingUnmodifiedSource());

        identifier = new Identifier("identifier.gif");
        crop = new Crop();
        crop.setFull(false);
        crop.setX(30f);
        crop.setY(30f);
        crop.setWidth(30f);
        crop.setHeight(30f);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        rotation = new Rotation(0);
        quality = Quality.DEFAULT;
        format = OutputFormat.GIF;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertFalse(ops.isRequestingUnmodifiedSource());

        identifier = new Identifier("identifier.gif");
        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.ASPECT_FIT_INSIDE);
        rotation = new Rotation(0);
        quality = Quality.DEFAULT;
        format = OutputFormat.GIF;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertTrue(ops.isRequestingUnmodifiedSource());

        identifier = new Identifier("identifier.gif");
        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.5f);
        rotation = new Rotation(0);
        quality = Quality.DEFAULT;
        format = OutputFormat.GIF;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertFalse(ops.isRequestingUnmodifiedSource());

        identifier = new Identifier("identifier.gif");
        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        rotation = new Rotation(2);
        quality = Quality.DEFAULT;
        format = OutputFormat.GIF;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertFalse(ops.isRequestingUnmodifiedSource());

        identifier = new Identifier("identifier.gif");
        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        rotation = new Rotation();
        rotation.setMirror(true);
        quality = Quality.DEFAULT;
        format = OutputFormat.GIF;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertTrue(ops.isRequestingUnmodifiedSource());

        identifier = new Identifier("identifier.gif");
        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        rotation = new Rotation(0);
        quality = Quality.COLOR;
        format = OutputFormat.GIF;
        ops = new Operations(identifier, crop, scale, rotation, quality,
                format);
        assertTrue(ops.isRequestingUnmodifiedSource());
    }

    public void testToString() {
        String expected = String.format("%s_%s_%s_%s_%s_%s",
                this.ops.getIdentifier(), this.ops.getRegion(),
                this.ops.getScale(), this.ops.getRotation(),
                this.ops.getQuality().toString().toLowerCase(),
                this.ops.getOutputFormat());
        assertEquals(expected, this.ops.toString());
    }

}
