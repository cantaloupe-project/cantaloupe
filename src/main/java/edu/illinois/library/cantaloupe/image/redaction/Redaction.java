package edu.illinois.library.cantaloupe.image.redaction;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Operation;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates a redacted region of an image.</p>
 *
 * <p>Instances should be obtained from the {@link RedactionService}.</p>
 */
public class Redaction implements Operation {

    private Rectangle region;

    /**
     * No-op constructor.
     */
    public Redaction() {}

    public Redaction(Rectangle region) {
        this.setRegion(region);
    }

    /**
     * @return Redacted region in source image pixel coordinates.
     */
    public Rectangle getRegion() {
        return region;
    }

    /**
     * @param sourceSize Size of the source image.
     * @param appliedCrop Crop that has been applied to the source image.
     * @return Region of the cropped image to be redacted, or an empty
     *         rectangle if none.
     */
    public Rectangle getResultingRegion(final Dimension sourceSize,
                                        final Crop appliedCrop) {
        final Rectangle cropRegion = appliedCrop.getRectangle(sourceSize);
        if (this.getRegion().intersects(cropRegion)) {
            final Rectangle offsetRect = (Rectangle) this.getRegion().clone();
            offsetRect.x -= cropRegion.x;
            offsetRect.y -= cropRegion.y;
            return offsetRect;
        }
        return new Rectangle(0, 0, 0, 0);
    }

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    @Override
    public boolean isNoOp() {
        return (region == null || region.getWidth() < 1 ||
                region.getHeight() < 1);
    }

    /**
     * @param region Redacted region in source image pixel coordinates.
     */
    public void setRegion(Rectangle region) {
        this.region = region;
    }

    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("x", getRegion().x);
        map.put("y", getRegion().y);
        map.put("width", getRegion().width);
        map.put("height", getRegion().height);
        return map;
    }

    /**
     * @return String representation of the instance, in the format
     * "{x},{y}/{width}x{height}".
     */
    @Override
    public String toString() {
        return String.format("%d,%d/%dx%d",
                getRegion().x, getRegion().y, getRegion().width,
                getRegion().height);
    }

}
