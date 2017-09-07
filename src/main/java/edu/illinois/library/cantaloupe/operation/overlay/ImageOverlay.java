package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.Operation;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates an image overlaid on top of another image.</p>
 *
 * <p>Instances should be obtained from the {@link OverlayService}.</p>
 */
public class ImageOverlay extends Overlay implements Operation {

    private static ImageOverlayCache overlayCache = new ImageOverlayCache();

    private File file;
    private URL url;

    /**
     * Constructor for images that reside on the local filesystem.
     *
     * @param file     Image file.
     * @param position Position of the overlay.
     * @param inset    Inset in pixels.
     */
    public ImageOverlay(File file, Position position, int inset) {
        super(position, inset);
        setFile(file);
    }

    /**
     * Constructor for images that reside on a web server.
     *
     * @param url      Image URL.
     * @param position Position of the overlay.
     * @param inset    Inset in pixels.
     */
    public ImageOverlay(URL url, Position position, int inset) {
        super(position, inset);
        setURL(url);
    }

    /**
     * For reading the image, clients should use {@link #openStream()} instead.
     *
     * @return Image file.
     */
    public File getFile() {
        return file;
    }

    /**
     * @return The identifier of the image, such as its filename or the file
     *         component of its URL.
     */
    public String getIdentifier() {
        if (getFile() != null) {
            return getFile().getName();
        } else {
            return getURL().getFile().replace("/", "");
        }
    }

    /**
     * For reading the image, clients should use {@link #openStream()} instead.
     *
     * @return URL of the image.
     */
    public URL getURL() {
        return url;
    }

    /**
     * @return Stream from which the image can be read.
     * @throws IOException
     */
    public InputStream openStream() throws IOException {
        byte[] bytes;
        if (getFile() != null) {
            bytes = overlayCache.putAndGet(getFile());
        } else {
            bytes = overlayCache.putAndGet(getURL());
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * @param file Image file.
     */
    public void setFile(File file) {
        this.url = null;
        this.file = file;
    }

    /**
     * @param url Image URL.
     */
    public void setURL(URL url) {
        this.file = null;
        this.url = url;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with <code>identifier</code>, <code>position</code>, and
     *         <code>inset</code> keys.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("filename", getIdentifier());
        map.put("position", getPosition().toString());
        map.put("inset", getInset());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return String representation of the instance, in the format
     * "{image filename}_{position}_{inset}".
     */
    @Override
    public String toString() {
        return String.format("%s_%s_%d",
                getIdentifier(), getPosition(), getInset());
    }

}
