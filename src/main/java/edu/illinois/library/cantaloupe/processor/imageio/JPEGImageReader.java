package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

final class JPEGImageReader extends AbstractImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEGImageReader.class);

    static String[] getPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.jpeg.JPEGImageReader" };
    }

    /**
     * @param sourceFile Source file to read.
     */
    JPEGImageReader(Path sourceFile) throws IOException {
        super(sourceFile, Format.JPG);
    }

    /**
     * @param inputStream Stream to read.
     */
    JPEGImageReader(ImageInputStream inputStream) throws IOException {
        super(inputStream, Format.JPG);
    }

    /**
     * @param streamSource Source of streams to read.
     */
    JPEGImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.JPG);
    }

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.JPEG;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    JPEGMetadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new JPEGMetadata(metadata, metadataFormat);
    }

    @Override
    String[] preferredIIOImplementations() {
        return getPreferredIIOImplementations();
    }

    /**
     * <p>Override to handle images with incompatible ICC profiles.</p>
     *
     * {@inheritDoc}
     */
    @Override
    BufferedImage read(final OperationList ops,
                       final Orientation orientation,
                       final ReductionFactor reductionFactor,
                       final Set<ImageReader.Hint> hints)
            throws IOException, ProcessorException {
        BufferedImage image;

        Crop crop = (Crop) ops.getFirst(Crop.class);
        if (crop == null || hints.contains(ImageReader.Hint.IGNORE_CROP)) {
            crop = new Crop();
            crop.setFull(true);
        }

        Dimension fullSize = new Dimension(
                iioReader.getWidth(0),
                iioReader.getHeight(0));
        image = readRegion(crop.getRectangle(fullSize), hints);

        if (image == null) {
            throw new UnsupportedSourceFormatException(iioReader.getFormatName());
        }

        return image;
    }

    private BufferedImage readRegion(final Rectangle region,
                                     final Set<ImageReader.Hint> hints)
            throws IOException {
        final Dimension imageSize = getSize();

        getLogger().debug("Acquiring region {},{}/{}x{} from {}x{} image",
                region.x, region.y, region.width, region.height,
                imageSize.width, imageSize.height);

        hints.add(ImageReader.Hint.ALREADY_CROPPED);
        final ImageReadParam param = iioReader.getDefaultReadParam();
        param.setSourceRegion(region);

        BufferedImage image;

        try {
            image = iioReader.read(0, param);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Numbers of source Raster bands " +
                    "and source color space components do not match")) {
                /*
                This probably means that the embedded ICC profile is
                incompatible with the source image data.
                (The Sun JPEGImageReader is not very lenient.)
                See: https://github.com/medusa-project/cantaloupe/issues/41
                and also the class doc.

                To deal with this, we will try reading again, ignoring the
                color profile. We need to reset the reader, and then read into
                a grayscale BufferedImage.

                Credit/blame for this goes to:
                http://stackoverflow.com/a/11571181
                */
                reset();

                final Iterator<ImageTypeSpecifier> imageTypes =
                        iioReader.getImageTypes(0);
                while (imageTypes.hasNext()) {
                    final ImageTypeSpecifier imageTypeSpecifier = imageTypes.next();
                    final int bufferedImageType = imageTypeSpecifier.getBufferedImageType();
                    if (bufferedImageType == BufferedImage.TYPE_BYTE_GRAY) {
                        param.setDestinationType(imageTypeSpecifier);
                        break;
                    }
                }
                image = iioReader.read(0, param);
            } else {
                throw e;
            }
        }
        return image;
    }

}
