package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
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

        private ImageInputStream inputStream;

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
     * getting the CPU count. Unknown whether it's any more or less reliable.
     */
    private static final int NUM_THREADS =
            Math.max(1, Runtime.getRuntime().availableProcessors());

    // N.B.: see the end notes in the KduRender.java file in the Kakadu SDK
    // for explanation of how this stuff needs to be destroyed. (And it DOES
    // need to be destroyed!)
    // N.B. 2: see KduRender2.java for use of the Kdu_thread_env.
    private Jpx_source jpxSrc                    = new Jpx_source();
    private Jp2_threadsafe_family_src familySrc  = new Jp2_threadsafe_family_src();
    private Kdu_compressed_source compSrc;
    private Kdu_codestream codestream            = new Kdu_codestream();
    private Kdu_channel_mapping channels         = new Kdu_channel_mapping();
    private Kdu_region_decompressor decompressor = new Kdu_region_decompressor();
    private Kdu_thread_env threadEnv             = new Kdu_thread_env();
    private Kdu_quality_limiter limiter          = new Kdu_quality_limiter(1 / 256f, false);

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

    private boolean isOpenAttempted, haveReadInfo, haveReadXMP;
    private int width, height, tileWidth, tileHeight, numDWTLevels = -1;
    private String xmp;

    private void handle(KduException e) {
        try {
            threadEnv.Handle_exception(e.Get_kdu_exception_code());
        } catch (KduException ke) {
            LOGGER.warn("{} (code: {})",
                    ke.getMessage(),
                    Integer.toHexString(ke.Get_kdu_exception_code()),
                    ke);
        }
    }

    @Override
    public void close() {
        limiter.Native_destroy();

        try {
            threadEnv.Destroy();
        } catch (KduException e) {
            LOGGER.warn("Failed to destroy the kdu_thread_env: {} (code: {})",
                    e.getMessage(),
                    Integer.toHexString(e.Get_kdu_exception_code()));
        }
        threadEnv.Native_destroy();
        decompressor.Native_destroy();
        channels.Native_destroy();
        try {
            if (codestream.Exists()) {
                codestream.Destroy();
            }
        } catch (KduException e) {
            LOGGER.warn("Failed to destroy the kdu_codestream: {} (code: {})",
                    e.getMessage(),
                    Integer.toHexString(e.Get_kdu_exception_code()));
        }
        if (compSrc != null) {
            compSrc.Native_destroy();
        }
        jpxSrc.Native_destroy();
        familySrc.Native_destroy();

        IOUtils.closeQuietly(inputStream);
    }

    public int getHeight() throws IOException {
        if (height == 0) {
            openImage();
            readInfo();
        }
        return height;
    }

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
        if (!haveReadXMP) {
            haveReadXMP = true;
            try {
                openImage();

                // Access the Jpx_source's metadata manager, which provides
                // access to all of the various metadata boxes. The one we're
                // interested in is a UUID box containing XMP data.
                final Jpx_meta_manager manager = jpxSrc.Access_meta_manager();
                manager.Set_box_filter(1, new long[]{Kdu_global.jp2_uuid_4cc});

                Jpx_metanode lastNode = new Jpx_metanode();
                do {
                    manager.Load_matches(-1, new int[]{}, -1, new int[]{});
                    lastNode = manager.Enumerate_matches(
                            lastNode, -1, -1, false, new Kdu_dims(), 0);
                    if (lastNode.Is_xmp_uuid()) {
                        final Jpx_input_box xmpBox = new Jpx_input_box();
                        try {
                            lastNode.Open_existing(xmpBox);
                            final int bufferSize = (int) xmpBox.Get_box_bytes();
                            byte[] buffer = new byte[bufferSize];
                            xmpBox.Read(buffer, bufferSize);
                            xmp = new String(buffer, StandardCharsets.UTF_8);
                            xmp = StringUtils.trimXMP(xmp);
                        } finally {
                            xmpBox.Close();
                        }
                        break;
                    }
                } while (lastNode.Exists());
            } catch (KduException e) {
                handle(e);
                throw new IOException(e);
            }
        }
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

        // Initialize error handling. (Is it just me or is Kakadu's error
        // handling system not thread-safe?)
        KduDebugMessage sysout             = new KduDebugMessage();
        KduErrorMessage syserr             = new KduErrorMessage();
        Kdu_message_formatter prettySysout = new Kdu_message_formatter(sysout);
        Kdu_message_formatter prettySyserr = new Kdu_message_formatter(syserr);
        try {
            Kdu_global.Kdu_customize_warnings(prettySysout);
            Kdu_global.Kdu_customize_errors(prettySyserr);

            if (sourceFile != null) {
                familySrc.Open(sourceFile.toString());
            } else {
                compSrc = new KduImageInputStreamSource(inputStream);
                familySrc.Open(compSrc);
            }

            Jpx_layer_source xLayer       = null;
            Jpx_codestream_source xStream = null;
            int success = jpxSrc.Open(familySrc, true);
            if (success >= 0) {
                // Succeeded in opening as wrapped JP2/JPX source.
                xLayer  = jpxSrc.Access_layer(0);
                xStream = jpxSrc.Access_codestream(xLayer.Get_codestream_id(0));
                compSrc = xStream.Open_stream();
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
            if (xLayer != null) {
                channels.Configure(
                        xLayer.Access_colour(0), xLayer.Access_channels(),
                        xStream.Get_codestream_id(),
                        xStream.Access_palette(),
                        xStream.Access_dimensions());
            } else {
                channels.Configure(codestream);
            }
        } catch (KduException e) {
            handle(e);
            throw new IOException(e);
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
            final int referenceComponent = channels.Get_source_component(0);
            Kdu_dims image_dims = new Kdu_dims();
            codestream.Get_dims(referenceComponent, image_dims);
            width = image_dims.Access_size().Get_x();
            height = image_dims.Access_size().Get_y();

            // Read the tile dimensions.
            Kdu_coords tileIndex = new Kdu_coords();
            Kdu_dims tileDims = new Kdu_dims();
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
        // Note: Kdu_dims and Kdu_coords are integer-based, and this can lead
        // to precision loss when Rectangles and Dimensions are converted
        // back-and-forth... and precision loss can cause crashes in
        // Kdu_region_decompressor. Try to stay in the Rectangle/Dimension
        // space and convert into the libkdu equivalent objects only when
        // necessary.

        // Find the best resolution level to read.
        if (scaleOp != null) {
            final double scales[] = scaleOp.getResultingScales(
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

        try {
            openImage();

            final int referenceComponent = channels.Get_source_component(0);
            final int accessMode = Kdu_global.KDU_WANT_OUTPUT_COMPONENTS;

            limiter.Set_display_resolution(600f, 600f);

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

            // Get the effective source image size.
            final Kdu_dims sourceDims = decompressor.Get_rendered_image_dims(
                    codestream, channels, -1, reductionFactor.factor,
                    expandNumerator, expandDenominator, accessMode);
            final Kdu_coords sourcePos  = sourceDims.Access_pos();

            final Rectangle regionRect = new Rectangle(roi);

            // Adjust the ROI coordinates for the selected decomposition level.
            // Note that some wacky source images have a non-0,0 origin,
            // in which case the ROI origin must be shifted to match.
            final double reducedScale = reductionFactor.getScale();
            regionRect.moveRight(sourcePos.Get_x());
            regionRect.moveDown(sourcePos.Get_y());
            regionRect.scaleX(diffScales[0] * reducedScale);
            regionRect.scaleY(diffScales[1] * reducedScale);

            Rectangle sourceRect = toRectangle(sourceDims);
            sourceRect.scaleX(reducedScale);
            sourceRect.scaleY(reducedScale);

            // N.B.: if the region is not entirely within the source image
            // coordinates, either kdu_region_decompressor::start() or
            // process() will crash the JVM (which may be a bug in Kakadu
            // or its Java binding). This should never be the case, but we
            // check anyway to be extra safe.
            //
            // N.B. 2: sometimes one of the sourceRect dimensions is slightly
            // (usually < 0.2 anecdotally) smaller than its regionRect
            // dimension. The delta argument to contains() works around this,
            // but it would be better to TODO: find out why the discrepancy exists and fix it.
            if (!sourceRect.contains(regionRect, 0.2)) {
                throw new IllegalArgumentException(String.format(
                        "Rendered region is not entirely within the image " +
                                "on the canvas. This might be a bug. " +
                                "[region: %s] [image: %s]",
                        regionRect, sourceRect));
            }

            LOGGER.debug("Rendered region {}; source {}; " +
                            "{}x reduction factor; differential scale {}/{}",
                    regionRect, sourceRect,
                    reductionFactor.factor,
                    expandNumerator.Get_x(), expandDenominator.Get_x()); // y should == x

            final Kdu_dims regionDims = toKduDims(regionRect);

            final Stopwatch watch = new Stopwatch();

            decompressor.Start(codestream, channels, -1,
                    reductionFactor.factor, MAX_LAYERS, regionDims,
                    expandNumerator, expandDenominator, false, accessMode,
                    false, threadEnv);

            Kdu_dims newRegion        = new Kdu_dims();
            Kdu_dims incompleteRegion = new Kdu_dims();
            Kdu_dims viewDims         = new Kdu_dims();
            viewDims.Assign(regionDims);
            incompleteRegion.Assign(regionDims);

            image = new BufferedImage(
                    regionDims.Access_size().Get_x(),
                    regionDims.Access_size().Get_y(),
                    BufferedImage.TYPE_INT_ARGB);

            final int regionBufferSize = regionDims.Access_size().Get_x() * 128;
            final int[] regionBuffer   = new int[regionBufferSize];

            while (decompressor.Process(regionBuffer, regionDims.Access_pos(),
                    0, 0, regionBufferSize, incompleteRegion, newRegion)) {
                Kdu_coords newPos = newRegion.Access_pos();
                Kdu_coords newSize = newRegion.Access_size();
                newPos.Subtract(viewDims.Access_pos());

                int bufferIndex = 0;
                for (int y = newPos.Get_y(); y < newPos.Get_y() + newSize.Get_y(); y++) {
                    for (int x = newPos.Get_x(); x < newSize.Get_x(); x++) {
                        image.setRGB(x, y, regionBuffer[bufferIndex++]);
                    }
                }
            }
            if (decompressor.Finish()) {
                if (reductionFactor.factor - 1 > getNumDecompositionLevels()) {
                    LOGGER.error("Insufficient DWT levels ({}) for reduction factor ({})",
                            codestream.Get_min_dwt_levels(),
                            reductionFactor.factor);
                }
                LOGGER.trace("readRegion(): read in {}", watch);
            }
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

        return image;
    }

    private static Kdu_dims toKduDims(Rectangle rect) throws KduException {
        Kdu_dims regionDims = new Kdu_dims();
        regionDims.From_u32(rect.intX(), rect.intY(),
                rect.intWidth(), rect.intHeight());
        return regionDims;
    }

    private static Rectangle toRectangle(Kdu_dims dims) throws KduException {
        return new Rectangle(
                dims.Access_pos().Get_x(),
                dims.Access_pos().Get_y(),
                dims.Access_size().Get_x(),
                dims.Access_size().Get_y());
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
        Kdu_coords ref_subs = new Kdu_coords();
        Kdu_coords subs = new Kdu_coords();
        codestream.Get_subsampling(reference_component,ref_subs);
        Kdu_coords min_subs = new Kdu_coords(); min_subs.Assign(ref_subs);

        for (c = 0; c < channels.Get_num_channels(); c++) {
            codestream.Get_subsampling(channels.Get_source_component(c),subs);
            if (subs.Get_x() < min_subs.Get_x()) {
                min_subs.Set_x(subs.Get_x());
            }
            if (subs.Get_y() < min_subs.Get_y()) {
                min_subs.Set_y(subs.Get_y());
            }
        }

        Kdu_coords expansion = new Kdu_coords();
        expansion.Set_x((int) Math.round(ref_subs.Get_x() / (double) min_subs.Get_x()));
        expansion.Set_y((int) Math.round(ref_subs.Get_y() / (double) min_subs.Get_y()));

        for (c = 0; c < channels.Get_num_channels(); c++) {
            codestream.Get_subsampling(channels.Get_source_component(c),subs);

            if ((((subs.Get_x() * expansion.Get_x()) % ref_subs.Get_x()) != 0) ||
                    (((subs.Get_y() * expansion.Get_y()) % ref_subs.Get_y()) != 0)) {
                Kdu_global.Kdu_print_error(
                        "The supplied JP2 file contains colour channels " +
                                "whose sub-sampling factors are not integer " +
                                "multiples of one another.");
                codestream.Apply_input_restrictions(0, 1, 0, 0, null,
                        Kdu_global.KDU_WANT_OUTPUT_COMPONENTS,
                        threadEnv, limiter);
                channels.Configure(codestream);
                expansion = new Kdu_coords(1,1);
            }
        }
        return expansion;
    }

}
