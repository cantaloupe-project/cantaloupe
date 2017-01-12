package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the <a href="https://pdfbox.apache.org">Apache PDFBox</a>
 * library to render source PDFs, and Java 2D to perform post-rasterization
 * processing steps.
 */
class PdfBoxProcessor extends AbstractJava2dProcessor
        implements FileProcessor, StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(PdfBoxProcessor.class);

    static final String DOWNSCALE_FILTER_CONFIG_KEY =
            "PdfBoxProcessor.downscale_filter";
    static final String DPI_CONFIG_KEY = "PdfBoxProcessor.dpi";
    static final String SHARPEN_CONFIG_KEY = "PdfBoxProcessor.sharpen";
    static final String UPSCALE_FILTER_CONFIG_KEY =
            "PdfBoxProcessor.upscale_filter";

    private BufferedImage fullImage;
    private File sourceFile;
    private StreamSource streamSource;

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats = new HashSet<>();
        if (format == Format.PDF) {
            outputFormats.addAll(ImageWriter.supportedFormats());
        }
        return outputFormats;
    }

    Scale.Filter getDownscaleFilter() {
        final String upscaleFilterStr = ConfigurationFactory.getInstance().
                getString(DOWNSCALE_FILTER_CONFIG_KEY);
        try {
            return Scale.Filter.valueOf(upscaleFilterStr.toUpperCase());
        } catch (Exception e) {
            logger.warn("Invalid value for {}", DOWNSCALE_FILTER_CONFIG_KEY);
        }
        return null;
    }

    Scale.Filter getUpscaleFilter() {
        final String upscaleFilterStr = ConfigurationFactory.getInstance().
                getString(UPSCALE_FILTER_CONFIG_KEY);
        try {
            return Scale.Filter.valueOf(upscaleFilterStr.toUpperCase());
        } catch (Exception e) {
            logger.warn("Invalid value for {}", UPSCALE_FILTER_CONFIG_KEY);
        }
        return null;
    }

    @Override
    public ImageInfo readImageInfo() throws ProcessorException {
        try {
            if (fullImage == null) {
                // This is a very inefficient method of getting the size.
                // Unfortunately, it's the only choice PDFBox offers.
                // At least cache it in an ivar to avoid having to load it
                // multiple times.
                fullImage = readImage();
            }
            return new ImageInfo(fullImage.getWidth(), fullImage.getHeight(),
                    fullImage.getWidth(), fullImage.getHeight(),
                    getSourceFormat());
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public File getSourceFile() {
        return sourceFile;
    }

    @Override
    public StreamSource getStreamSource() {
        return streamSource;
    }

    @Override
    public void process(OperationList opList,
                        ImageInfo imageInfo,
                        OutputStream outputStream) throws ProcessorException {
        try {
            // If the op list contains a scale operation, see if we can use
            // a reduction factor in order to use a scale-appropriate
            // rasterization DPI.
            Scale scale = new Scale();
            for (Operation op : opList) {
                if (op instanceof Scale) {
                    scale = (Scale) op;
                    break;
                }
            }
            ReductionFactor reductionFactor = new ReductionFactor();
            Float pct = scale.getResultingScale(imageInfo.getSize());
            if (pct != null) {
                reductionFactor = ReductionFactor.forScale(pct);
            }

            // This processor supports a "page" URI query option.
            Integer page = 1;
            String pageStr = (String) opList.getOptions().get("page");
            if (pageStr != null) {
                try {
                    page = Integer.parseInt(pageStr);
                } catch (NumberFormatException e) {
                    logger.info("Page number from URI query string is not " +
                            "an integer; using page 1.");
                }
            }
            page = Math.max(page, 1);

            final BufferedImage image = readImage(page - 1, reductionFactor.factor);
            final Configuration config = ConfigurationFactory.getInstance();
            postProcess(image, null, opList, imageInfo,
                    reductionFactor, Orientation.ROTATE_0, false,
                    getUpscaleFilter(), getDownscaleFilter(),
                    config.getFloat(SHARPEN_CONFIG_KEY, 0f),
                    outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private BufferedImage readImage() throws IOException {
        return readImage(0, 0);
    }

    /**
     * @param pageIndex
     * @param reductionFactor Scale factor by which to reduce the image (or
     *                        enlarge it if negative).
     * @return Rasterized first page of the PDF.
     * @throws IOException
     */
    private BufferedImage readImage(int pageIndex,
                                    int reductionFactor) throws IOException {
        float dpi = getDpi(reductionFactor);
        logger.debug("readImage(): using a DPI of {} ({}x reduction factor)",
                Math.round(dpi), reductionFactor);

        InputStream inputStream = null;
        PDDocument doc = null;
        try {
            if (sourceFile != null) {
                doc = PDDocument.load(sourceFile);
            } else {
                inputStream = streamSource.newInputStream();
                doc = PDDocument.load(inputStream);
            }

            // If the given page index is out of bounds, the renderer will
            // throw an exception. In that case, render the first page.
            PDFRenderer renderer = new PDFRenderer(doc);
            try {
                return renderer.renderImageWithDPI(pageIndex, dpi);
            } catch (IndexOutOfBoundsException e) {
                return renderer.renderImageWithDPI(0, dpi);
            }
        } finally {
            try {
                if (doc != null) {
                    doc.close();
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
    }

    private float getDpi(int reductionFactor) {
        float dpi = ConfigurationFactory.getInstance().
                getFloat(DPI_CONFIG_KEY, 150);
        // Decrease the DPI if the reduction factor is positive.
        for (int i = 0; i < reductionFactor; i++) {
            dpi /= 2f;
        }
        // Increase the DPI if the reduction factor is negative.
        for (int i = 0; i > reductionFactor; i--) {
            dpi *= 2f;
        }
        return dpi;
    }

    @Override
    public void setSourceFile(File sourceFile) {
        this.streamSource = null;
        this.sourceFile = sourceFile;
    }

    @Override
    public void setStreamSource(StreamSource streamSource) {
        this.sourceFile = null;
        this.streamSource = streamSource;
    }

}
