package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>Provides generic functionality for applying transformations to {@link
 * BufferedImage}s and {@link BufferedImageSequence}s using Java 2D.</p>
 *
 * <p>This class could be thought of as a higher-level interface than the
 * various {@link Java2DUtil} methods.</p>
 *
 * @since 4.1
 */
final class Java2DPostProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Java2DPostProcessor.class);

    /**
     * Can be used for all images but not {@link BufferedImageSequence image
     * sequences}; for those, use {@link #postProcess(BufferedImageSequence,
     * OperationList, Info)}.
     *
     * @param image           Image to process.
     * @param readerHints     Hints from the image reader. May be
     *                        {@literal null}.
     * @param opList          Operations to apply to the image.
     * @param info            Information about the source image.
     * @param reductionFactor May be {@literal null}.
     */
    static BufferedImage postProcess(BufferedImage image,
                                     Set<ReaderHint> readerHints,
                                     OperationList opList,
                                     Info info,
                                     ReductionFactor reductionFactor) {
        if (reductionFactor == null) {
            reductionFactor = new ReductionFactor();
        }
        if (readerHints == null) {
            readerHints = EnumSet.noneOf(ReaderHint.class);
        }

        image = Java2DUtil.reduceTo8Bits(image);

        final Dimension fullSize = info.getSize();

        // N.B.: Any Crop or Rotate operations present in the operation list
        // have already been corrected for this orientation, but we also need
        // to account for operation lists that don't include one or both of
        // those.
        Orientation orientation = Orientation.ROTATE_0;
        if (!readerHints.contains(ReaderHint.ALREADY_ORIENTED)) {
            final Metadata metadata = info.getMetadata();
            if (metadata != null) {
                orientation = metadata.getOrientation();
            }
        }

        // Apply the crop operation, if present, and retain a reference
        // to it for subsequent operations to refer to.
        Crop crop = new CropByPercent();
        for (Operation op : opList) {
            if (op instanceof Crop) {
                crop = (Crop) op;
                if (crop.hasEffect(fullSize, opList) &&
                        !readerHints.contains(ReaderHint.ALREADY_CROPPED)) {
                    image = Java2DUtil.crop(image, crop, reductionFactor,
                            opList.getScaleConstraint());
                }
            }
        }

        if (!readerHints.contains(ReaderHint.ALREADY_ORIENTED) &&
                !Orientation.ROTATE_0.equals(orientation)) {
            image = Java2DUtil.rotate(image, orientation);
        }

        // Apply redactions.
        final Set<Redaction> redactions = opList.stream()
                .filter(op -> op instanceof Redaction &&
                        op.hasEffect(fullSize, opList))
                .map(op -> (Redaction) op)
                .collect(Collectors.toSet());
        Java2DUtil.applyRedactions(image, fullSize, crop,
                new double[] { 1.0, 1.0 }, reductionFactor,
                opList.getScaleConstraint(), redactions);

        // Apply remaining operations.
        for (Operation op : opList) {
            if (!op.hasEffect(fullSize, opList)) {
                continue;
            }
            if (op instanceof Scale) {
                final Scale scale = (Scale) op;
                final boolean isLinear = scale.isLinear() &&
                        !scale.isUp(fullSize, opList.getScaleConstraint());
                if (isLinear) {
                    image = Java2DUtil.convertColorToLinearRGB(image);
                }
                image = Java2DUtil.scale(image, scale,
                        opList.getScaleConstraint(), reductionFactor, true);
                if (isLinear) {
                    image = Java2DUtil.convertColorToSRGB(image);
                }
            } else if (op instanceof Transpose) {
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
        return image;
    }

    /**
     * For processing {@link BufferedImageSequence image sequences}, such as to
     * support animated GIFs.
     *
     * @param sequence Sequence containing one or more images, which will be
     *                 replaced with the post-processed variants.
     * @param opList   Operations to apply to each image in the sequence.
     * @param info     Information about the source image.
     * @throws IllegalArgumentException if the sequence is empty.
     */
    static void postProcess(final BufferedImageSequence sequence,
                            final OperationList opList,
                            final Info info) throws IOException {
        final int numFrames = sequence.length();

        // 1. If the sequence contains no frames, throw an exception.
        // 2. If it contains only one frame, process the frame in the current
        //    thread.
        // 3. If it contains more than one frame, spread the work across one
        //    thread per CPU.
        if (numFrames < 1) {
            throw new IllegalArgumentException("Empty sequence");
        } else if (numFrames == 1) {
            BufferedImage image = sequence.get(0);
            image = postProcess(image, null, opList, info, null);
            sequence.set(0, image);
        } else {
            final int numThreads = Math.min(
                    numFrames, Runtime.getRuntime().availableProcessors());
            final int framesPerThread =
                    (int) Math.ceil(numFrames / (float) numThreads);
            final CountDownLatch latch = new CountDownLatch(numFrames);

            LOGGER.debug("Processing {} frames in {} threads ({} frames/thread)",
                    numFrames, numThreads, framesPerThread);

            // Create a list containing numThreads queues. Each map will
            // contain { "frame": int, "image": BufferedImage }.
            final List<Queue<Map<String,Object>>> processingQueues =
                    new ArrayList<>(numThreads);
            for (short thread = 0; thread < numThreads; thread++) {
                final Queue<Map<String,Object>> queue = new LinkedList<>();
                processingQueues.add(queue);

                final int startFrame = thread * framesPerThread;
                final int endFrame = Math.min(startFrame + framesPerThread,
                        numFrames);
                for (int frameNum = startFrame; frameNum < endFrame; frameNum++) {
                    Map<String,Object> map = new HashMap<>();
                    map.put("frame", frameNum);
                    map.put("image", sequence.get(frameNum));
                    queue.add(map);
                }
            }

            // Process each queue in a separate thread.
            int i = 0;
            for (Queue<Map<String,Object>> queue : processingQueues) {
                final int queueNum = i;
                ThreadPool.getInstance().submit(() -> {
                    Map<String,Object> dict;
                    while ((dict = queue.poll()) != null) {
                        int frameNum = (int) dict.get("frame");
                        LOGGER.trace("Thread {}: processing frame {} (latch count: {})",
                                queueNum, frameNum, latch.getCount());
                        BufferedImage image = (BufferedImage) dict.get("image");
                        image = postProcess(image, null, opList, info, null);
                        sequence.set(frameNum, image);
                        latch.countDown();
                    }
                    return null;
                });
                i++;
            }

            // Wait for all threads to finish.
            try {
                latch.await(5, TimeUnit.MINUTES); // hopefully not this long...
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }

    private Java2DPostProcessor() {}

}
