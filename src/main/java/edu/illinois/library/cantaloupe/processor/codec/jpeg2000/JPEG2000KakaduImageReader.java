package edu.illinois.library.cantaloupe.processor.codec.jpeg2000;

import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import edu.illinois.library.cantaloupe.util.StringUtils;
import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_input_box;
import kdu_jni.Jpx_layer_source;
import kdu_jni.Jpx_meta_manager;
import kdu_jni.Jpx_metanode;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_compressed_source;
import kdu_jni.Kdu_compressed_source_nonnative;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message;
import kdu_jni.Kdu_message_formatter;
import kdu_jni.Kdu_quality_limiter;
import kdu_jni.Kdu_region_decompressor;
import kdu_jni.Kdu_simple_file_source;
import kdu_jni.Kdu_thread_env;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * <p>JPEG2000 image reader using the Kakadu native library ({@literal libkdu})
 * via the Java Native Interface (JNI). Written against version 7.10.</p>
 *
 * <p><strong>Important: {@link #close()} must be called after use.</strong></p>
 *
 * <h1>Usage</h1>
 *
 * <p>The Kakadu shared library and JNI binding (two separate files) must be
 * present on the library path, or else the {@literal -Djava.library.path} VM
 * argument must be provided at launch, with a value of the pathname of the
 * directory containing the library.</p>
 *
 * <h1>License</h1>
 *
 * <p>This software was developed using a Kakadu Public Service License
 * and may not be used commercially. See the
 * <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Kakadu-Licence-Terms-Feb-2018.pdf">
 * Kakadu Software License Terms and Conditions</a> for detailed terms.</p>
 *
 * @since 4.1
 * @author Alex Dolski UIUC
 */
public final class JPEG2000KakaduImageReader implements AutoCloseable {

    /**
     * Custom {@link Kdu_compressed_source_nonnative} backed by an {@link
     * ImageInputStream}, which, in contrast to {@link java.io.InputStream}, is
     * seekable, even if it has to employ buffering or a disk cache (will
     * depend on the implementation). Seeking can offer major performance
     * advantages when the stream is capable of exploiting it fully.
     */
    private static class KduImageInputStreamSource
            extends Kdu_compressed_source_nonnative {

        private final ImageInputStream inputStream;

        KduImageInputStreamSource(ImageInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int Get_capabilities() {
            return Kdu_global.KDU_SOURCE_CAP_SEQUENTIAL |
                    Kdu_global.KDU_SOURCE_CAP_SEEKABLE;
        }

        @Override
        public long Get_pos() throws KduException {
            try {
                return inputStream.getStreamPosition();
            } catch (IOException e) {
                throw new KduException(e.getMessage());
            }
        }

        @Override
        public int Post_read(final int numBytesRequested) {
            final byte[] buffer = new byte[numBytesRequested];
            int numBytesRead = 0;
            try {
                while (numBytesRead < numBytesRequested) {
                    int numSupplied = inputStream.read(
                            buffer, numBytesRead, buffer.length - numBytesRead);
                    if (numSupplied < 0) {
                        break;
                    }
                    numBytesRead += numSupplied;
                }
                Push_data(buffer, 0, numBytesRead);
            } catch (KduException | IOException e) {
                LOGGER.error(e.getMessage());
            }
            return numBytesRead;
        }

        @Override
        public boolean Seek(long offset) throws KduException {
            try {
                inputStream.seek(offset);
                return true;
            } catch (IOException e) {
                throw new KduException(e.getMessage());
            }
        }

    }

    /**
     * N.B.: it might be better to extend {@link
     * kdu_jni.Kdu_thread_safe_message} instead, but there is apparently some
     * kind of bug up through Kakadu 8.0.3 (at least) that causes it to not
     * work right. (See
     * https://github.com/cantaloupe-project/cantaloupe/issues/396)
     */
    private static abstract class AbstractKduMessage extends Kdu_message {

        final StringBuilder builder = new StringBuilder();

        @Override
        public void Put_text(String text) {
            builder.append(text);
        }

    }

    private static class KduDebugMessage extends AbstractKduMessage {

        @Override
        public void Flush(boolean isEndOfMessage) {
            if (isEndOfMessage) {
                LOGGER.debug(builder.toString());
                builder.setLength(0);
            }
        }

    }

    private static class KduErrorMessage extends AbstractKduMessage {

        @Override
        public void Flush(boolean isEndOfMessage) throws KduException {
            if (isEndOfMessage) {
                LOGGER.error(builder.toString());
                throw new KduException(Kdu_global.KDU_ERROR_EXCEPTION,
                        "In " + this.getClass().getSimpleName());
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEG2000KakaduImageReader.class);

    /**
     * {@link Kdu_region_decompressor} requires scale factors to be expressed
     * as fractions. This is set to roughly 210x smaller than {@link
     * Integer#MAX_VALUE} to allow for 210x enlargement, still with good
     * precision.
     *
     * @see #readRegion
     */
    private static final int EXPAND_DENOMINATOR = 10000000;
    private static final int MAX_LAYERS         = 16384;

    /**
     * N.B.: {@link Kdu_global#Kdu_get_num_processors()} is another way of
     * getting the CPU count. Unknown whether it's any more reliable.
     */
    private static final int NUM_THREADS =
            Math.max(1, Runtime.getRuntime().availableProcessors());

    private static final KduDebugMessage KDU_SYSOUT = new KduDebugMessage();
    private static final KduErrorMessage KDU_SYSERR = new KduErrorMessage();
    private static final Kdu_message_formatter KDU_PRETTY_SYSOUT =
            new Kdu_message_formatter(KDU_SYSOUT);
    private static final Kdu_message_formatter KDU_PRETTY_SYSERR =
            new Kdu_message_formatter(KDU_SYSERR);

    private Jpx_source jpxSrc;
    private Jp2_threadsafe_family_src familySrc;
    private Kdu_compressed_source compSrc;
    private Kdu_codestream codestream;
    private Kdu_channel_mapping channels;
    private Kdu_region_decompressor decompressor;
    private Kdu_thread_env threadEnv;
    private Kdu_quality_limiter limiter;

    /**
     * Set by {@link #setSource(Path)}. Used preferentially over {@link
     * #inputStream}.
     */
    private Path sourceFile;

    /**
     * Set by {@link #setSource(ImageInputStream)}. Used if {@link
     * #sourceFile} is not set.
     */
    private ImageInputStream inputStream;

    private boolean isOpenAttempted, haveReadInfo, haveReadMetadata,
            isDecompressing;

    private int width, height, tileWidth, tileHeight, numDWTLevels = -1;
    private String xmp;
    private byte[] iptc;

    static {
        try {
            Kdu_global.Kdu_customize_warnings(KDU_PRETTY_SYSOUT);
            Kdu_global.Kdu_customize_errors(KDU_PRETTY_SYSERR);
        } catch (KduException e) {
            LOGGER.error("Static initializer: {}", e.getMessage(), e);
        }
    }

    public JPEG2000KakaduImageReader() {
        init();
    }

    private void handle(KduException e) {
        try {
            threadEnv.Handle_exception(e.Get_kdu_exception_code());
        } catch (KduException ke) {
            LOGGER.debug("{} (code: {})",
                    ke.getMessage(),
                    Integer.toHexString(ke.Get_kdu_exception_code()),
                    ke);
        }
    }

    /**
     * Closes everything.
     */
    @Override
    public void close() {
        close(true);
    }

    /**
     * Variant of {@link #close()} with an option to leave the {@link
     * #setSource(ImageInputStream) input stream} open.
     */
    private void close(boolean alsoCloseInputStream) {
        // N.B.: see the end notes in the KduRender.java file in the Kakadu SDK
        // for explanation of how this stuff needs to be destroyed. (And it DOES
        // need to be destroyed!)
        if (isDecompressing) {
            try {
                decompressor.Finish();
            } catch (KduException e) {
                LOGGER.warn("Failed to stop the kdu_region_decompressor: {} (code: {})",
                        e.getMessage(),
                        Integer.toHexString(e.Get_kdu_exception_code()));
            }
        }
        limiter.Native_destroy();
        decompressor.Native_destroy();
        channels.Native_destroy();
        try {
            if (codestream.Exists()) {
                threadEnv.Cs_terminate(codestream);
                codestream.Destroy();
            }
        } catch (KduException e) {
            LOGGER.warn("Failed to destroy the kdu_codestream: {} (code: {})",
                    e.getMessage(),
                    Integer.toHexString(e.Get_kdu_exception_code()));
        }

        if (compSrc != null) {
            try {
                compSrc.Close();
            } catch (KduException e) {
                LOGGER.warn("Failed to close the {}: {} (code: {})",
                        compSrc.getClass().getSimpleName(),
                        e.getMessage(),
                        Integer.toHexString(e.Get_kdu_exception_code()));
            } finally {
                compSrc.Native_destroy();
            }
        }
        jpxSrc.Native_destroy();
        familySrc.Native_destroy();

        try {
            threadEnv.Destroy();
        } catch (KduException e) {
            LOGGER.warn("Failed to destroy the kdu_thread_env: {} (code: {})",
                    e.getMessage(),
                    Integer.toHexString(e.Get_kdu_exception_code()));
        }
        threadEnv.Native_destroy();

        if (inputStream != null) {
            try {
                if (alsoCloseInputStream) {
                    IOUtils.closeQuietly(inputStream);
                } else {
                    inputStream.seek(0);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to close the {}: {}",
                        inputStream.getClass().getSimpleName(),
                        e.getMessage(), e);
            }
        }

        isOpenAttempted = false;
    }

    private void init() {
        jpxSrc       = new Jpx_source();
        familySrc    = new Jp2_threadsafe_family_src();
        codestream   = new Kdu_codestream();
        channels     = new Kdu_channel_mapping();
        decompressor = new Kdu_region_decompressor();
        threadEnv    = new Kdu_thread_env();
        limiter      = new Kdu_quality_limiter(1 / 256f, false);
    }

    public int getHeight() throws IOException {
        if (height == 0) {
            openImage();
            readInfo();
        }
        return height;
    }

    public byte[] getIPTC() throws IOException {
        readMetadata();
        return iptc;
    }

    /**
     * @return Number of decomposition levels available in the codestream as
     *         reported in its {@literal COD} or {@literal COC} segment. Note
     *         that this may (rarely) be larger than the actual number of
     *         decomposition levels available in the codestream, and may change
     *         (to reflect a more accurate number) after {@link #readRegion}
     *         is invoked.
     */
    public int getNumDecompositionLevels() throws IOException {
        if (numDWTLevels == -1) {
            openImage();
            readInfo();
        }
        return numDWTLevels;
    }

    public int getTileHeight() throws IOException {
        if (tileHeight == 0) {
            openImage();
            readInfo();
        }
        return tileHeight;
    }

    public int getTileWidth() throws IOException {
        if (tileWidth == 0) {
            openImage();
            readInfo();
        }
        return tileWidth;
    }

    public int getWidth() throws IOException {
        if (width == 0) {
            openImage();
            readInfo();
        }
        return width;
    }

    public String getXMP() throws IOException {
        readMetadata();
        return xmp;
    }

    public void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setSource(Path file) {
        this.sourceFile = file;
    }

    private void openImage() throws IOException {
        if (isOpenAttempted) {
            return;
        }
        isOpenAttempted = true;

        try {
            if (sourceFile != null) {
                familySrc.Open(sourceFile.toString());
            } else {
                compSrc = new KduImageInputStreamSource(inputStream);
                familySrc.Open(compSrc);
            }

            Jpx_layer_source layerSrc           = null;
            Jpx_codestream_source codestreamSrc = null;
            int success = jpxSrc.Open(familySrc, true);
            if (success >= 0) {
                // Succeeded in opening as wrapped JP2/JPX source.
                layerSrc      = jpxSrc.Access_layer(0);
                codestreamSrc = jpxSrc.Access_codestream(layerSrc.Get_codestream_id(0));
                compSrc       = codestreamSrc.Open_stream();
            } else {
                // Must open as raw codestream.
                familySrc.Close();
                jpxSrc.Close();
                if (sourceFile != null) {
                    compSrc = new Kdu_simple_file_source(sourceFile.toString());
                } else {
                    inputStream.seek(0);
                    compSrc = new KduImageInputStreamSource(inputStream);
                }
            }

            // Tell Kakadu to use as many more threads as we have CPUs.
            threadEnv.Create();
            for (int t = 1; t < NUM_THREADS; t++) {
                threadEnv.Add_thread();
            }

            codestream.Create(compSrc, threadEnv);
            codestream.Set_resilient();

            boolean anyChannels = false;
            if (layerSrc != null) {
                channels.Configure(
                        layerSrc.Access_colour(0),
                        layerSrc.Access_channels(),
                        codestreamSrc.Get_codestream_id(),
                        codestreamSrc.Access_palette(),
                        codestreamSrc.Access_dimensions());
                anyChannels = (channels.Get_num_channels() > 0);
            }
            if (!anyChannels) {
                channels.Configure(codestream);
            }
        } catch (KduException e) {
            handle(e);
            if (e.Get_kdu_exception_code() == 1801745731) {
                throw new SourceFormatException();
            } else {
                throw new IOException(e);
            }
        }
    }

    private void readMetadata() throws IOException {
        if (!haveReadMetadata) {
            haveReadMetadata = true;
            try {
                openImage();

                // Access the Jpx_source's metadata manager, which provides
                // access to all of the various metadata boxes.
                final Jpx_meta_manager manager = jpxSrc.Access_meta_manager();
                manager.Set_box_filter(1, new long[] { Kdu_global.jp2_uuid_4cc });

                Jpx_metanode lastNode = new Jpx_metanode();
                do {
                    manager.Load_matches(-1, new int[] {}, -1, new int[] {});
                    lastNode = manager.Enumerate_matches(
                            lastNode, -1, -1, false, new Kdu_dims(), 0);
                    if (lastNode.Is_iptc_uuid() || lastNode.Is_xmp_uuid()) {
                        byte[] buffer;
                        final Jpx_input_box box = new Jpx_input_box();
                        try {
                            lastNode.Open_existing(box);
                            final int bufferSize = (int) box.Get_box_bytes();
                            buffer = new byte[bufferSize];
                            box.Read(buffer, bufferSize);
                            if (lastNode.Is_iptc_uuid() && bufferSize > 16) {
                                iptc = Arrays.copyOfRange(buffer, 16, buffer.length);
                            } else if (lastNode.Is_xmp_uuid()) {
                                xmp = new String(buffer, StandardCharsets.UTF_8);
                                xmp = StringUtils.trimXMP(xmp);
                            }
                        } finally {
                            box.Close();
                        }
                    }
                } while ((xmp == null || iptc == null) && lastNode.Exists());
            } catch (KduException e) {
                handle(e);
                throw new IOException(e);
            }
        }
    }

    private void readInfo() throws IOException {
        if (haveReadInfo) {
            return;
        }
        haveReadInfo = true;

        try {
            final Stopwatch watch = new Stopwatch();

            // Read the image dimensions.
            {
                final int referenceComponent = channels.Get_source_component(0);
                final Kdu_dims imageDims = new Kdu_dims();
                codestream.Get_dims(referenceComponent, imageDims);
                width = imageDims.Access_size().Get_x();
                height = imageDims.Access_size().Get_y();
            }

            // Read the tile dimensions.
            {
                Kdu_coords tileIndex = new Kdu_coords();
                Kdu_dims tileDims    = new Kdu_dims();
                codestream.Get_tile_dims(tileIndex, -1, tileDims, false);
                tileWidth = tileDims.Access_size().Get_x();
                tileHeight = tileDims.Access_size().Get_y();

                // Swap dimensions to harmonize with the main image dimensions,
                // if necessary.
                if (width > height && tileWidth < tileHeight ||
                        width < height && tileWidth > tileHeight) {
                    int tmp = tileWidth;
                    //noinspection SuspiciousNameCombination
                    tileWidth = tileHeight;
                    tileHeight = tmp;
                }
            }

            // Read the DWT level count.
            numDWTLevels = codestream.Get_min_dwt_levels();

            LOGGER.trace("readInfo(): read in {}", watch);
        } catch (KduException e) {
            handle(e);
            throw new IOException(e);
        }
    }

    /**
     * Reads a scaled region of interest into a {@link BufferedImage} using a
     * {@link Kdu_region_decompressor}.
     *
     * @param roi             Region to read, in source coordinates.
     * @param scaleOp         Scale operation to apply.
     * @param scaleConstraint Scale constraint.
     * @param reductionFactor {@link ReductionFactor#factor} will be set
     *                        appropriately during reading.
     * @param diffScales      Two-element array that will be populated with the
     *                        X and Y axis differential scales used during
     *                        reading.
     */
    public BufferedImage readRegion(final Rectangle roi,
                                    final Scale scaleOp,
                                    final ScaleConstraint scaleConstraint,
                                    final ReductionFactor reductionFactor,
                                    final double[] diffScales) throws IOException {
        // N.B. 1: Kdu_dims and Kdu_coords are integer-based, and this can lead
        // to precision loss when Rectangles and Dimensions are converted
        // back-and-forth. Try to stay in the Rectangle/Dimension space and
        // convert only when necessary.

        // N.B. 2: The arguments (except for reductionFactor & diffScales) must
        // not be modified as this method may need to re-invoke itself. Here we
        // store the initial ROI in case.
        final Rectangle initialROI = new Rectangle(roi);

        // Find what we assume for now is the best resolution level to read
        // based on the requested scale and the number of DWT levels reported
        // in the codestream COD/COC segment (which
        // Kdu_region_decompressor.Finish() may end up telling us is incorrect,
        // but we'll cross that bridge when we get to it).
        if (scaleOp != null) {
            final double[] scales = scaleOp.getResultingScales(
                    roi.size(), scaleConstraint);
            // If x & y scales are different, base the reduction factor on the
            // largest one.
            final double maxScale = Arrays.stream(scales).max().orElse(1);
            reductionFactor.factor =
                    ReductionFactor.forScale(maxScale, 0.001).factor;

            // Clamp the reduction factor to the range of 0-(# DWT levels).
            if (reductionFactor.factor < 0) {
                reductionFactor.factor = 0;
            } else if (reductionFactor.factor > getNumDecompositionLevels()) {
                reductionFactor.factor = getNumDecompositionLevels();
            }
        }

        BufferedImage image;

        final Stopwatch watch = new Stopwatch();

        try {
            openImage();

            final int referenceComponent = channels.Get_source_component(0);
            final int accessMode = Kdu_global.KDU_WANT_OUTPUT_COMPONENTS;

            // The expand numerator & denominator here tell
            // kdu_region_decompressor.start() at what fractional scale to
            // render the result. The expansion is relative to the selected
            // resolution level, not the full image dimensions.
            final Kdu_coords expandNumerator = determineReferenceExpansion(
                    referenceComponent, channels, codestream,
                    threadEnv, limiter);
            if (scaleOp != null) {
                double[] tmp = scaleOp.getDifferentialScales(
                        roi.size(), reductionFactor, scaleConstraint);
                System.arraycopy(tmp, 0, diffScales, 0, 2);
            }
            expandNumerator.Set_x((int) Math.round(
                    expandNumerator.Get_x() * diffScales[0] * EXPAND_DENOMINATOR));
            expandNumerator.Set_y((int) Math.round(
                    expandNumerator.Get_y() * diffScales[1] * EXPAND_DENOMINATOR));
            final Kdu_coords expandDenominator =
                    new Kdu_coords(EXPAND_DENOMINATOR, EXPAND_DENOMINATOR);

            // Get the effective source image size at the selected resolution
            // level.
            final Kdu_dims sourceDims = decompressor.Get_rendered_image_dims(
                    codestream, channels, -1, reductionFactor.factor,
                    expandNumerator, expandDenominator, accessMode);

            // Adjust the ROI coordinates for the image size on the canvas.
            final double rfScale = reductionFactor.getScale();
            roi.scaleX(rfScale);
            roi.scaleY(rfScale);
            Kdu_dims regionDims = toKduDims(roi);
            Kdu_coords refSubs  = new Kdu_coords();
            codestream.Get_subsampling(referenceComponent, refSubs);
            regionDims = decompressor.Find_render_dims(regionDims, refSubs,
                    expandNumerator, expandDenominator);

            // The kdu_quality_limiter can save decoding effort by truncating
            // code blocks to sensible lengths. The docs are pretty detailed
            // about optimizing these argument values.
            final float xPPI = 600f * refSubs.Get_x();
            final float yPPI = 600f * refSubs.Get_y();
            limiter.Set_display_resolution(xPPI, yPPI);

            LOGGER.debug("Rendered region {},{}/{}x{}; source {},{}/{}x{}; " +
                            "{}x reduction factor; differential scale {}/{}; PPI {}x{}",
                    regionDims.Access_pos().Get_x(),
                    regionDims.Access_pos().Get_y(),
                    regionDims.Access_size().Get_x(),
                    regionDims.Access_size().Get_y(),
                    sourceDims.Access_pos().Get_x(),
                    sourceDims.Access_pos().Get_y(),
                    sourceDims.Access_size().Get_x(),
                    sourceDims.Access_size().Get_y(),
                    reductionFactor.factor,
                    expandNumerator.Get_x(),
                    expandDenominator.Get_x(), // y should == x
                    xPPI,
                    yPPI);

            decompressor.Start(codestream, channels, -1,
                    reductionFactor.factor, MAX_LAYERS, regionDims,
                    expandNumerator, expandDenominator, false, accessMode,
                    false, threadEnv);
            isDecompressing = true;

            Kdu_dims newRegion        = new Kdu_dims();
            Kdu_dims incompleteRegion = new Kdu_dims();
            Kdu_dims viewDims         = new Kdu_dims();
            viewDims.Assign(regionDims);
            incompleteRegion.Assign(regionDims);

            image = new BufferedImage(
                    regionDims.Access_size().Get_x(),
                    regionDims.Access_size().Get_y(),
                    BufferedImage.TYPE_INT_ARGB);

            final int regionBufferSize = regionDims.Access_size().Get_x() * 32;
            final int[] regionBuffer   = new int[regionBufferSize];

            while (decompressor.Process(regionBuffer, regionDims.Access_pos(),
                    0, 0, regionBufferSize, incompleteRegion, newRegion)) {
                Kdu_coords newPos = newRegion.Access_pos();
                Kdu_coords newSize = newRegion.Access_size();
                newPos.Subtract(viewDims.Access_pos());

                image.setRGB(newPos.Get_x(), newPos.Get_y(),
                        newSize.Get_x(), newSize.Get_y(),
                        regionBuffer, 0, newSize.Get_x());
            }
            if (decompressor.Finish()) {
                // If the incomplete region is non-empty, this means that the
                // image was not fully decoded, probably because it contains
                // fewer resolution levels than the number we tried to discard,
                // so we will have to re-invoke this method, discarding fewer.
                if (!incompleteRegion.Is_empty()) {
                    final int initialNumDWTLevels = numDWTLevels;
                    numDWTLevels = codestream.Get_min_dwt_levels();
                    if (reductionFactor.factor > numDWTLevels) {
                        LOGGER.debug("COD/COC segment reported {} DWT levels, " +
                                        "but only {} are available. Retrying.",
                                initialNumDWTLevels, numDWTLevels);
                        close(false);
                        init();
                        image = readRegion(initialROI, scaleOp, scaleConstraint,
                                reductionFactor, diffScales);
                    }
                }
            } else {
                // Would be nice if we knew more...
                LOGGER.error("Fatal error in the codestream management machinery.");
            }
            isDecompressing = false;
        } catch (KduException e) {
            try {
                threadEnv.Handle_exception(e.Get_kdu_exception_code());
            } catch (KduException e2) {
                LOGGER.warn("readRegion(): {} (code: {})",
                        e2.getMessage(),
                        Integer.toHexString(e2.Get_kdu_exception_code()));
            }
            throw new IOException(e);
        }

        LOGGER.trace("readRegion(): read in {}", watch);

        return image;
    }

    private static Kdu_dims toKduDims(Rectangle rect) throws KduException {
        Kdu_dims regionDims = new Kdu_dims();
        regionDims.From_double(rect.x(), rect.y(), rect.width(), rect.height());
        return regionDims;
    }

    /**
     * <p>This method is largely lifted from the {@literal KduRender.java} file
     * in the Kakadu SDK. The author's documentation follows:</p>
     *
     * <blockquote>This function almost invariably returns (1,1), but there can
     * be some wacky images for which larger expansions are required. The need
     * for it arises from the fact that {@link Kdu_region_decompressor}
     * performs its decompressed image sizing based upon a single image
     * component (the {@literal image_component}). Specifically, the size of
     * the decompressed result is obtained by expanding the dimensions of the
     * reference component by the x-y values returned by this function.
     * Reference expansion factors must have the property that when the first
     * component is expanded by this much, any other components (typically
     * colour components) are also expanded by an integral amount. The {@link
     * Kdu_region_decompressor} actually does support rational expansion of
     * individual image components, but we do not exploit this feature in the
     * present coding example.</blockquote>
     */
    private static Kdu_coords determineReferenceExpansion(
            int reference_component,
            Kdu_channel_mapping channels,
            Kdu_codestream codestream,
            Kdu_thread_env threadEnv,
            Kdu_quality_limiter limiter) throws KduException {
        int c;
        Kdu_coords refSubs = new Kdu_coords();
        Kdu_coords subs = new Kdu_coords();
        codestream.Get_subsampling(reference_component, refSubs);
        Kdu_coords minSubs = new Kdu_coords(); minSubs.Assign(refSubs);

        for (c = 0; c < channels.Get_num_channels(); c++) {
            codestream.Get_subsampling(channels.Get_source_component(c),subs);
            if (subs.Get_x() < minSubs.Get_x()) {
                minSubs.Set_x(subs.Get_x());
            }
            if (subs.Get_y() < minSubs.Get_y()) {
                minSubs.Set_y(subs.Get_y());
            }
        }

        Kdu_coords expansion = new Kdu_coords();
        expansion.Set_x((int) Math.round(refSubs.Get_x() / (double) minSubs.Get_x()));
        expansion.Set_y((int) Math.round(refSubs.Get_y() / (double) minSubs.Get_y()));

        for (c = 0; c < channels.Get_num_channels(); c++) {
            codestream.Get_subsampling(channels.Get_source_component(c),subs);

            if ((((subs.Get_x() * expansion.Get_x()) % refSubs.Get_x()) != 0) ||
                    (((subs.Get_y() * expansion.Get_y()) % refSubs.Get_y()) != 0)) {
                Kdu_global.Kdu_print_error(
                        "The supplied JP2 file contains colour channels " +
                                "whose sub-sampling factors are not integer " +
                                "multiples of one another.");
                codestream.Apply_input_restrictions(0, 1, 0, 0, null,
                        Kdu_global.KDU_WANT_OUTPUT_COMPONENTS,
                        threadEnv, limiter);
                channels.Configure(codestream);
                expansion = new Kdu_coords(1, 1);
            }
        }
        return expansion;
    }

}
