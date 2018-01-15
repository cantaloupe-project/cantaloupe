package edu.illinois.library.cantaloupe.processor.imageio;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sequence of images in temporal order, to support video or slide-show image
 * content, such as animated GIFs.
 */
public class BufferedImageSequence implements Iterable<BufferedImage> {

    private final List<BufferedImage> images = new ArrayList<>();

    public void add(BufferedImage image) {
        images.add(image);
    }

    public BufferedImage get(int index) {
        return images.get(index);
    }

    @Override
    public Iterator<BufferedImage> iterator() {
        return images.iterator();
    }

    public int length() {
        return images.size();
    }

    public void set(int index, BufferedImage image) {
        images.set(index, image);
    }

}
