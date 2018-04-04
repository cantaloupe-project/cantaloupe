package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;

class NullMetadata extends AbstractMetadata implements Metadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NullMetadata.class);

    NullMetadata() {
        super(null, null);
    }

    /**
     * @param metadata
     * @param formatName
     */
    NullMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return Null.
     */
    @Override
    public Object getEXIF() {
        return null;
    }

    /**
     * @return Null.
     */
    @Override
    public Object getIPTC() {
        return null;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * @return {@link Orientation#ROTATE_0}
     */
    @Override
    public Orientation getOrientation() {
        return Orientation.ROTATE_0;
    }

    /**
     * @return Null.
     */
    @Override
    public byte[] getXMP() {
        return null;
    }

    /**
     * @return Null.
     */
    @Override
    public String getXMPRDF() {
        return null;
    }

}
