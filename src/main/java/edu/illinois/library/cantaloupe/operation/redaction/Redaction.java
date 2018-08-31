package edu.illinois.library.cantaloupe.operation.redaction;

import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>Encapsulates a redacted region of an image.</p>
 *
 * <p>Instances should be obtained from the {@link RedactionService}.</p>
 */
public class Redaction implements Operation {

    private boolean isFrozen = false;
    private Rectangle region;

    /**
     * No-op constructor.
     */
    public Redaction() {}

    public Redaction(Rectangle region) {
        this.setRegion(region);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Redaction) {
            Redaction other = (Redaction) obj;
            return Objects.equals(other.getRegion(), getRegion());
        }
        return super.equals(obj);
    }

    @Override
    public void freeze() {
        isFrozen = true;
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
    public boolean hasEffect() {
        return (region != null && region.getWidth() > 0 &&
                region.getHeight() > 0);
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        if (!hasEffect()) {
            return false;
        }

        Rectangle resultingImage;
        Crop crop = (Crop) opList.getFirst(Crop.class);
        if (crop != null) {
            resultingImage = crop.getRectangle(fullSize);
        } else {
            resultingImage = new Rectangle(
                    0, 0, fullSize.width, fullSize.height);
        }
        return getRegion().intersects(resultingImage);
    }

    @Override
    public int hashCode() {
        return getRegion().hashCode();
    }

    /**
     * @param region Redacted region in source image pixel coordinates.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setRegion(Rectangle region) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.region = region;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with <code>x</code>, <code>y</code>, <code>width</code>,
     *         and <code>height</code> keys.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("x", getRegion().x);
        map.put("y", getRegion().y);
        map.put("width", getRegion().width);
        map.put("height", getRegion().height);
        return Collections.unmodifiableMap(map);
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
