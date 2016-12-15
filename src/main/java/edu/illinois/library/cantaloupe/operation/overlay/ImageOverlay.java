package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.Operation;

import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates an image overlay applied to an image.</p>
 *
 * <p>Instances should be obtained from the {@link OverlayService}.</p>
 */
public class ImageOverlay extends Overlay implements Operation {

    private File image;

    public ImageOverlay(File image, Position position, int inset) {
        super(position, inset);
        setImage(image);
    }

    public File getImage() {
        return image;
    }

    public void setImage(File image) {
        this.image = image;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with <code>filename</code>, <code>position</code>, and
     *         <code>inset</code> keys.
     */
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
