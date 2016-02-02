package edu.illinois.library.cantaloupe.image.watermark;

import java.io.File;

/**
 * <p>Encapsulates a watermark applied to an image.</p>
 *
 * <p>Instances should be obtained from the
 * {@link WatermarkService}.</p>
 */
public class Watermark {

    private File image;
    private int inset;
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

    public void setImage(File image) {
        this.image = image;
    }

    public void setInset(int inset) {
        this.inset = inset;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

}
