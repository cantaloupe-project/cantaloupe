package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GIFMetadata implements Metadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFMetadata.class);

    private GIFMetadataReader reader;

    /**
     * Cached by {@link #getOrientation()}.
     */
    private Orientation orientation;

    private boolean checkedForXMP;
    private String xmp;

    GIFMetadata(GIFMetadataReader reader) {
        this.reader = reader;
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
            final String xmp = getXMP();
            if (xmp != null) {
                orientation = Util.readOrientation(xmp);
            }
        }
        return orientation;
    }

    @Override
    public String getXMP() {
        if (!checkedForXMP) {
            checkedForXMP = true;
            try {
                xmp = reader.getXMP();
                if (xmp != null) {
                    xmp = Util.trimXMP(xmp);
                }
            } catch (IOException e) {
                LOGGER.warn("getXMP(): {}", e.getMessage());
            }
        }
        return xmp;
    }

}
