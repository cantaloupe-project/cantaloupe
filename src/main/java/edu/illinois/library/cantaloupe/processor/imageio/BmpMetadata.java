package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.processor.Orientation;

import javax.imageio.metadata.IIOMetadata;

class BmpMetadata extends AbstractMetadata implements Metadata {

    /**
     * @param metadata
     * @param formatName
     */
    BmpMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return Null, as BMP does not support EXIF.
     */
    @Override
    public Object getExif() {
        return null;
    }

    /**
     * @return Null, as BMP does not support IPTC.
     */
    @Override
    public Object getIptc() {
        return null;
    }

    /**
     * @return {@link Orientation#ROTATE_0}
     */
    @Override
    public Orientation getOrientation() {
        return Orientation.ROTATE_0;
    }

    /**
     * @return Null, as BMP does not support XMP.
     */
    @Override
    public String getXmp() {
        return null;
    }

}
