package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.Normalize;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.processor.imageio.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.processor.imageio.Metadata;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

abstract class AbstractJava2DProcessor extends AbstractImageIOProcessor {

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            Collections.unmodifiableSet(EnumSet.of(
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
                    ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = Collections.unmodifiableSet(EnumSet.of(
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = Collections.unmodifiableSet(EnumSet.of(
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));

    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features;
        if (!getAvailableOutputFormats().isEmpty()) {
            features = SUPPORTED_FEATURES;
        } else {
            features = Collections.unmodifiableSet(Collections.emptySet());
        }
        return features;
    }

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

    /**
     * Convenience method for processors that use a Java 2D pipeline. Can be
     * used for all images but not {@link BufferedImageSequence image
     * sequences}; for those, use {@link #postProcess(BufferedImageSequence,
     * OperationList, Info, OutputStream)}.
     *
     * @param image           Image to process.
     * @param readerHints     Hints from the image reader. May be
     *                        {@literal null}.
     * @param opList          Operations to apply to the image.
     * @param imageInfo       Information about the source image.
     * @param reductionFactor May be {@literal null}.
     * @param outputStream    Output stream to write the resulting image to.
     */
    void postProcess(BufferedImage image,
                     final Set<ImageReader.Hint> readerHints,
                     final OperationList opList,
                     final Info imageInfo,
                     final ReductionFactor reductionFactor,
                     final OutputStream outputStream) throws IOException {
        image = doPostProcess(image, readerHints, opList, imageInfo,
                reductionFactor);
        new ImageWriter(opList).write(image, outputStream);
    }

    /**
     * Variation of {@link #postProcess(BufferedImage, Set, OperationList,
     * Info, ReductionFactor, OutputStream)} for processing
     * {@link BufferedImageSequence image sequences}, such as to support
     * animated GIFs.
     *
     * @param sequence     Sequence containing one or more images, which will
     *                     be replaced with the post-processed versions.
     * @param opList       Operations to apply to each image in the sequence.
     * @param info         Information about the source image.
     * @param outputStream Output stream to write the resulting image to.
     * @throws IllegalArgumentException if the sequence is empty.
     */
    void postProcess(final BufferedImageSequence sequence,
                     final OperationList opList,
                     final Info info,
                     final OutputStream outputStream) throws IOException {
        final int numFrames = sequence.length();

        // 1. If the sequence contains only one frame, process the frame in the
        //    current thread.
        // 2. If it contains more than one frame, spread the work across as
        //    many threads as there are CPUs.
        // 3. If it contains no frames, throw an exception.
        if (numFrames == 1) {
            BufferedImage image = sequence.get(0);
            image = doPostProcess(image, opList, info);
            sequence.set(0, image);
        } else if (numFrames > 1) {
            final int numThreads =
                    Math.min(numFrames, Runtime.getRuntime().availableProcessors());
            final int framesPerThread =
                    (int) Math.ceil(numFrames / (float) numThreads);
            final AtomicInteger numProcessed = new AtomicInteger(0);
            final Object monitor = new Object();

            for (short thread = 0; thread < numThreads; thread++) {
                final int startFrame = thread * framesPerThread;
                final int endFrame = startFrame + framesPerThread;

                ThreadPool.getInstance().submit(() -> {
                    for (int frameNum = startFrame; frameNum < endFrame; frameNum++) {
                        BufferedImage image = sequence.get(frameNum);
                        image = doPostProcess(image, opList, info);
                        sequence.set(frameNum, image);
                        numProcessed.incrementAndGet();
                    }
                    synchronized (monitor) {
                        monitor.notifyAll();
                    }
                    return null;
                });
            }

            // Wait for all threads to finish.
            while (numProcessed.get() < numFrames) {
                try {
                    synchronized (monitor) {
                        monitor.wait();
                    }
                } catch (InterruptedException e) {
                    // no-op
                }
            }
        } else {
            throw new IllegalArgumentException("Empty sequence");
        }

        Metadata metadata = getReader().getMetadata(0);
        new ImageWriter(opList, metadata).write(sequence, outputStream);
    }

    private BufferedImage doPostProcess(BufferedImage image,
                                        OperationList opList,
                                        Info imageInfo) throws IOException {
        return doPostProcess(image, null, opList, imageInfo, null);
    }

    private BufferedImage doPostProcess(BufferedImage image,
                                        Set<ImageReader.Hint> readerHints,
                                        final OperationList opList,
                                        final Info imageInfo,
                                        ReductionFactor reductionFactor) throws IOException {
        final Format outputFormat = opList.getOutputFormat();

        if (reductionFactor == null) {
            reductionFactor = new ReductionFactor();
        }
        if (readerHints == null) {
            readerHints = Collections.emptySet();
        }

        if (opList.getFirst(Normalize.class) != null) {
            image = Java2DUtil.stretchContrast(image);
        }

        // If the Encode operation specifies a max sample size of 8 bits, or if
        // the output format's max sample size is 8 bits, we will need to
        // clamp the image's sample size to 8 bits. HOWEVER, if the output
        // format's max sample size is LESS THAN 8 bits (e.g. GIF), don't do
        // anything and let the writer handle it.
        //
        // The writer could actually do this itself regardless, but doing it
        // here could make subsequent processing steps more efficient as they
        // will have less data to deal with.
        Encode encode = (Encode) opList.getFirst(Encode.class);
        if (((encode != null && encode.getMaxSampleSize() != null && encode.getMaxSampleSize() <= 8)
                || outputFormat.getMaxSampleSize() <= 8)
                && !Format.GIF.equals(outputFormat)) {
            image = Java2DUtil.reduceTo8Bits(image);
        }

        final Dimension fullSize = imageInfo.getSize();

        // Apply the crop operation, if present, and retain a reference
        // to it for subsequent operations to refer to.
        Crop crop = new Crop(0, 0, image.getWidth(), image.getHeight(),
                imageInfo.getOrientation(), imageInfo.getSize());
        for (Operation op : opList) {
            if (op instanceof Crop) {
                crop = (Crop) op;
                if (crop.hasEffect(fullSize, opList) &&
                        !readerHints.contains(ImageReader.Hint.ALREADY_CROPPED)) {
                    image = Java2DUtil.cropImage(image, crop, reductionFactor);
                }
            }
        }

        // Redactions happen immediately after cropping.
        List<Redaction> redactions = new ArrayList<>();
        for (Operation op : opList) {
            if (op instanceof Redaction) {
                if (op.hasEffect(fullSize, opList)) {
                    redactions.add((Redaction) op);
                }
            }
        }
        image = Java2DUtil.applyRedactions(image, crop, reductionFactor,
                redactions);

        // Apply remaining operations.
        for (Operation op : opList) {
            if (op.hasEffect(fullSize, opList)) {
                if (op instanceof Scale) {
                    image = Java2DUtil.scaleImage(image, (Scale) op,
                            reductionFactor);
                } else if (op instanceof Transpose) {
                    image = Java2DUtil.transposeImage(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = Java2DUtil.rotateImage(image, (Rotate) op);
                } else if (op instanceof ColorTransform) {
                    image = Java2DUtil.transformColor(image, (ColorTransform) op);
                } else if (op instanceof Sharpen) {
                    image = Java2DUtil.sharpenImage(image, (Sharpen) op);
                } else if (op instanceof Overlay) {
                    image = Java2DUtil.applyOverlay(image, (Overlay) op);
                }
            }
        }

        return image;
    }

}
