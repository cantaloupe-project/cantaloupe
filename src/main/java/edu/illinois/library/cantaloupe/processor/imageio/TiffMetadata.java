package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.processor.Orientation;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class TiffMetadata extends AbstractMetadata
        implements Metadata {

    private static Logger logger = LoggerFactory.
            getLogger(TiffMetadata.class);

    private TIFFDirectory ifd;

    /**
     * @param metadata
     * @param formatName
     */
    TiffMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
        try {
            ifd = TIFFDirectory.createFromMetadata(getIioMetadata());
        } catch (IIOInvalidTreeException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public TIFFField getExif() {
        final TIFFField srcExifField =
                ifd.getTIFFField(EXIFParentTIFFTagSet.TAG_EXIF_IFD_POINTER);
        if (srcExifField != null) {
            final TIFFDirectory srcExifDir = (TIFFDirectory) srcExifField.getData();
            if (srcExifDir != null) {
                return srcExifField;
            }
        }
        return null;
    }

    @Override
    public TIFFField getIptc() {
        return ifd.getTIFFField(33723);
    }

    /**
     * @return Native TIFF metadata.
     */
    public List<TIFFField> getNativeMetadata() {
        final List<TIFFField> fields = new ArrayList<>();

        // Tags to preserve from the baseline IFD. EXIF metadata resides in
        // a separate IFD, so this does not include any EXIF tags.
        final Set<Integer> baselineTagsToPreserve = new HashSet<>(Arrays.asList(
                BaselineTIFFTagSet.TAG_ARTIST,
                BaselineTIFFTagSet.TAG_COPYRIGHT,
                BaselineTIFFTagSet.TAG_DATE_TIME,
                BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION,
                BaselineTIFFTagSet.TAG_MAKE,
                BaselineTIFFTagSet.TAG_MODEL,
                BaselineTIFFTagSet.TAG_SOFTWARE));

        // Copy the baseline tags from above from the source base IFD into
        // the derivative base IFD.
        for (Object tagNumber : baselineTagsToPreserve) {
            final TIFFField srcField = ifd.getTIFFField((Integer) tagNumber);
            if (srcField != null) {
                fields.add(srcField);
            }
        }
        return fields;
    }

    /**
     * @return Orientation from the metadata.
     */
    @Override
    public Orientation getOrientation() {
        // Check EXIF.
        final TIFFField orientationField = ifd.getTIFFField(274);
        if (orientationField != null) {
            return orientationForExifValue(orientationField.getAsInt(0));
        }
        return Orientation.ROTATE_0;
    }

    @Override
    public TIFFField getXmp() {
        return ifd.getTIFFField(700);
    }

}
