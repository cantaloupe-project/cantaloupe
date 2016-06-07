package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MetadataCopy;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.icc.IccProfile;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * TIFF image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * TIFFs.
 *
 * @see <a href="http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html">
 *     ImageIO TIFF Plugin Documentation</a>
 * @see <a href="http://www.digitalpreservation.gov/formats/content/tiff_tags.shtml">
 *     Tags for TIFF, DNG, and Related Specifications</a>
 * @see <a href="http://www.color.org/icc_specs2.xalter">ICC Specifications</a>
 */
class ImageIoTiffImageWriter extends AbstractImageIoImageWriter {

    static final String TIF_COMPRESSION_CONFIG_KEY = // TODO: fix w/ java 2d
            "JaiProcessor.tif.compression";

    ImageIoTiffImageWriter(OperationList opList) {
        super(opList);
    }

    ImageIoTiffImageWriter(OperationList opList,
                           ImageIoMetadata sourceMetadata) {
        super(opList, sourceMetadata);
    }

    /**
     * No-op.
     *
     * @see {@link #addIccProfile(IIOMetadata, IccProfile)}
     */
    @Override
    protected void addIccProfile(final IIOMetadataNode baseNode,
                                 final IccProfile profile) {}

    /**
     * @param baseMetadata Metadata to embed the profile into.
     * @param profile Profile to embed.
     * @return Metadata with ICC profile embedded.
     * @throws IOException
     */
    private IIOMetadata addIccProfile(final IIOMetadata baseMetadata,
                                      final IccProfile profile)
            throws IOException {
        if (profile != null) {
            final TIFFDirectory dir =
                    TIFFDirectory.createFromMetadata(baseMetadata);
            final BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
            final TIFFTag iccTag = base.getTag(BaselineTIFFTagSet.TAG_ICC_PROFILE);
            final ICC_ColorSpace colorSpace =
                    new ICC_ColorSpace(profile.getProfile());

            final byte[] data = colorSpace.getProfile().getData();
            final TIFFField iccField = new TIFFField(
                    iccTag, TIFFTag.TIFF_UNDEFINED, data.length, data);
            dir.addTIFFField(iccField);
            return dir.getAsMetadata();
        }
        return baseMetadata;
    }

    /**
     * No-op.
     *
     * @see {@link #addMetadata(IIOMetadata, IIOMetadata)}
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseNode) {}

    /**
     * @param sourceMetadata
     * @param derivativeMetadata
     * @return
     * @throws IOException
     */
    private IIOMetadata addMetadata(final IIOMetadata sourceMetadata,
                                    final IIOMetadata derivativeMetadata)
            throws IOException {
        final TIFFDirectory srcDir =
                TIFFDirectory.createFromMetadata(sourceMetadata);
        final TIFFDirectory destDir =
                TIFFDirectory.createFromMetadata(derivativeMetadata);

        // Tags to preserve from the baseline IFD. EXIF metadata resides in a
        // separate IFD, so this does not include any EXIF tags.
        final Set<Integer> baselineTagsToPreserve = new HashSet<>(Arrays.asList(
                BaselineTIFFTagSet.TAG_ARTIST,
                BaselineTIFFTagSet.TAG_COPYRIGHT,
                BaselineTIFFTagSet.TAG_DATE_TIME,
                BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION,
                BaselineTIFFTagSet.TAG_MAKE,
                BaselineTIFFTagSet.TAG_MODEL,
                BaselineTIFFTagSet.TAG_SOFTWARE,
                700, // XMP
                33723 // IPTC
        ));

        // Copy the baseline tags from above from the source metadata into the
        // derivative metadata.
        for (Object tagNumber : baselineTagsToPreserve) {
            final TIFFField srcField = srcDir.getTIFFField((Integer) tagNumber);
            if (srcField != null) {
                destDir.addTIFFField(srcField);
            }
        }

        // Copy the EXIF IFD from the source metadata, if present.
        final TIFFField srcExifField =
                srcDir.getTIFFField(EXIFParentTIFFTagSet.TAG_EXIF_IFD_POINTER);
        if (srcExifField != null) {
            final TIFFDirectory srcExifDir = (TIFFDirectory) srcExifField.getData();
            if (srcExifDir != null) {
                destDir.addTIFFField(srcExifField);
            }
        }

        return destDir.getAsMetadata();
    }

    /**
     * @param writer Writer to obtain the default metadata from.
     * @param writeParam Write parameters on which to base the metadata.
     * @param image Image to apply the metadata to.
     * @return Image metadata with added metadata corresponding to any
     *         writer-specific operations applied.
     * @throws IOException
     */
    @Override
    protected IIOMetadata getMetadata(final ImageWriter writer,
                            final ImageWriteParam writeParam,
                            final RenderedImage image) throws IOException {
        IIOMetadata derivativeMetadata = writer.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(image),
                writeParam);
        for (final Operation op : opList) {
            if (op instanceof IccProfile) {
                derivativeMetadata =
                        addIccProfile(derivativeMetadata, (IccProfile) op);
            } else if (op instanceof MetadataCopy && sourceMetadata != null) {
                derivativeMetadata = addMetadata(
                        sourceMetadata.getIioMetadata(),
                        derivativeMetadata);
            }
        }
        return derivativeMetadata;
    }

    /**
     * @param writer Writer to abtain parameters for
     * @return Write parameters respecting the application configuration.
     */
    private ImageWriteParam getWriteParam(ImageWriter writer) {
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        final Configuration config = Configuration.getInstance();
        final String compressionType = config.getString(
                TIF_COMPRESSION_CONFIG_KEY);
        if (compressionType != null) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType(compressionType);
        }
        return writeParam;
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(BufferedImage image, final OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.TIF.getPreferredMediaType().toString());
        final ImageWriter writer = writers.next();
        try {
            final ImageWriteParam writeParam = getWriteParam(writer);
            final IIOMetadata metadata = getMetadata(writer, writeParam, image);
            final IIOImage iioImage = new IIOImage(image, null, metadata);
            final ImageOutputStream ios =
                    ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);
            writer.write(metadata, iioImage, writeParam);
            ios.flush(); // http://stackoverflow.com/a/14489406
        } finally {
            writer.dispose();
        }
    }

    /**
     * Writes a JAI {@link PlanarImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(PlanarImage image, OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.TIF.getPreferredMediaType().toString());
        final ImageWriter writer = writers.next();
        try {
            final ImageWriteParam writeParam = getWriteParam(writer);
            final IIOMetadata metadata = getMetadata(writer, writeParam, image);
            final IIOImage iioImage = new IIOImage(image, null, metadata);
            final ImageOutputStream ios =
                    ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);
            writer.write(null, iioImage, writeParam);
            ios.flush(); // http://stackoverflow.com/a/14489406
        } finally {
            writer.dispose();
        }
    }

}
