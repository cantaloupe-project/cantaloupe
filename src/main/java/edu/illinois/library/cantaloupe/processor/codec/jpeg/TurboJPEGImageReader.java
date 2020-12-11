package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.util.Rational;
import org.libjpegturbo.turbojpeg.TJ;
import org.libjpegturbo.turbojpeg.TJDecompressor;
import org.libjpegturbo.turbojpeg.TJException;
import org.libjpegturbo.turbojpeg.TJScalingFactor;
import org.libjpegturbo.turbojpeg.TJTransform;
import org.libjpegturbo.turbojpeg.TJTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>Reader using libjpeg-turbo via its bundled TurboJPEG JNI binding.</p>
 *
 * <p>Region and scale-reduced decoding are supported. A pair of reading
 * methods are capable of reading into {@link BufferedImage}s as well as into
 * byte buffers for consumption by {@link TurboJPEGImageWriter}. Care is
 * taken to read only necessary image blocks, and to only decompress them when
 * necessary.</p>
 *
 * <p>In general, when the source JPEG is going to be written by {@link
 * TurboJPEGImageWriter} without any further transformation, {@link #read()}
 * should be used. Otherwise, {@link #readAsBufferedImage} should be used.</p>
 *
 * @see org.libjpegturbo.turbojpeg for libjpeg-turbo setup.
 * @author Alex Dolski UIUC
 */
public final class TurboJPEGImageReader implements AutoCloseable {

    /**
     * <p>Lossless transform.</p>
     *
     * <p>Note: libjpeg-turbo applies these <strong>before</strong>
     * cropping.</p>
     */
    public enum Transform {
        FLIP_HORIZONTAL(TJTransform.OP_HFLIP),
        FLIP_VERTICAL(TJTransform.OP_VFLIP),
        TRANSPOSE(TJTransform.OP_TRANSPOSE),
        TRANSVERSE(TJTransform.OP_TRANSVERSE),
        ROTATE_90(TJTransform.OP_ROT90),
        ROTATE_180(TJTransform.OP_ROT180),
        ROTATE_270(TJTransform.OP_ROT270);

        private final int tjEquivalentTx;

        Transform(int tjEquivalentTx) {
            this.tjEquivalentTx = tjEquivalentTx;
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TurboJPEGImageReader.class);

    /**
     * Array of fractional scaling factors that the TurboJPEG decompressor
     * supports.
     */
    private static final TJScalingFactor[] SCALING_FACTORS =
            TJ.getScalingFactors();

    /**
     * TurboJPEG has some issues handling lossless transformations on very
     * small images.
     */
    private static final int TRANSFORM_MIN_SIZE = 48;

    /**
     * The region the client wants to read. The region we tell TurboJPEG to
     * read may be slightly larger as its origin must be {@link
     * #getMCUSafeRegion evenly divisible by the MCU block size}.
     */
    private Rectangle region, mcuSafeRegion;
    private TJScalingFactor scalingFactor = new TJScalingFactor(1, 1);
    private Transform transform;
    private boolean useGrayscaleConversion, useFastUpsample, useFastDCT,
            useAccurateDCT;

    /**
     * Stream from which JPEG data will be read and stored in {@link
     * #jpegBytes}.
     */
    private InputStream inputStream;

    /**
     * Buffered JPEG data read from {@link #inputStream}.
     */
    private byte[] jpegBytes;

    /**
     * Decompressor initialized with {@link #jpegBytes}.
     */
    private TJDecompressor decompressor;

    /**
     * Cached from {@link #getSubsampling()}.
     */
    private transient int subsampling = -1;

    /**
     * Cached values from {@link #getWidth()} and {@link #getHeight()}.
     */
    private transient int width, height;

    /**
     * Cached values from {@link #getBlockWidth()} and {@link
     * #getBlockHeight()}.
     */
    private transient int blockWidth, blockHeight;

    /**
     * Moves the origin of the {@link #region crop region} left and up to the
     * minimum extent necessary to be evenly divisible by the MCU block size,
     * and also increases the width and height as necessary to do the same.
     * This enables reading individual blocks rather than the whole image.
     *
     * @param roiWithinRegion Empty rectangle whose coordinates will be
     *                        modified to reflect the ROI within the MCU-safe
     *                        region, in MCU-safe region coordinates.
     */
    static Rectangle getMCUSafeRegion(final Rectangle region,
                                      final int width,
                                      final int height,
                                      final int blockWidth,
                                      final int blockHeight,
                                      final Rectangle roiWithinRegion) {
        final Rectangle safeRegion = new Rectangle(region);

        if (width >= TRANSFORM_MIN_SIZE &&
                height >= TRANSFORM_MIN_SIZE &&
                region.width() >= TRANSFORM_MIN_SIZE &&
                region.height() >= TRANSFORM_MIN_SIZE) {
            // Stretch origin left
            final int roiXInset = region.intX() % blockWidth;
            safeRegion.move(-roiXInset, 0);
            safeRegion.resize(roiXInset, 0);
            roiWithinRegion.setX(region.x() - safeRegion.x());

            // Stretch width right
            final int roiExtraWidth =
                    (safeRegion.intX() + safeRegion.intWidth()) % blockWidth;
            if (roiExtraWidth != 0) {
                safeRegion.resize(blockWidth - roiExtraWidth, 0);
            }
            if (safeRegion.x() + safeRegion.width() > width) {
                safeRegion.setWidth(width - safeRegion.x());
            }
            roiWithinRegion.setWidth(region.width());

            // Stretch origin up
            final int roiYInset = region.intY() % blockHeight;
            safeRegion.move(0, -roiYInset);
            safeRegion.resize(0, roiYInset);
            roiWithinRegion.setY(region.y() - safeRegion.y());

            // Stretch height down
            final int roiExtraHeight =
                    (safeRegion.intY() + safeRegion.intHeight()) % blockHeight;
            if (roiExtraHeight != 0) {
                safeRegion.resize(0, blockHeight - roiExtraHeight);
            }
            if (safeRegion.y() + safeRegion.height() > height) {
                safeRegion.setHeight(height - safeRegion.y());
            }
            roiWithinRegion.setHeight(region.height());
        } else {
            safeRegion.setX(0);
            safeRegion.setY(0);
            safeRegion.setWidth(width);
            safeRegion.setHeight(height);
            roiWithinRegion.setX(region.x());
            roiWithinRegion.setY(region.y());
            roiWithinRegion.setWidth(region.width());
            roiWithinRegion.setHeight(region.height());
        }

        return safeRegion;
    }

    /**
     * @throws UnsatisfiedLinkError if there is an error loading libjpeg-turbo.
     */
    public static synchronized void initialize() {
        TJ.getScalingFactors(); // call a trivial native method
    }

    @Override
    public void close() {
        jpegBytes = null;
        try {
            if (decompressor != null) {
                decompressor.close();
            }
        } catch (IOException ignore) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * @return 8 or 16, depending on the subsampling.
     */
    public int getBlockWidth() throws IOException {
        if (blockWidth < 1) {
            initDecompressor();
            switch (getSubsampling()) {
                case TJ.SAMP_420:
                    blockWidth = 16;
                    break;
                case TJ.SAMP_422:
                    blockWidth = 16;
                    break;
                default:
                    blockWidth = 8;
                    break;
            }
        }
        return blockWidth;
    }

    /**
     * @return 8 or 16, depending on the subsampling.
     */
    public int getBlockHeight() throws IOException {
        if (blockHeight < 1) {
            initDecompressor();
            switch (getSubsampling()) {
                case TJ.SAMP_420:
                    blockHeight = 16;
                    break;
                default:
                    blockHeight = 8;
                    break;
            }
        }
        return blockHeight;
    }

    /**
     * @return Full source image width.
     */
    public int getWidth() throws IOException {
        if (width < 1) {
            initDecompressor();
            width = decompressor.getWidth();
        }
        return width;
    }

    /**
     * @return Full source image height.
     */
    public int getHeight() throws IOException {
        if (height < 1) {
            initDecompressor();
            height = decompressor.getHeight();
        }
        return height;
    }

    /**
     * @return One of the {@link TJ#SAMP_444 TJ#SAMP_*} constant values.
     */
    public int getSubsampling() throws IOException {
        if (subsampling < 0) {
            initDecompressor();
            subsampling = decompressor.getSubsamp();
        }
        return subsampling;
    }

    /**
     * @return {@literal false} if there are no reduced-size edge blocks and
     *         the image can be transformed without loss of edge pixels.
     */
    public boolean isTransformable() throws IOException {
        return getWidth() >= TRANSFORM_MIN_SIZE &&
                getHeight() >= TRANSFORM_MIN_SIZE &&
                getWidth() % getBlockWidth() == 0 &&
                getHeight() % getBlockHeight() == 0;
    }

    /**
     * <p>Performs lossless cropping on the input image.</p>
     *
     * <p>When this method is used, clients should not use {@link #read()}
     * unless the source image is {@link #isTransformable() transformable}.
     * Otherwise, they should use {@link #readAsBufferedImage(Rectangle)}.</p>
     */
    public void setRegion(int x, int y, int width, int height) {
        this.region = new Rectangle(x, y, width, height);
        this.mcuSafeRegion = null;
    }

    /**
     * <p>Sets the scale factor to use while decompressing the image. Note that
     * TurboJPEG may not support this scale factor, in which case an {@link
     * TransformationNotSupportedException} will be thrown and clients will
     * have to scale the resulting image themselves.</p>
     *
     * <p>Scale factors known to be available in libjpeg-turbo 2.0.2 include:
     * 2/1, 15/8, 7/4, 13/8, 3/2, 11/8, 5/4, 9/8, 1/1, 7/8, 3/4, 5/8, 1/2, 3/8,
     * 1/4, 1/8.</p>
     *
     * @throws TransformationNotSupportedException if libjpeg-turbo does not
     *         support the given scale rational, in which case the image will
     *         be read at 1:1 scale.
     */
    public void setScale(Rational rational)
            throws TransformationNotSupportedException {
        TJScalingFactor requested = new TJScalingFactor(
                (int) rational.getNumerator(), (int) rational.getDenominator());

        // See if TJ supports the one we need.
        boolean supported = false;
        for (TJScalingFactor candidate : SCALING_FACTORS) {
            if (candidate.equals(requested)) {
                scalingFactor = candidate;
                supported = true;
                break;
            }
        }
        if (!supported) {
            throw new TransformationNotSupportedException(
                    "libjpeg-turbo does not support this scale.");
        }
    }

    /**
     * @param source Stream containing JPEG image data, pre-positioned to
     *               offset zero. Will be read fully into memory.
     */
    public void setSource(InputStream source) {
        this.inputStream = source;
    }

    /**
     * @param transform Lossless transform to apply before decompression.
     * @throws TransformationNotSupportedException if the source image is not
     *         {@link #isTransformable() transformable}.
     */
    public void setTransform(Transform transform)
            throws IOException, TransformationNotSupportedException {
        if (!isTransformable()) {
            throw new TransformationNotSupportedException(
                    "Cannot perform lossless transformation as one or both " +
                            "dimensions are not multiples of the block size.");
        }
        this.transform = transform;
    }

    /**
     * Use the most accurate DCT/IDCT algorithm available in the underlying
     * codec. The default if this flag is not specified is implementation-
     * specific. For example, the implementation of TurboJPEG for
     * libjpeg[-turbo] uses the accurate algorithm, because this has been shown
     * to have a larger effect.
     */
    public void setUseAccurateDCT(boolean useAccurateDCT) {
        this.useAccurateDCT = useAccurateDCT;
        this.useFastDCT     = !useAccurateDCT;
    }

    /**
     * @see #setUseAccurateDCT(boolean)
     */
    public void setUseFastDCT(boolean useFastDCT) {
        this.useFastDCT     = useFastDCT;
        this.useAccurateDCT = !useFastDCT;
    }

    /**
     * When decompressing an image that was compressed using chrominance
     * subsampling, use the fastest chrominance upsampling algorithm available
     * in the underlying codec. The default is to use smooth upsampling, which
     * creates a smooth transition between neighboring chrominance components
     * in order to reduce upsampling artifacts in the decompressed image.
     */
    public void setUseFastUpsample(boolean useFastUpsample) {
        this.useFastUpsample = useFastUpsample;
    }

    /**
     * @param useGrayscaleConversion Whether to apply lossless grayscale
     *                               conversion to the image before
     *                               decompressing it.
     * @throws TransformationNotSupportedException if the source image is not
     *         {@link #isTransformable() transformable}.
     */
    public void setUseGrayscaleConversion(boolean useGrayscaleConversion)
            throws IOException, TransformationNotSupportedException {
        if (!isTransformable()) {
            throw new TransformationNotSupportedException(
                    "Cannot perform lossless transformation as one or both " +
                            "dimensions are not multiples of the block size.");
        }
        this.useGrayscaleConversion = useGrayscaleConversion;
    }

    /**
     * Wraps {@link #getMCUSafeRegion(Rectangle, int, int, int, int,
     * Rectangle)}.
     *
     * @param roiWithinRegion Empty rectangle whose coordinates will be
     *                        modified to reflect the ROI within the MCU-safe
     *                        region, in MCU-safe region coordinates.
     */
    private Rectangle getMCUSafeRegion(Rectangle roiWithinRegion)
            throws IOException {
        if (region == null) {
            return null;
        }
        if (mcuSafeRegion == null) {
            mcuSafeRegion = getMCUSafeRegion(region,
                    getWidth(), getHeight(),
                    getBlockWidth(), getBlockHeight(),
                    roiWithinRegion);
        }
        return mcuSafeRegion;
    }

    /**
     * @return One of the {@link TJ#FLAG_ACCURATEDCT TJ#FLAG_*} constant
     *         values.
     */
    private int getFlags() {
        int flags = 0;
        if (useFastUpsample) {
            flags |= TJ.FLAG_FASTUPSAMPLE;
        }
        if (useFastDCT) {
            flags |= TJ.FLAG_FASTDCT;
        } else if (useAccurateDCT) {
            flags |= TJ.FLAG_ACCURATEDCT;
        }
        return flags;
    }

    private boolean isRegionMCUSafe() throws IOException {
        if (region != null) {
            final int blockWidth = getBlockWidth();
            final int blockHeight = getBlockHeight();
            return region.intX() % blockWidth == 0 &&
                    region.intY() % blockHeight == 0 &&
                    region.intWidth() % blockWidth == 0 &&
                    region.intHeight() % blockHeight == 0;
        }
        return getWidth() % blockWidth == 0 &&
                getHeight() % blockHeight == 0;
    }

    private boolean isTransforming(TJTransform xform) {
        return (xform.op != TJTransform.OP_NONE ||
                xform.options != 0 ||
                xform.cf != null);
    }

    private void initDecompressor() throws IOException {
        if (decompressor == null) {
            jpegBytes = readInputStream();
            try {
                decompressor = new TJDecompressor(jpegBytes);
            } catch (TJException e) {
                if (e.getMessage().contains("Not a JPEG file")) {
                    throw new SourceFormatException();
                }
                throw e;
            }
        }
    }

    /**
     * @param roiWithinSafeRegion Coordinates will be modified.
     */
    private void initDecompressor(Rectangle roiWithinSafeRegion)
            throws IOException {
        initDecompressor();

        TJTransform xform = new TJTransform();
        if (useGrayscaleConversion) {
            xform.options |= TJTransform.OPT_GRAY;
        }
        if (transform != null) {
            xform.op = transform.tjEquivalentTx;
        }
        final Rectangle safeRegion = getMCUSafeRegion(roiWithinSafeRegion);
        if (safeRegion != null && (safeRegion.intWidth() != getWidth() ||
                safeRegion.intHeight() != getHeight())) {
            xform.x      = safeRegion.intX();
            xform.y      = safeRegion.intY();
            xform.width  = safeRegion.intWidth();
            xform.height = safeRegion.intHeight();
            xform.options |= TJTransform.OPT_CROP;
        }

        if (isTransforming(xform)) {
            try (TJTransformer tjt = new TJTransformer(jpegBytes)) {
                decompressor.close();
                TJTransform[] xforms  = new TJTransform[] { xform };
                xform.options        |= TJTransform.OPT_TRIM;
                TJDecompressor[] tjds = tjt.transform(xforms, 0);
                decompressor          = tjds[0];
            } catch (TJException e) {
                if (e.getMessage().contains("Not a JPEG file")) {
                    throw new SourceFormatException();
                }
                throw e;
            }
        }
    }

    /**
     * Reads all JPEG data from {@link #inputStream} into a byte array.
     * Unfortunately the TurboJPEG API isn't capable of reading from streams.
     */
    private byte[] readInputStream() throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, n);
            }
            return os.toByteArray();
        }
    }

    /**
     * @return Relatively raw data that is expected to eventually be written
     *         by {@link TurboJPEGImageWriter}.
     */
    public TurboJPEGImage read() throws IOException {
        final Rectangle margin  = new Rectangle();
        initDecompressor(margin);

        int width  = getWidth();
        int height = getHeight();
        final Rectangle safeRegion = getMCUSafeRegion(new Rectangle());
        if (safeRegion != null) {
            width  = safeRegion.intWidth();
            height = safeRegion.intHeight();
        }
        width  = scalingFactor.getScaled(width);
        height = scalingFactor.getScaled(height);

        final TurboJPEGImage image = new TurboJPEGImage();
        image.setScaledWidth(width);
        image.setScaledHeight(height);


            byte[] data = decompressor.decompress(width, 0, height,
                    TJ.PF_BGRX, getFlags());
            image.setData(data);
            image.setDecompressed(true);

        return image;
    }

    /**
     * @param roiWithinSafeRegion Empty rectangle whose coordinates will be
     *                            modified to reflect any additional cropping
     *                            that will be required on the returned image.
     * @return                    Image that may require additional processing
     *                            and/or is not expected to get written later
     *                            by {@link TurboJPEGImageWriter}.
     */
    public BufferedImage readAsBufferedImage(Rectangle roiWithinSafeRegion)
            throws IOException {
        initDecompressor(roiWithinSafeRegion);

        int width  = getWidth();
        int height = getHeight();
        final Rectangle safeRegion = getMCUSafeRegion(roiWithinSafeRegion);
        if (safeRegion != null) {
            width  = safeRegion.intWidth();
            height = safeRegion.intHeight();
        }
        width  = scalingFactor.getScaled(width);
        height = scalingFactor.getScaled(height);

        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        try {
            decompressor.decompress(image, getFlags());
        } catch (TJException e) {
            if (e.getErrorCode() == TJ.ERR_FATAL) {
                throw e;
            } else {
                LOGGER.warn("readAsBufferedImage(): image is corrupt");
            }
        }
        return image;
    }

}
