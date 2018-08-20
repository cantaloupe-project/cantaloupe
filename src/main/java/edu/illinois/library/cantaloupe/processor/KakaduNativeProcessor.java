package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Normalize;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_input_box;
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
import kdu_jni.Kdu_region_decompressor;
import kdu_jni.Kdu_thread_env;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the Kakadu native library ({@literal libkdu}) via the
 * Java Native Interface (JNI). Written against version 7.10.</p>
 *
 * <p>A {@link Kdu_region_decompressor} is used to acquire a cropped and scaled
 * image that is {@link BufferedImage buffered in memory}, and Java 2D is used
 * for all post-scale processing steps.</p>
 *
 * <h1>Comparison with {@link KakaduDemoProcessor}</h1>
 *
 * <p>Compared to {@link KakaduDemoProcessor}, this one offers a number of
 * benefits:</p>
 *
 * <ul>
 *     <li>It doesn't need to invoke a process.</li>
 *     <li>It doesn't have to do intermediary conversions to and from TIFF.</li>
 *     <li>It doesn't have to do differential scaling in Java, and instead uses
 *     the high-quality, hardware-accelerated scaler built into
 *     <code>kdu_region_decompress()</code>.</li>
 *     <li>It can read from both {@link FileProcessor files} and {@link
 *     StreamProcessor streams}.</li>
 *     <li>It works equally efficiently in Windows.</li>
 *     <li>It doesn't have to resort to silly tricks involving symlinks and
 *     {@literal /dev/stdout}.</li>
 * </ul>
 *
 * <p>As well as some drawbacks:</p>
 *
 * <ol>
 *     <li>Despite the efficiency advantages described above, {@link
 *     Kdu_region_decompressor} is high-level API that doesn't benefit from
 *     the expert tuning of {@literal kdu_expand}, and isn't able to achieve
 *     the same effective performance.</li>
 * </ol>
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
 * @since 4.0
 * @author Alex Dolski UIUC
 */
class KakaduNativeProcessor implements FileProcessor, StreamProcessor {

