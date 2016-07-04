package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MetadataCopy;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.icc.IccProfile;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
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
import java.util.Iterator;

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
class TiffImageWriter extends AbstractImageWriter {

    static final String JAI_TIF_COMPRESSION_CONFIG_KEY =
            "JaiProcessor.tif.compression";
    static final String JAVA2D_TIF_COMPRESSION_CONFIG_KEY =
            "Java2dProcessor.tif.compression";

    TiffImageWriter(OperationList opList) {
        super(opList);
    }

    TiffImageWriter(OperationList opList,
                    Metadata sourceMetadata) {
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
     * @see {@link #addMetadata(Metadata, IIOMetadata)}
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseNode) {}

    /**
     * @param sourceMetadata
     * @param derivativeMetadata
     * @return
     * @throws IOException
     */
    private IIOMetadata addMetadata(final Metadata sourceMetadata,
                                    IIOMetadata derivativeMetadata)
            throws IOException {
        if (sourceMetadata instanceof TiffMetadata) {
            final TIFFDirectory destDir =
                    TIFFDirectory.createFromMetadata(derivativeMetadata);

            for (TIFFField field : ((TiffMetadata) sourceMetadata).getNativeMetadata()) {
                destDir.addTIFFField(field);
            }

            final TIFFField iptcField = (TIFFField) sourceMetadata.getIptc();
            if (iptcField != null) {
                destDir.addTIFFField(iptcField);
            }

            final TIFFField xmpField = (TIFFField) sourceMetadata.getXmp();
            if (xmpField != null) {
                destDir.addTIFFField(xmpField);
            }

            final TIFFField exifField = (TIFFField) sourceMetadata.getExif();
            if (exifField != null) {
                destDir.addTIFFField(exifField);
            }

            derivativeMetadata = destDir.getAsMetadata();
        }
        return derivativeMetadata;
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
                        sourceMetadata, derivativeMetadata);
            }
        }
        return derivativeMetadata;
    }

    /**
     * @param writer Writer to abtain parameters for
     * @return Write parameters respecting the application configuration.
     */
    private ImageWriteParam getJaiWriteParam(ImageWriter writer) {
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        final Configuration config = Configuration.getInstance();
        final String compressionType = config.getString(
                JAI_TIF_COMPRESSION_CONFIG_KEY);
        if (compressionType != null) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType(compressionType);
        }
        return writeParam;
    }

    /**
     * @param writer Writer to abtain parameters for
     * @return Write parameters respecting the application configuration.
     */
    private ImageWriteParam getJava2dWriteParam(ImageWriter writer) {
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        final Configuration config = Configuration.getInstance();
        final String compressionType = config.getString(
                JAVA2D_TIF_COMPRESSION_CONFIG_KEY);
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
        final Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType(
                Format.TIF.getPreferredMediaType().toString());
        while (it.hasNext()) {
            final ImageWriter writer = it.next();
            if (writer instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter) {
                try {
                    final ImageWriteParam writeParam = getJava2dWriteParam(writer);
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
                break;
            }
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
        final Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType(
                Format.TIF.getPreferredMediaType().toString());
        while (it.hasNext()) {
            final ImageWriter writer = it.next();
            if (writer instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter) {
                try {
                    final ImageWriteParam writeParam = getJaiWriteParam(writer);
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
                break;
            }
        }
    }

}
