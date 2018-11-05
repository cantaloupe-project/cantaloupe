package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.util.StringUtils;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOInvalidTreeException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @see <a href="http://www.digitalpreservation.gov/formats/content/tiff_tags.shtml">
 *      Tags for TIFF, DNG, and Related Specifications</a>
 */
class TIFFMetadata extends IIOMetadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TIFFMetadata.class);

    static final int IPTC_TAG_NUMBER = 33723;
    static final int XMP_TAG_NUMBER  = 700;

    /**
     * Native TIFF tags to preserve from the baseline IFD by
     * getNativeMetadata().
     */
    private static final Set<Integer> BASELINE_NATIVE_TAGS_TO_PRESERVE =
            new HashSet<>(Arrays.asList(
                    BaselineTIFFTagSet.TAG_ARTIST,
                    BaselineTIFFTagSet.TAG_COPYRIGHT,
                    BaselineTIFFTagSet.TAG_DATE_TIME,
                    BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION,
                    BaselineTIFFTagSet.TAG_MAKE,
                    BaselineTIFFTagSet.TAG_MODEL,
                    BaselineTIFFTagSet.TAG_SOFTWARE));

    private boolean checkedForEXIF;
    private boolean checkedForNativeMetadata;
    private boolean checkedForXMP;

    /**
     * Cached by {@link #getEXIF()}.
     */
    private TIFFField exif;

    /**
     * Set by constructor.
     */
    private TIFFDirectory ifd;

    private final List<TIFFField> nativeMetadata = new ArrayList<>();

    /**
     * Cached by {@link #getOrientation()}.
     */
    private Orientation orientation;

    /**
     * Cached by {@link #getXMP()}.
     */
    private String xmp;

    TIFFMetadata(javax.imageio.metadata.IIOMetadata metadata,
                 String formatName) {
        super(metadata, formatName);
        try {
            ifd = TIFFDirectory.createFromMetadata(getIIOMetadata());
        } catch (IIOInvalidTreeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public TIFFField getEXIF() {
        if (!checkedForEXIF) {
            checkedForEXIF = true;
            final TIFFField srcExifField =
                    ifd.getTIFFField(EXIFParentTIFFTagSet.TAG_EXIF_IFD_POINTER);
            if (srcExifField != null) {
                final TIFFDirectory srcExifDir = (TIFFDirectory) srcExifField.getData();
                if (srcExifDir != null) {
                    exif = srcExifField;
                }
            }
        }
        return exif;
    }

    @Override
    public TIFFField getIPTC() {
        return ifd.getTIFFField(IPTC_TAG_NUMBER);
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * @return Native TIFF metadata.
     */
    List<TIFFField> getNativeMetadata() {
        if (!checkedForNativeMetadata) {
            checkedForNativeMetadata = true;
            // Copy the baseline tags from the source base IFD into the
            // derivative base IFD.
            for (Integer tagNumber : BASELINE_NATIVE_TAGS_TO_PRESERVE) {
                final TIFFField srcField = ifd.getTIFFField(tagNumber);
                if (srcField != null) {
                    nativeMetadata.add(srcField);
                }
            }
        }
        return nativeMetadata;
    }

    /**
     * @return Orientation from the metadata.
     */
    @Override
    public Orientation getOrientation() {
        if (orientation == null) {
            final TIFFField orientationField = ifd.getTIFFField(274);
            if (orientationField != null) {
                orientation = Util.orientationForExifValue(orientationField.getAsInt(0));
            }
            if (orientation == null) {
                orientation = Orientation.ROTATE_0;
            }
        }
        return orientation;
    }

    @Override
    public String getXMP() {
        if (!checkedForXMP) {
            checkedForXMP = true;
            final TIFFField xmpField = getXMPField();
            if (xmpField != null) {
                xmp = new String(
                        (byte[]) xmpField.getData(),
                        StandardCharsets.UTF_8);
                xmp = StringUtils.trimXMP(xmp);
            }
        }
        return xmp;
    }

    TIFFField getXMPField() {
        return ifd.getTIFFField(XMP_TAG_NUMBER);
    }

}
