package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.image.Operation;

import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates a watermark applied to an image.</p>
 *
 * <p>Instances should be obtained from the
 * {@link WatermarkService}.</p>
 */
public class Watermark implements Operation {

    private File image;
    private int inset = 0;
    private Position position;

    /**
     * No-op constructor.
     */
    public Watermark() {}

    public Watermark(File image, Position position, int inset) {
        this.setImage(image);
        this.setPosition(position);
        this.setInset(inset);
    }

    public File getImage() {
        return image;
    }

    public int getInset() {
        return inset;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    @Override
    public boolean isNoOp() {
        return false;
    }

    public void setImage(File image) {
        this.image = image;
    }

    public void setInset(int inset) {
        this.inset = inset;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("filename", getImage().getName());
        map.put("position", getPosition().toString());
        map.put("inset", getInset());
        return map;
    }

    /**
     * @return String representation of the instance, in the format
     * "{image filename}_{position}_{inset}".
     */
    @Override
    public String toString() {
        return String.format("%s_%s_%d",
                getImage().getName(), getPosition(), getInset());
    }

}