    /**
     * Custom {@link Kdu_compressed_source_nonnative} that supplies data from
     * an {@link ImageInputStream}.
     */
    private static class KduImageInputStreamSource
            extends Kdu_compressed_source_nonnative {

        private ImageInputStream inputStream;

        KduImageInputStreamSource(ImageInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int Get_capabilities() {
            // ImageInputStream is seekable, even if it has to employ buffering
            // (will depend on the implementation).
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
            LoggerFactory.getLogger(KakaduNativeProcessor.class);

    /**
     * This is set to roughly 210x smaller than {@link Integer#MAX_VALUE} to
     * allow for 210x enlargement, still with good precision.
     *
     * @see #readRegion
     */
    private static final int EXPAND_DENOMINATOR = 10000000;
    private static final int MAX_LAYERS = 16384;
    private static final int NUM_THREADS =
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES = EnumSet.of(
            ProcessorFeature.MIRRORING,
            ProcessorFeature.REGION_BY_PERCENT,
            ProcessorFeature.REGION_BY_PIXELS,
            ProcessorFeature.REGION_SQUARE,
            ProcessorFeature.ROTATION_ARBITRARY,
            ProcessorFeature.ROTATION_BY_90S,
            ProcessorFeature.SIZE_ABOVE_FULL,
            ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
            ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
            ProcessorFeature.SIZE_BY_HEIGHT,
            ProcessorFeature.SIZE_BY_PERCENT,
            ProcessorFeature.SIZE_BY_WIDTH,
            ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);

    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = EnumSet.of(
            edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
            edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
            edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
            edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE);

    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = EnumSet.of(
            edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
            edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
            edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
            edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY);

    private static final AtomicBoolean IS_CLASS_INITIALIZED = new AtomicBoolean();
    private static InitializationException initializationException;

    /**
     * Will be {@literal null} if {@link #streamFactory} isn't.
     */
    private Path sourceFile;

    /**
     * Will be {@literal null} if {@link #sourceFile} isn't.
     */
    private StreamFactory streamFactory;

    private Format sourceFormat;

    private static synchronized void initializeClass() {
        if (!IS_CLASS_INITIALIZED.get()) {
            IS_CLASS_INITIALIZED.set(true);
            try {
                Kdu_global.Kdu_get_core_version(); // call something trivial
            } catch (KduException | UnsatisfiedLinkError e) {
                initializationException = new InitializationException(e);
            }
        }
    }

    static synchronized void resetInitialization() {
        IS_CLASS_INITIALIZED.set(false);
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        return ImageWriterFactory.supportedFormats();
    }

    @Override
    public InitializationException getInitializationException() {
        initializeClass();
        return initializationException;
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public Format getSourceFormat() {
        return sourceFormat;
    }

    @Override
    public StreamFactory getStreamFactory() {
        return streamFactory;
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features;
        if (!getAvailableOutputFormats().isEmpty()) {
            features = SUPPORTED_FEATURES;
        } else {
            features = Collections.unmodifiableSet(Collections.emptySet());
        }
        return features;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIIIF1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities;
        if (!getAvailableOutputFormats().isEmpty()) {
            qualities = SUPPORTED_IIIF_1_1_QUALITIES;
        } else {
            qualities = Collections.unmodifiableSet(Collections.emptySet());
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIIIF2Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities;
        if (!getAvailableOutputFormats().isEmpty()) {
            qualities = SUPPORTED_IIIF_2_0_QUALITIES;
        } else {
            qualities = Collections.unmodifiableSet(Collections.emptySet());
        }
        return qualities;
    }

    @Override
    public void setSourceFile(Path file) {
        this.sourceFile = file;
    }

    @Override
    public void setSourceFormat(Format format)
            throws UnsupportedSourceFormatException {
        if (!Format.JP2.equals(format)) {
            throw new UnsupportedSourceFormatException(format);
        }
        this.sourceFormat = format;
    }

    @Override
    public void setStreamFactory(StreamFactory streamFactory) {
        this.streamFactory = streamFactory;
    }

    /**
     * N.B.: This method does not support {@link Normalize}.
     *
     * {@inheritDoc}
     */
    @Override
    public void process(final OperationList opList,
                        final Info info,
                        final OutputStream outputStream) throws ProcessorException {
        try {
            final ReductionFactor reductionFactor = new ReductionFactor();
            final BufferedImage image = readRegion(
                    opList,
                    info.getSize(),
                    info.getNumResolutions() - 1,
                    reductionFactor);
            postProcess(image, opList, info, reductionFactor, outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Reads a region of interest into a {@link BufferedImage}.
     *
     * @param fullSize        Full size of the source image.
     * @param numLevels       Number of decomposition levels (not resolution
     *                        levels) contained in the source image.
     * @param reductionFactor {@link ReductionFactor#factor} will be set
     *                        according to the given {@link Scale}.
     * @param opList          List of operations to apply. Only some will be
     *                        used.
     */
    private BufferedImage readRegion(final OperationList opList,
                                     final Dimension fullSize,
                                     final int numLevels,
                                     final ReductionFactor reductionFactor) throws IOException {
        final Rectangle roi = getRegion(opList, fullSize);
        final Scale scaleOp = (Scale) opList.getFirst(Scale.class);

        // Find the best resolution level to read.
        if (scaleOp != null) {
            final double scales[] = scaleOp.getResultingScales(
                    roi.size(), opList.getScaleConstraint());
            // If x & y scales are different, base the reduction factor on the
            // largest scale.
            final double maxScale = Arrays.stream(scales).max().orElse(1);
            reductionFactor.factor =
                    ReductionFactor.forScale(maxScale, 0.001).factor;

            // Clamp the reduction factor to the range of 0-numLevels.
            if (reductionFactor.factor < 0) {
                reductionFactor.factor = 0;
            } else if (reductionFactor.factor > numLevels) {
                reductionFactor.factor = numLevels;
            }
        }

        BufferedImage image;

        try {
            // Initialize error handling. (Is it just me or is Kakadu's error
            // handling system not thread-safe?)
            KduDebugMessage sysout = new KduDebugMessage();
            KduErrorMessage syserr = new KduErrorMessage();
            Kdu_message_formatter prettySysout =
                    new Kdu_message_formatter(sysout);
            Kdu_message_formatter prettySyserr =
                    new Kdu_message_formatter(syserr);
            Kdu_global.Kdu_customize_warnings(prettySysout);
            Kdu_global.Kdu_customize_errors(prettySyserr);

            // N.B.: see the end notes in the KduRender.java file in the Kakadu
            // SDK for explanation of how these need to be destroyed.
            // N.B. 2: see KduRender2.java for use of the Kdu_thread_env.
            final Jpx_source jpxSrc = new Jpx_source();
            final Jp2_threadsafe_family_src familySrc = new Jp2_threadsafe_family_src();
            Kdu_compressed_source compSrc = null;
            final Kdu_codestream codestream = new Kdu_codestream();
            final Kdu_channel_mapping channels = new Kdu_channel_mapping();
            final Kdu_region_decompressor decompressor = new Kdu_region_decompressor();
            ImageInputStream inputStream = null;
            final Kdu_thread_env threadEnv = new Kdu_thread_env();

            try {
                if (sourceFile != null) {
                    familySrc.Open(sourceFile.toString(), true);
                } else {
                    inputStream = streamFactory.newImageInputStream();
                    compSrc = new KduImageInputStreamSource(inputStream);
                    familySrc.Open(compSrc);
                }
                jpxSrc.Open(familySrc, false);

                Jpx_codestream_source codestreamSrc = jpxSrc.Access_codestream(0);
                Jpx_input_box inputBox = codestreamSrc.Open_stream();

                threadEnv.Create();
                for (int t = 0; t < NUM_THREADS; t++) {
                    threadEnv.Add_thread();
                }

                codestream.Create(inputBox, threadEnv);

                channels.Configure(codestream);

                final int referenceComponent = channels.Get_source_component(0);
                final int accessMode = Kdu_global.KDU_WANT_OUTPUT_COMPONENTS;

                // Get the ROI coordinates relative to the source image.
                final Kdu_dims regionDims = toKduDims(roi);
                final Kdu_coords regionPos = regionDims.Access_pos();
                final Kdu_coords regionSize = regionDims.Access_size();

                // The expand numerator & denominator here tell
                // kdu_region_decompressor.start() at what fractional scale to
                // render the result. The expansion is relative to the selected
                // resolution level, NOT the full image dimensions.
                final Kdu_coords expandNumerator = determineReferenceExpansion(
                        referenceComponent, channels, codestream);
                final Dimension regionArea = new Dimension(
                        regionSize.Get_x(), regionSize.Get_y());
                final double[] diffScales = (scaleOp != null)
                        ? scaleOp.getDifferentialScales(
                                regionArea, reductionFactor, opList.getScaleConstraint())
                        : new double[] {1.0, 1.0};
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
                final Kdu_coords sourcePos = sourceDims.Access_pos();
                final Kdu_coords sourceSize = sourceDims.Access_size();

                // Adjust the ROI coordinates for the selected decomposition level.
                // Note that some wacky source images have a non-0,0 origin,
                // in which case the ROI origin must be shifted to match.
                final double reducedScale = reductionFactor.getScale();
                regionPos.Set_x(sourcePos.Get_x() +
                        (int) Math.floor(regionPos.Get_x() * diffScales[0] * reducedScale));
                regionPos.Set_y(sourcePos.Get_y() +
                        (int) Math.floor(regionPos.Get_y() * diffScales[1] * reducedScale));
                regionSize.Set_x((int) Math.floor(regionSize.Get_x() * diffScales[0] * reducedScale));
                regionSize.Set_y((int) Math.floor(regionSize.Get_y() * diffScales[1] * reducedScale));

                // N.B.: if the region is not entirely within the source image
                // coordinates, either kdu_region_decompressor::start() or
                // process() will crash the JVM (which may be a bug in Kakadu
                // or its Java binding). This should never be the case, but we
                // check anyway to be safe.
                if (!sourceDims.Contains(regionDims)) {
                    throw new IllegalArgumentException(String.format(
                            "Rendered region is not entirely within the full " +
                                    "image on the canvas. This is probably a bug. " +
                                    "(Region: %d,%d/%dx%d; image: %d,%d/%dx%d)",
                            regionPos.Get_x(), regionPos.Get_y(),
                            regionSize.Get_x(), regionSize.Get_y(),
                            sourcePos.Get_x(), sourcePos.Get_y(),
                            sourceSize.Get_x(), sourceSize.Get_y()));
                }

                LOGGER.debug("Rendered region {},{}/{}x{}; " +
                                "source {},{}/{}x{}; {}x reduction factor; " +
                                "differential scale {}/{}",
                        regionPos.Get_x(), regionPos.Get_y(),
                        regionSize.Get_x(), regionSize.Get_y(),
                        sourcePos.Get_x(), sourcePos.Get_y(),
                        sourceSize.Get_x(), sourceSize.Get_y(),
                        reductionFactor.factor,
                        expandNumerator.Get_x(), expandDenominator.Get_x()); // y should be the same

                decompressor.Start(codestream, channels, -1,
                        reductionFactor.factor, MAX_LAYERS, regionDims,
                        expandNumerator, expandDenominator, false, accessMode,
                        false, threadEnv);

                Kdu_dims newRegion = new Kdu_dims();
                Kdu_dims incompleteRegion = new Kdu_dims();
                Kdu_dims viewDims = new Kdu_dims();
                viewDims.Assign(regionDims);
                incompleteRegion.Assign(regionDims);

                image = new BufferedImage(regionSize.Get_x(), regionSize.Get_y(),
                        BufferedImage.TYPE_INT_ARGB);

                final int regionBufferSize = regionSize.Get_x() * 128;
                final int[] regionBuffer = new int[regionBufferSize];

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
                    if (reductionFactor.factor - 1 > codestream.Get_min_dwt_levels()) {
                        LOGGER.error("Insufficient DWT levels ({}) for reduction factor ({})",
                                codestream.Get_min_dwt_levels(),
                                reductionFactor.factor);
                    }
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
            } finally {
                try {
                    threadEnv.Destroy();
                } catch (KduException e) {
                    LOGGER.warn("readRegion(): failed to destroy the kdu_thread_env: {} (code: {})",
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
                    LOGGER.warn("readRegion(): failed to destroy the codestream: {} (code: {})",
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
        } catch (KduException e) {
            LOGGER.error("readRegion(): {} (code: {})",
                    e.getMessage(),
                    Integer.toHexString(e.Get_kdu_exception_code()));
            throw new IOException(e);
        }
        return image;
    }

    /**
     * Computes the effective size of an image after all crop operations are
     * applied but excluding any scale operations, in order to select the best
     * decomposition level.
     */
    private static Rectangle getRegion(OperationList opList,
                                       Dimension fullSize) {
        Rectangle regionRect = new Rectangle(0, 0,
                fullSize.width(), fullSize.height());
        Crop crop = (Crop) opList.getFirst(Crop.class);
        if (crop != null && crop.hasEffect(fullSize, opList)) {
            regionRect = crop.getRectangle(
                    fullSize, opList.getScaleConstraint());
        }
        return regionRect;
    }

    /**
     * @return Region of interest in source image coordinates.
     */
    private static Kdu_dims toKduDims(Rectangle rect) throws KduException {
        Kdu_dims regionDims = new Kdu_dims();
        regionDims.From_u32(rect.intX(), rect.intY(),
                rect.intWidth(), rect.intHeight());
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
            Kdu_codestream codestream) throws KduException {
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
        expansion.Set_x(ref_subs.Get_x() / min_subs.Get_x());
        expansion.Set_y(ref_subs.Get_y() / min_subs.Get_y());

        for (c = 0; c < channels.Get_num_channels(); c++) {
            codestream.Get_subsampling(channels.Get_source_component(c),subs);

            if ((((subs.Get_x() * expansion.Get_x()) % ref_subs.Get_x()) != 0) ||
                    (((subs.Get_y() * expansion.Get_y()) % ref_subs.Get_y()) != 0)) {
                Kdu_global.Kdu_print_error(
                        "The supplied JP2 file contains colour channels " +
                                "whose sub-sampling factors are not integer " +
                                "multiples of one another.");
                codestream.Apply_input_restrictions(0, 1, 0, 0, null,
                        Kdu_global.KDU_WANT_OUTPUT_COMPONENTS);
                channels.Configure(codestream);
                expansion = new Kdu_coords(1,1);
            }
        }
        return expansion;
    }

    /**
     * @param image           Image to process.
     * @param opList          Operations to apply to the image.
     * @param imageInfo       Information about the source image.
     * @param reductionFactor May be {@literal null}.
     * @param outputStream    Output stream to write the resulting image to.
     */
    private void postProcess(BufferedImage image,
                             final OperationList opList,
                             final Info imageInfo,
                             ReductionFactor reductionFactor,
                             final OutputStream outputStream) throws IOException {
        if (reductionFactor == null) {
            reductionFactor = new ReductionFactor();
        }

        final Dimension fullSize = imageInfo.getSize();

        // Apply redactions.
        final Set<Redaction> redactions = new HashSet<>();
        for (Operation op : opList) {
            if (op instanceof Redaction && op.hasEffect(fullSize, opList)) {
                redactions.add((Redaction) op);
            }
        }
        if (!redactions.isEmpty()) {
            Crop crop = new Crop(0, 0, image.getWidth(), image.getHeight(),
                    imageInfo.getOrientation(), imageInfo.getSize());
            Java2DUtil.applyRedactions(image, crop, reductionFactor,
                    opList.getScaleConstraint(), redactions);
        }

        // Apply remaining operations.
        for (Operation op : opList) {
            if (op.hasEffect(fullSize, opList)) {
                if (op instanceof Transpose) {
                    image = Java2DUtil.transpose(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = Java2DUtil.rotate(image, (Rotate) op);
                } else if (op instanceof ColorTransform) {
                    image = Java2DUtil.transformColor(image, (ColorTransform) op);
                } else if (op instanceof Sharpen) {
                    image = Java2DUtil.sharpen(image, (Sharpen) op);
                } else if (op instanceof Overlay) {
                    Java2DUtil.applyOverlay(image, (Overlay) op);
                }
            }
        }

        new ImageWriterFactory().newImageWriter(opList).
                write(image, outputStream);
    }

    @Override
    public Info readImageInfo() throws IOException {
        try {
            // Initialize error handling. (Is it just me or is Kakadu's error
            // handling system not thread-safe?)
            KduDebugMessage sysout = new KduDebugMessage();
            KduErrorMessage syserr = new KduErrorMessage();
            Kdu_message_formatter prettySysout =
                    new Kdu_message_formatter(sysout);
            Kdu_message_formatter prettySyserr =
                    new Kdu_message_formatter(syserr);
            Kdu_global.Kdu_customize_warnings(prettySysout);
            Kdu_global.Kdu_customize_errors(prettySyserr);

            // N.B.: see the end notes in the KduRender.java file in the Kakadu
            // SDK for explanation of how these need to be destroyed.
            ImageInputStream inputStream = null;
            final Jpx_source jpxSrc = new Jpx_source();
            Kdu_compressed_source compSrc = null;
            final Jp2_threadsafe_family_src familySrc = new Jp2_threadsafe_family_src();
            final Kdu_codestream codestream = new Kdu_codestream();
            final Kdu_channel_mapping channels = new Kdu_channel_mapping();

            try {
                if (sourceFile != null) {
                    familySrc.Open(sourceFile.toString(), true);
                } else {
                    inputStream = streamFactory.newImageInputStream();
                    compSrc = new KduImageInputStreamSource(inputStream);
                    familySrc.Open(compSrc);
                }
                jpxSrc.Open(familySrc, false);

                Jpx_codestream_source codestreamSrc = jpxSrc.Access_codestream(0);
                Jpx_input_box inputBox = codestreamSrc.Open_stream();

                codestream.Create(inputBox);

                channels.Configure(codestream);

                final int referenceComponent = channels.Get_source_component(0);

                // Get the main image dimensions.
                Kdu_dims image_dims = new Kdu_dims();
                codestream.Get_dims(referenceComponent, image_dims);
                final int width = image_dims.Access_size().Get_x();
                final int height = image_dims.Access_size().Get_y();

                // Get the tile size.
                Kdu_coords tileIndex = new Kdu_coords();
                Kdu_dims tileDims = new Kdu_dims();
                codestream.Get_tile_dims(tileIndex, -1, tileDims, false);
                int tileWidth = tileDims.Access_size().Get_x();
                int tileHeight = tileDims.Access_size().Get_y();
                // Swap dimensions to harmonize with the main image dimensions, if
                // necessary.
                if (width > height && tileWidth < tileHeight ||
                        width < height && tileWidth > tileHeight) {
                    int tmp = tileWidth;
                    tileWidth = tileHeight;
                    tileHeight = tmp;
                }

                // Get the DWT level count.
                final int levels = codestream.Get_min_dwt_levels();

                return Info.builder()
                        .withFormat(Format.JP2)
                        .withSize(width, height)
                        .withTileSize(tileWidth, tileHeight)
                        .withOrientation(Orientation.ROTATE_0) // TODO: may need to parse the EXIF to get this?
                        .withNumResolutions(levels + 1)
                        .build();
            } catch (KduException e) {
                throw new IOException(e.getMessage() + " (code: " +
                        Integer.toHexString(e.Get_kdu_exception_code()) + ")", e);
            } finally {
                if (channels != null) {
                    channels.Native_destroy();
                }

                try {
                    if (codestream.Exists()) {
                        codestream.Destroy();
                    }
                } catch (KduException e) {
                    LOGGER.error("readImageInfo(): {} (code: {})",
                            e.getMessage(),
                            Integer.toHexString(e.Get_kdu_exception_code()));
                }

                if (compSrc != null) {
                    compSrc.Native_destroy();
                }
                jpxSrc.Native_destroy();
                familySrc.Native_destroy();

                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (KduException e) {
            LOGGER.error("readImageInfo(): {} (code: {})",
                    e.getMessage(),
                    Integer.toHexString(e.Get_kdu_exception_code()));
            throw new IOException(e);
        }
    }

}
