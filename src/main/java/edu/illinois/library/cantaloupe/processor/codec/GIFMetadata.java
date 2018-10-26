package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadataNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GIFMetadata implements Metadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFMetadata.class);

    private GIFMetadataReader reader;

    /**
     * Cached by {@link #getOrientation()}.
     */
    private Orientation orientation;

    GIFMetadata(GIFMetadataReader reader) {
        this.reader = reader;
    }

    @Override
    public IIOMetadataNode getAsTree() {
        return null;
    }

    int getDelayTime() {
        try {
            return reader.getDelayTime();
        } catch (IOException e) {
            LOGGER.warn("getDelayTime(): {}", e.getMessage());
        }
        return 0;
    }

    /**
     * @return Null, as GIF does not support raw EXIF.
     */
    @Override
    public Object getEXIF() {
        return null;
    }

    /**
     * @return Null, as GIF does not support raw IPTC IIM.
     */
    @Override
    public Object getIPTC() {
        return null;
    }

    /**
     * @return Loop count of multi-frame (animated) GIFs. 0 is returned for
     *         single-frame and infinitely-looping GIFs.
     */
    int getLoopCount() {
        try {
            return reader.getLoopCount();
        } catch (IOException e) {
            LOGGER.warn("getLoopCount(): {}", e.getMessage());
        }
        return 0;
    }

    /**
     * @return Effective orientation of the image. The return value is cached.
     */
    @Override
    public Orientation getOrientation() {
        if (orientation == null) {
            final String xmp = new String(getXMP(), StandardCharsets.UTF_8);
            orientation = Util.readOrientation(xmp);
        }
        return orientation;
    }

    @Override
    public byte[] getXMP() {
        try {
            return reader.getXMP().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("getXMP(): {}", e.getMessage());
        }
        return null;
    }

}
