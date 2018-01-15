package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Normalize;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.imageio.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Processor using the Java 2D and ImageIO libraries.</p>
 */
class Java2dProcessor extends AbstractJava2DProcessor
        implements StreamProcessor, FileProcessor {

    @Override
    public void process(final OperationList ops,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(ops, imageInfo, outputStream);

        ImageReader reader = null;
        try {
            reader = getReader();
            final ReductionFactor rf = new ReductionFactor();
            final Set<ImageReader.Hint> hints =
                    EnumSet.noneOf(ImageReader.Hint.class);

            if (ops.getFirst(Normalize.class) != null) {
                // When normalizing, the reader needs to read the entire image
                // so that its histogram can be sampled accurately, which will
                // preserve the luminance across tiles.
                hints.add(ImageReader.Hint.IGNORE_CROP);
            }

            // If the source and output formats are both GIF, the source may
            // contain multiple frames, in which case the post-processing steps
            // will have to be different. (No problem if it only contains one
            // frame, though.)
            if (Format.GIF.equals(imageInfo.getSourceFormat()) &&
                    Format.GIF.equals(ops.getOutputFormat())) {
                BufferedImageSequence seq = reader.readSequence();
                postProcess(seq, ops, imageInfo, outputStream);
            } else {
                BufferedImage image =
                        reader.read(ops, imageInfo.getOrientation(), rf, hints);
                postProcess(image, hints, ops, imageInfo, rf, outputStream);
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

}
