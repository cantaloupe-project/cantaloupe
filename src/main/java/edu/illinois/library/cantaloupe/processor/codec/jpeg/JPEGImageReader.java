package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.source.stream.BufferedImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

public final class JPEGImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEGImageReader.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.jpg.reader";

    /**
     * N.B.: This file must exist in the resource bundle.
     */
    private static final String CMYK_ICC_PROFILE = "eciCMYK.icc";

    /**
     * @return ICC profile to use for CMYK images that do not contain an
     *         embedded profile.
     */
    private static ICC_Profile defaultCMYKProfile() throws IOException {
        try (InputStream is = new BufferedInputStream(
                JPEGImageReader.class.getResourceAsStream("/" + CMYK_ICC_PROFILE))) {
            return ICC_Profile.getInstance(is);
        }
    }

    @Override
    protected String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.jpeg.JPEGImageReader" };
    }

    @Override
    public Compression getCompression(int imageIndex) throws IOException {
        // Trigger any contract-required exceptions
        getSize(0);
        return Compression.JPEG;
    }

    @Override
    protected Format getFormat() {
        return Format.JPG;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public JPEGMetadata getMetadata(int imageIndex) throws IOException {
        // Trigger any contract-required exceptions
        getSize(0);
        try {
            final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
            final String metadataFormat = metadata.getNativeMetadataFormatName();
            return new JPEGMetadata(metadata, metadataFormat);
        } catch (IIOException e) {
            LOGGER.warn("getMetadata(): {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected String getUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

    /**
     * Expedient but not necessarily efficient method that reads a whole image
     * (excluding subimages) in one shot.
     */
    public BufferedImage read() throws IOException {
        BufferedImage image = null;
        try {
            image = super.read();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Numbers of source Raster bands " +
                    "and source color space components do not match")) {
                ImageReadParam readParam = iioReader.getDefaultReadParam();
                image = readGrayscaleWithIncompatibleICCProfile(readParam);
            } else {
                throw e;
            }
        } catch (IIOException e) {
            if ("Unsupported Image Type".equals(e.getMessage())) {
                ImageReadParam readParam = iioReader.getDefaultReadParam();
                image = readCMYK(readParam);
            } else {
                handle(e);
            }
        }
        return image;
    }

    /**
     * Override to handle images with incompatible ICC profiles, and also
     * CMYK images, neither of which the Sun JPEGImageReader can do.
     *
     * {@inheritDoc}
     */
    @Override
    public BufferedImage read(final OperationList ops,
                              final ReductionFactor reductionFactor,
                              final Set<ReaderHint> hints) throws IOException {
        BufferedImage image;

        Crop crop = (Crop) ops.getFirst(Crop.class);
        if (crop == null || hints.contains(ReaderHint.IGNORE_CROP)) {
            crop = new CropByPercent();
        }

        Dimension fullSize = getSize(0);
        image = readRegion(
                crop.getRectangle(fullSize, ops.getScaleConstraint()),
                hints);

        if (image == null) {
            throw new SourceFormatException(iioReader.getFormatName());
        }

        return image;
    }

    private BufferedImage readRegion(final Rectangle region,
                                     final Set<ReaderHint> hints) throws IOException {
        final Dimension imageSize = getSize(0);

        getLogger().debug("Acquiring region {},{}/{}x{} from {}x{} image",
                region.intX(), region.intY(),
                region.intWidth(), region.intHeight(),
                imageSize.intWidth(), imageSize.intHeight());

        hints.add(ReaderHint.ALREADY_CROPPED);
        final ImageReadParam param = iioReader.getDefaultReadParam();
        param.setSourceRegion(region.toAWTRectangle());

        BufferedImage image = null;
        try {
            image = iioReader.read(0, param);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Numbers of source Raster bands " +
                    "and source color space components do not match")) {
                image = readGrayscaleWithIncompatibleICCProfile(param);
            } else {
                throw e;
            }
        } catch (IIOException e) {
            if ("Unsupported Image Type".equals(e.getMessage())) {
                image = readCMYK(param);
            } else {
                handle(e);
            }
        }
        return image;
    }

    /**
     * Used for images whose embedded ICC profile is incompatible with the
     * source image data. (The Sun JPEGImageReader is not very lenient.)
     *
     * @see <a href="https://github.com/cantaloupe-project/cantaloupe/issues/41">
     *     GitHub issue</a>
     */
    private BufferedImage readGrayscaleWithIncompatibleICCProfile(ImageReadParam readParam)
            throws IOException {
        /*
        To deal with this, we will try reading again, ignoring the embedded
        ICC profile. We need to reset the reader, and then read into a
        grayscale BufferedImage.

        Credit/blame for this technique goes to:
        http://stackoverflow.com/a/11571181
        */
        reset();

        final Iterator<ImageTypeSpecifier> imageTypes = iioReader.getImageTypes(0);
        while (imageTypes.hasNext()) {
            final ImageTypeSpecifier imageTypeSpecifier = imageTypes.next();
            final int bufferedImageType = imageTypeSpecifier.getBufferedImageType();
            if (bufferedImageType == BufferedImage.TYPE_BYTE_GRAY) {
                readParam.setDestinationType(imageTypeSpecifier);
                break;
            }
        }
        return iioReader.read(0, readParam);
    }

    /**
     * Reads an image with CMYK color, which the Sun JPEGImageReader doesn't
     * support, as of Java 9.
     */
    private BufferedImage readCMYK(ImageReadParam readParam) throws IOException {
        /*
        The steps involved here are:

        1. Reset the reader, acquiring a fresh input stream
        2. Wrap the input stream in a buffered input stream so that it can be
           reset in a later step
        3. Extract the profile and color info from the image NOT using ImageIO
        4. Reset the stream
        5. Read the image into a Raster using ImageIO
        6. If the image is YCCK, convert it to CMYK
        7. If the image has an Adobe APP14 marker segment, invert its colors
        8. Convert the CMYK Raster to an RGB BufferedImage

        Credit/blame for this technique goes to:
        https://stackoverflow.com/a/12132556/177529
        */
        reset();

        /*
        inputStream may not support mark()/reset(), so wrap it in a
        BufferedInputStream which does. The buffer is large enough to
        accommodate a potentially large header (including thumbnail, ICC
        profile, etc.) This will still hopefully be less expensive than
        calling reset() on the instance yet again.
         */
        ImageInputStream bis =
                new BufferedImageInputStream(inputStream, 20971520);
        bis.mark();

        final JPEGMetadataReader mdReader = new JPEGMetadataReader();
        mdReader.setSource(bis);
        final ICC_Profile profile = mdReader.getICCProfile();
        final JPEGMetadataReader.AdobeColorTransform transform =
                mdReader.getColorTransform();

        bis.reset();

        final WritableRaster raster =
                (WritableRaster) iioReader.readRaster(0, readParam);

        if (JPEGMetadataReader.AdobeColorTransform.YCCK.equals(transform)) {
            convertYCCKToCMYK(raster);
        }
        if (mdReader.hasAdobeSegment()) {
            convertInvertedColors(raster);
        }
        return convertCMYKToRGB(raster, profile);
    }

    private void convertYCCKToCMYK(WritableRaster raster) {
        final int height = raster.getHeight();
        final int width = raster.getWidth();
        final int stride = width * 4;
        final int[] pixelRow = new int[stride];

        for (int h = 0; h < height; h++) {
            raster.getPixels(0, h, width, 1, pixelRow);

            for (int x = 0; x < stride; x += 4) {
                int y = pixelRow[x];
                int cb = pixelRow[x + 1];
                int cr = pixelRow[x + 2];

                int c = (int) (y + 1.402 * cr - 178.956);
                int m = (int) (y - 0.34414 * cb - 0.71414 * cr + 135.95984);
                y = (int) (y + 1.772 * cb - 226.316);

                if (c < 0) c = 0; else if (c > 255) c = 255;
                if (m < 0) m = 0; else if (m > 255) m = 255;
                if (y < 0) y = 0; else if (y > 255) y = 255;

                pixelRow[x] = 255 - c;
                pixelRow[x + 1] = 255 - m;
                pixelRow[x + 2] = 255 - y;
            }

            raster.setPixels(0, h, width, 1, pixelRow);
        }
    }

    private void convertInvertedColors(WritableRaster raster) {
        final int height = raster.getHeight();
        final int width = raster.getWidth();
        final int stride = width * 4;
        final int[] pixelRow = new int[stride];

        for (int h = 0; h < height; h++) {
            raster.getPixels(0, h, width, 1, pixelRow);
            for (int x = 0; x < stride; x++) {
                pixelRow[x] = 255 - pixelRow[x];
            }
            raster.setPixels(0, h, width, 1, pixelRow);
        }
    }

    /**
     * @param cmykRaster  CMYK image raster.
     * @param cmykProfile If {@literal null}, a {@link #defaultCMYKProfile()
     *                    default CMYK profile} will be used.
     */
    private BufferedImage convertCMYKToRGB(Raster cmykRaster,
                                           ICC_Profile cmykProfile) throws IOException {
        if (cmykProfile == null) {
            cmykProfile = defaultCMYKProfile();
        }

        if (cmykProfile.getProfileClass() != ICC_Profile.CLASS_DISPLAY) {
            byte[] profileData = cmykProfile.getData();

            if (profileData[ICC_Profile.icHdrRenderingIntent] == ICC_Profile.icPerceptual) {
                intToBigEndian(
                        ICC_Profile.icSigDisplayClass,
                        profileData,
                        ICC_Profile.icHdrDeviceClass); // Header is first
                cmykProfile = ICC_Profile.getInstance(profileData);
            }
        }

        ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
        BufferedImage rgbImage = new BufferedImage(
                cmykRaster.getWidth(),
                cmykRaster.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        WritableRaster rgbRaster = rgbImage.getRaster();
        ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
        ColorConvertOp op = new ColorConvertOp(cmykCS, rgbCS, null);
        op.filter(cmykRaster, rgbRaster);

        return rgbImage;
    }

    private void intToBigEndian(int value, byte[] array, int index) {
        array[index]     = (byte) (value >> 24);
        array[index + 1] = (byte) (value >> 16);
        array[index + 2] = (byte) (value >> 8);
        array[index + 3] = (byte) (value);
    }

}
