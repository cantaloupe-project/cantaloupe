package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.Operation;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>Encapsulates an image overlaid on top of another image.</p>
 *
 * <p>Instances should be obtained from the {@link OverlayService}.</p>
 */
public class ImageOverlay extends Overlay implements Operation {

    static final Set<String> SUPPORTED_URI_SCHEMES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("http://", "https://", "file://")));

    private static ImageOverlayCache overlayCache = new ImageOverlayCache();

    private File file;
    private URI uri;

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
     * @param uri      Image URI.
     * @param position Position of the overlay.
     * @param inset    Inset in pixels.
     */
    public ImageOverlay(URI uri, Position position, int inset) {
        super(position, inset);
        setURI(uri);
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
     *         component of its URI.
     */
    public String getIdentifier() {
        if (getFile() != null) {
            return getFile().getName();
        } else {
            return getURI().getPath().replace("/", "");
        }
    }

    /**
     * For reading the image, clients should use {@link #openStream()} instead.
     *
     * @return URI of the image.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * @return Stream from which the image can be read.
     */
    public InputStream openStream() throws IOException {
        byte[] bytes;
        if (getFile() != null) {
            bytes = overlayCache.putAndGet(getFile());
        } else {
            bytes = overlayCache.putAndGet(getURI());
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * @param file Image file.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setFile(File file) {
        checkFrozen();
        this.uri = null;
        this.file = file;
    }

    /**
     * @param uri Image URI.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setURI(URI uri) {
        checkFrozen();
        this.file = null;
        this.uri = uri;
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
