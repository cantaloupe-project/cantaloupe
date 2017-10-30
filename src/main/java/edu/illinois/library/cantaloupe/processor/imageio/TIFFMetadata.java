package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.Orientation;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @see <a href="http://www.digitalpreservation.gov/formats/content/tiff_tags.shtml">
 *      Tags for TIFF, DNG, and Related Specifications</a>
 */
class TIFFMetadata extends AbstractMetadata implements Metadata {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(TIFFMetadata.class);

    /**
     * Native TIFF tags to preserve from the baseline IFD by
     * getNativeMetadata().
     */
    private static final Set<Integer> baselineNativeTagsToPreserve =
            new HashSet<>(Arrays.asList(
                    BaselineTIFFTagSet.TAG_ARTIST,
                    BaselineTIFFTagSet.TAG_COPYRIGHT,
                    BaselineTIFFTagSet.TAG_DATE_TIME,
                    BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION,
                    BaselineTIFFTagSet.TAG_MAKE,
                    BaselineTIFFTagSet.TAG_MODEL,
                    BaselineTIFFTagSet.TAG_SOFTWARE));

    private boolean checkedForExif = false;
    private boolean checkedForNativeMetadata = false;
    private boolean checkedForXmp = false;

    /** Cached by getEXIF() */
    private TIFFField exif;

    /** Set by constructor. */
    private TIFFDirectory ifd;

    private List<TIFFField> nativeMetadata = new ArrayList<>();

    /** Cached by getOrientation() */
    private Orientation orientation;

    /** Cached by getXMP() */
    private byte[] xmp;

    /**
     * @param metadata
     * @param formatName
     */
    TIFFMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
        try {
            ifd = TIFFDirectory.createFromMetadata(getIIOMetadata());
        } catch (IIOInvalidTreeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public TIFFField getEXIF() {
        if (!checkedForExif) {
            checkedForExif = true;
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
        return ifd.getTIFFField(33723);
    }

    /**
     * @return Native TIFF metadata.
     */
    List<TIFFField> getNativeMetadata() {
        if (!checkedForNativeMetadata) {
            checkedForNativeMetadata = true;
            // Copy the baseline tags from the source base IFD into the
            // derivative base IFD.
            for (Integer tagNumber : baselineNativeTagsToPreserve) {
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
                orientation = orientationForExifValue(orientationField.getAsInt(0));
            }
            if (orientation == null) {
                orientation = Orientation.ROTATE_0;
            }
        }
        return orientation;
    }

    @Override
    public byte[] getXMP() {
        if (!checkedForXmp) {
            checkedForXmp = true;
            final TIFFField xmpField = getXmpField();
            if (xmpField != null) {
                xmp = (byte[]) xmpField.getData();
            }
        }
        return xmp;
    }

    TIFFField getXmpField() {
        return ifd.getTIFFField(700);
    }

    @Override
    public String getXMPRDF() {
        final byte[] xmpData = getXMP();
        if (xmpData != null) {
            try {
                final String xmp = new String(xmpData, "UTF-8");
                // Trim off the junk
                final int start = xmp.indexOf("<rdf:RDF");
                final int end = xmp.indexOf("</rdf:RDF");
                return xmp.substring(start, end + 10);
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("getXMPRDF(): {}", e.getMessage());
            }
        }
        return null;
    }

}
