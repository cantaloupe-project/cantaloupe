package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

final class TIFFImageReader extends AbstractImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TIFFImageReader.class);

    static {
        // See: https://github.com/geosolutions-it/imageio-ext/wiki/TIFF-plugin
        System.setProperty("it.geosolutions.imageio.tiff.lazy", "true");
    }

    /**
     * @param sourceFile Source file to read.
     */
    TIFFImageReader(Path sourceFile) throws IOException {
        super(sourceFile, Format.TIF);
    }

    /**
     * @param inputStream Stream to read.
     */
    TIFFImageReader(ImageInputStream inputStream) throws IOException {
        super(inputStream, Format.TIF);
    }

    /**
     * @param streamSource Source of streams to read.
     */
    TIFFImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.TIF);
    }

    @Override
    Compression getCompression(int imageIndex) throws IOException {
        String compStr = "";
        final IIOMetadataNode node = getMetadata(0).getAsTree();
        final NodeList fields = node.getElementsByTagName("TIFFField");
        for (int i = 0; i < fields.getLength(); i++) {
            if ("259".equals(fields.item(i).getAttributes().getNamedItem("number").getNodeValue())) {
                compStr = fields.item(i).getChildNodes().item(0).
                        getChildNodes().item(0).getAttributes().
                        getNamedItem("description").getNodeValue();
                break;
            }
        }

        switch (compStr) {
            case "JPEG":
                return Compression.JPEG;
            case "LZW":
                return Compression.LZW;
            case "PackBits":
                return Compression.RLE;
            case "Uncompressed":
                return Compression.UNCOMPRESSED;
            case "ZLib":
                return Compression.DEFLATE;
            default:
                return Compression.UNDEFINED;
        }
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new TIFFMetadata(metadata, metadataFormat);
    }

    @Override
    String[] preferredIIOImplementations() {
        // N.B.: The GeoSolutions TIFF reader supports BigTIFF among other
        // enhancements. The Sun reader will do as a fallback, although there
        // shouldn't be any need to fall back.
        String[] impls = new String[2];
        impls[0] = it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader.class.getName();

        // In Java 9, the Sun TIFF reader has moved out of the JAI ImageIO
        // Tools and into the JDK.
        if (SystemUtils.getJavaMajorVersion() >= 9) {
            impls[1] = "com.sun.imageio.plugins.tiff.TIFFImageReader";
        } else {
            impls[1] = "com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader";
        }

        return impls;
    }

    /**
     * <p>Override that is both multi-resolution- and tile-aware.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require additional cropping.</p>
     *
     * @param ops
     * @param orientation     Orientation of the source image data as reported
     *                        by e.g. embedded metadata.
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return                Image best matching the given arguments. Clients
     *                        should check the hints set to see whether they
     *                        need to perform additional cropping.
     */
    @Override
    public BufferedImage read(final OperationList ops,
                              final Orientation orientation,
                              final ReductionFactor reductionFactor,
                              final Set<ImageReader.Hint> hints)
            throws IOException, ProcessorException {
        Crop crop = (Crop) ops.getFirst(Crop.class);
        if (crop == null) {
            crop = new Crop();
            crop.setFull(true);
        }

        Scale scale = (Scale) ops.getFirst(Scale.class);
        if (scale == null) {
            scale = new Scale();
        }

        BufferedImage image;
        if (hints != null && hints.contains(ImageReader.Hint.IGNORE_CROP)) {
            image = read();
        } else {
            image = readSmallestUsableSubimage(crop, scale, reductionFactor,
                    hints);
        }
        if (image == null) {
            throw new UnsupportedSourceFormatException(iioReader.getFormatName());
        }
        return image;
    }

}
