package edu.illinois.library.cantaloupe.processor.codec;

import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import edu.illinois.library.cantaloupe.image.Orientation;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

abstract class AbstractMetadata {

    private String formatName;
    private IIOMetadata iioMetadata;

    AbstractMetadata(IIOMetadata metadata, String format) {
        iioMetadata = metadata;
        formatName = format;
    }

    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) getIIOMetadata().getAsTree(getFormatName());
    }

    private String getFormatName() {
        return formatName;
    }

    public IIOMetadata getIIOMetadata() {
        return iioMetadata;
    }

    abstract Logger getLogger();

    abstract byte[] getXMP();

    String getXMPRDF() {
        final byte[] xmpData = getXMP();
        if (xmpData != null) {
            final String xmp = new String(xmpData, StandardCharsets.UTF_8);
            final int start = xmp.indexOf("<rdf:RDF");
            final int end = xmp.indexOf("</rdf:RDF");
            return (start >= 0) ? xmp.substring(start, end + 10) : xmp;
        }
        return null;
    }

    Orientation orientationForExifValue(int value) {
        switch (value) {
            case 6:
                return Orientation.ROTATE_90;
            case 3:
                return Orientation.ROTATE_180;
            case 8:
                return Orientation.ROTATE_270;
            default:
                return Orientation.ROTATE_0;
        }
    }

    /**
     * Reads the orientation (tiff:Orientation) from EXIF data.
     *
     * @param exif EXIF data.
     * @return Orientation, or {@link Orientation#ROTATE_0} if unspecified.
     */
    Orientation readOrientation(byte[] exif) {
        // See https://community.oracle.com/thread/1264022?start=0&tstart=0
        // for an explanation of the technique used here.
        if (exif != null) {
            final String preferredImpl =
                    new TIFFImageReader().getPreferredIIOImplementations()[0];
            final Iterator<ImageReader> it =
                    ImageIO.getImageReadersByFormatName("TIFF");
            while (it.hasNext()) {
                final ImageReader reader = it.next();
                if (reader.getClass().getName().equals(preferredImpl)) {
                    try (ImageInputStream wrapper = new MemoryCacheImageInputStream(
                            new ByteArrayInputStream(exif, 6, exif.length - 6))) {
                        reader.setInput(wrapper, true, false);

                        final IIOMetadata exifMetadata = reader.getImageMetadata(0);
                        final TIFFDirectory exifDir =
                                TIFFDirectory.createFromMetadata(exifMetadata);
                        final TIFFField orientationField = exifDir.getTIFFField(274);
                        if (orientationField != null) {
                            return orientationForExifValue(orientationField.getAsInt(0));
                        }
                    } catch (IOException e) {
                        getLogger().info(e.getMessage(), e);
                    } finally {
                        reader.dispose();
                    }
                }
            }
        }
        return Orientation.ROTATE_0;
    }

    /**
     * Reads the orientation (tiff:Orientation) from an XMP string.
     *
     * @param xmp XMP string.
     * @return Orientation, or null if unspecified.
     */
    Orientation readOrientation(String xmp) {
        RIOT.init();

        final Model model = ModelFactory.createDefaultModel();

        try (StringReader reader = new StringReader(xmp)) {
            model.read(reader, null, "RDF/XML");

            final NodeIterator it = model.listObjectsOfProperty(
                    model.createProperty("http://ns.adobe.com/tiff/1.0/Orientation"));
            if (it.hasNext()) {
                final int orientationValue =
                        Integer.parseInt(it.next().asLiteral().getString());
                return orientationForExifValue(orientationValue);
            }
        } catch (RiotException e) {
            // The XMP string is invalid RDF/XML. Not much we can do.
            getLogger().info(e.getMessage());
        }
        return null;
    }

}
