package edu.illinois.library.cantaloupe.processor.codec.tiff;

import edu.illinois.library.cantaloupe.image.exif.Directory;
import edu.illinois.library.cantaloupe.image.iptc.DataSet;
import edu.illinois.library.cantaloupe.image.iptc.Reader;
import edu.illinois.library.cantaloupe.image.xmp.Utils;
import edu.illinois.library.cantaloupe.processor.codec.IIOMetadata;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOInvalidTreeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * @see <a href="http://www.digitalpreservation.gov/formats/content/tiff_tags.shtml">
 *      Tags for TIFF, DNG, and Related Specifications</a>
 */
class TIFFMetadata extends IIOMetadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TIFFMetadata.class);

    static final int IPTC_TAG_NUMBER = 33723;
    static final int XMP_TAG_NUMBER = 700;

    private boolean checkedForEXIF, checkedForXMP;

    /**
     * Set by constructor.
     */
    private TIFFDirectory ifd;

    TIFFMetadata(javax.imageio.metadata.IIOMetadata metadata,
                 String formatName) {
        super(metadata, formatName);
        try {
            ifd = TIFFDirectory.createFromMetadata(iioMetadata);
        } catch (IIOInvalidTreeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public Optional<Directory> getEXIF() {
        if (!checkedForEXIF) {
            checkedForEXIF = true;
            // Without checking, we don't know if IFD0 contains an EXIF
            // sub-IFD, but it doesn't matter.
            exif = Directory.fromTIFFDirectory(ifd);
        }
        return Optional.ofNullable(exif);
    }

    @Override
    public Optional<List<DataSet>> getIPTC() {
        TIFFField field = ifd.getTIFFField(IPTC_TAG_NUMBER);
        if (field != null) {
            try (Reader reader = new Reader()) {
                // The data returned by the GeoSolutions TIFF reader is
                // expected to be a byte array, but sometimes it's an array of
                // longs. Converting the long array to a byte array and passing
                // it to the reader typically results in zero data sets read.
                Object data = field.getData();
                if (data instanceof byte[]) {
                    reader.setSource((byte[]) data);
                    iptcDataSets = reader.read();
                }
            } catch (IOException e) {
                LOGGER.info("getIPTC(): {}", e.getMessage(), e);
            }
        }
        return Optional.ofNullable(iptcDataSets);
    }

    /**
     * This override returns empty. Although TIFF has lots of native metadata,
     * most of the baseline tags have been integrated into the {@link
     * #getEXIF() EXIF standard}; and since tags in non-EXIF sub-IFDs are not
     * supported, that leaves not much left for this method to do.
     */
    @Override
    public Optional<Object> getNativeMetadata() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getXMP() {
        if (!checkedForXMP) {
            checkedForXMP = true;
            final TIFFField xmpField = getXMPField();
            if (xmpField != null) {
                xmp = new String(
                        (byte[]) xmpField.getData(),
                        StandardCharsets.UTF_8);
                xmp = Utils.trimXMP(xmp);
            }
        }
        return Optional.ofNullable(xmp);
    }

    private TIFFField getXMPField() {
        return ifd.getTIFFField(XMP_TAG_NUMBER);
    }

}
