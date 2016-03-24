package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the Java Advanced Imaging (JAI) framework.
 *
 * @see <a href="http://docs.oracle.com/cd/E19957-01/806-5413-10/806-5413-10.pdf">
 *     Programming in Java Advanced Imaging</a>
 */
class JaiProcessor extends AbstractImageIoProcessor
        implements FileProcessor, StreamProcessor {

    private static Logger logger = LoggerFactory.getLogger(JaiProcessor.class);

    static final String JPG_QUALITY_CONFIG_KEY =
            "JaiProcessor.jpg.quality";
    static final String TIF_COMPRESSION_CONFIG_KEY =
            "JaiProcessor.tif.compression";

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
        SUPPORTED_IIIF_2_0_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));
        SUPPORTED_FEATURES.addAll(Arrays.asList(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.ROTATION_ARBITRARY,
                ProcessorFeature.ROTATION_BY_90S,
                ProcessorFeature.SIZE_ABOVE_FULL,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(final OperationList ops,
                        final ImageInfo imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            final ReductionFactor rf = new ReductionFactor();
            RenderedImage renderedImage = reader.readRendered(ops, rf);

            if (renderedImage != null) {
                BufferedImage image = null;
                RenderedOp renderedOp = JaiUtil.reformatImage(
                        RenderedOp.wrapRenderedImage(renderedImage),
                        new Dimension(renderedImage.getTileWidth(),
                                renderedImage.getTileHeight()));
                for (Operation op : ops) {
                    if (op instanceof Crop) {
                        renderedOp = JaiUtil.
                                cropImage(renderedOp, (Crop) op, rf);
                    } else if (op instanceof Scale && !op.isNoOp()) {
                        Interpolation interpolation = Interpolation.
                                getInstance(Interpolation.INTERP_BILINEAR);
                        // The JAI scale operation has a bug that causes it to
                        // fail on right-edge deflate-compressed tiles when
                        // using any interpolation other than nearest-neighbor,
                        // with an ArrayIndexOutOfBoundsException in
                        // PlanarImage.cobbleByte().
                        // Example: /iiif/2/56324x18006-pyramidal-tiled-deflate.tif/32768,0,23556,18006/737,/0/default.jpg
                        // So, if we are scaling a TIFF and its metadata says
                        // it's deflate-compressed, use nearest-neighbor, which
                        // is horrible, but better than nothing.
                        if (getSourceFormat().equals(Format.TIF)) {
                            try {
                                Node node = reader.getMetadata(0);
                                StringWriter writer = new StringWriter();
                                Transformer t = TransformerFactory.newInstance().newTransformer();
                                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                                t.setOutputProperty(OutputKeys.INDENT, "no");
                                t.transform(new DOMSource(node),
                                        new StreamResult(writer));
                                if (writer.toString().contains("description=\"ZLib\"")) {
                                    interpolation = Interpolation.
                                            getInstance(Interpolation.INTERP_NEAREST);
                                }
                            } catch (TransformerException e) {
                                logger.error(e.getMessage());
                            }
                        }
                        logger.debug("Scaling using {}",
                                interpolation.getClass().getName());
                        renderedOp = JaiUtil.scaleImage(renderedOp, (Scale) op,
                                interpolation, rf);
                    } else if (op instanceof Transpose) {
                        renderedOp = JaiUtil.
                                transposeImage(renderedOp, (Transpose) op);
                    } else if (op instanceof Rotate) {
                        renderedOp = JaiUtil.
                                rotateImage(renderedOp, (Rotate) op);
                    } else if (op instanceof Filter) {
                        renderedOp = JaiUtil.
                                filterImage(renderedOp, (Filter) op);
                    } else if (op instanceof Watermark) {
                        // Let's cheat and apply the watermark using Java 2D.
                        // There seems to be minimal performance penalty in doing
                        // this, and doing it in JAI is harder.
                        image = renderedOp.getAsBufferedImage();
                        try {
                            image = Java2dUtil.applyWatermark(image,
                                    (Watermark) op);
                        } catch (ConfigurationException e) {
                            logger.error(e.getMessage());
                        }
                    }
                }
                ImageIoImageWriter writer = new ImageIoImageWriter();

                if (image != null) {
                    writer.write(image, ops.getOutputFormat(), outputStream);
                } else {
                    writer.write(renderedOp, ops.getOutputFormat(),
                            outputStream);
                }
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
