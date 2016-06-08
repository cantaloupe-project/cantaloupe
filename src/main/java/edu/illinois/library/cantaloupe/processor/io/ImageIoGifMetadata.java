package edu.illinois.library.cantaloupe.processor.io;

import javax.imageio.metadata.IIOMetadata;

class ImageIoGifMetadata extends AbstractImageIoMetadata
        implements ImageIoMetadata {

    /**
     * @param metadata
     * @param formatName
     */
    public ImageIoGifMetadata(IIOMetadata metadata, String formatName) {
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
     * @return
     */
    @Override
    public Object getXmp() {
        return null;
    }

}
