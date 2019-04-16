package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.image.exif.Directory;
import edu.illinois.library.cantaloupe.image.iptc.DataSet;
import edu.illinois.library.cantaloupe.processor.codec.IIOMetadata;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadataNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html">
 *      JPEG Metadata Format Specification and Usage Notes</a>
 */
class JPEGMetadata extends IIOMetadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEGMetadata.class);

    private boolean checkedForEXIF, checkedForIPTC, checkedForXMP;

    JPEGMetadata(javax.imageio.metadata.IIOMetadata metadata,
                 String formatName) {
        super(metadata, formatName);
    }

    @Override
    public Optional<Directory> getEXIF() {
        if (!checkedForEXIF) {
            checkedForEXIF = true;
            // EXIF and XMP metadata both appear in the IIOMetadataNode tree as
            // identical nodes at /markerSequence/unknown[@MarkerTag=225]
            final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                    getElementsByTagName("markerSequence").item(0);
            final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
                if ("225".equals(marker.getAttribute("MarkerTag"))) { // APP1
                    byte[] data = (byte[]) marker.getUserObject();
                    // Check the first byte to see whether it's EXIF or XMP.
                    if (data[0] == 69) {
                        try (edu.illinois.library.cantaloupe.image.exif.Reader reader =
                                     new edu.illinois.library.cantaloupe.image.exif.Reader()) {
                            reader.setSource(data);
                            exif = reader.read();
                            break;
                        } catch (IOException e) {
                            LOGGER.info("getEXIF(): {}", e.getMessage(), e);
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(exif);
    }

    @Override
    public Optional<List<DataSet>> getIPTC() {
        if (!checkedForIPTC) {
            checkedForIPTC = true;
            // IPTC metadata appears in the IIOMetadataNode tree at
            // /markerSequence/unknown[@MarkerTag=237]
            final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                    getElementsByTagName("markerSequence").item(0);
            final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
                if ("237".equals(marker.getAttribute("MarkerTag"))) { // APP13
                    // This data includes the segment marker, which must be
                    // trimmed off.
                    byte[] data = (byte[]) marker.getUserObject();
                    if (Util.isPhotoshopSegment(data)) {
                        data = Arrays.copyOfRange(data, 26, data.length);
                        try (edu.illinois.library.cantaloupe.image.iptc.Reader reader =
                                     new edu.illinois.library.cantaloupe.image.iptc.Reader()) {
                            reader.setSource(data);
                            iptcDataSets = reader.read();
                            break;
                        } catch (IOException e) {
                            LOGGER.info("getIPTC(): {}", e.getMessage(), e);
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(iptcDataSets);
    }

    @Override
    public Optional<String> getXMP() {
        if (!checkedForXMP) {
            checkedForXMP = true;
            // EXIF and XMP metadata both appear in the IIOMetadataNode tree as
            // identical nodes at /markerSequence/unknown[@MarkerTag=225].
            // There may be multiple XMP nodes. (See the Adobe XMP
            // Specification Part 3.)
            final IIOMetadataNode markerSequence = (IIOMetadataNode) getAsTree().
                    getElementsByTagName("markerSequence").item(0);
            final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
                if ("225".equals(marker.getAttribute("MarkerTag"))) {
                    byte[] data = (byte[]) marker.getUserObject();
                    // Check the first byte to see whether it's EXIF or XMP.
                    if (data[0] == 104) {
                        xmp = new String(data, StandardCharsets.UTF_8);
                        xmp = StringUtils.trimXMP(xmp);
                        break;
                    }
                }
            }
        }
        return Optional.ofNullable(xmp);
    }

}
