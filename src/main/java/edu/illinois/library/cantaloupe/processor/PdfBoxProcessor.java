package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * Processor using the <a href="https://pdfbox.apache.org">Apache PDFBox</a>
 * library to render source PDFs, and Java 2D to perform post-rasterization
 * processing steps.
 */
class PdfBoxProcessor extends AbstractJava2DProcessor
        implements FileProcessor, StreamProcessor {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(PdfBoxProcessor.class);

    private PDDocument doc;
    private InputStream docInputStream;
    private Dimension imageSize;
    private Path sourceFile;
    private StreamSource streamSource;

    private void closeResources() {
        IOUtils.closeQuietly(docInputStream);
        docInputStream = null;
        IOUtils.closeQuietly(doc);
        doc = null;
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats;
        if (Format.PDF.equals(format)) {
            outputFormats = ImageWriter.supportedFormats();
        } else {
            outputFormats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return outputFormats;
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public StreamSource getStreamSource() {
        return streamSource;
    }

    private void loadDocument() throws IOException {
        if (doc == null) {
            if (sourceFile != null) {
                doc = PDDocument.load(sourceFile.toFile());
            } else {
                docInputStream = streamSource.newInputStream();
                doc = PDDocument.load(docInputStream);
            }
        }
    }

    @Override
    public void process(OperationList opList,
                        Info imageInfo,
                        OutputStream outputStream) throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        try {
            // If the op list contains a scale operation, see if we can use
            // a reduction factor in order to use a scale-appropriate
            // rasterization DPI.
            Scale scale = (Scale) opList.getFirst(Scale.class);
            if (scale == null) {
                scale = new Scale();
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
                    LOGGER.info("Page number from URI query string is not " +
                            "an integer; using page 1.");
                }
            }
            page = Math.max(page, 1);

            final BufferedImage image = readImage(page - 1, reductionFactor.factor);
            postProcess(image, null, opList, imageInfo, reductionFactor,
                    outputStream);
        } catch (IOException | IndexOutOfBoundsException e) {
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
     */
    private BufferedImage readImage(int pageIndex,
                                    int reductionFactor) throws IOException {
        float dpi = new RasterizationHelper().getDPI(reductionFactor);
        LOGGER.debug("readImage(): using a DPI of {} ({}x reduction factor)",
                Math.round(dpi), reductionFactor);
        try {
            loadDocument();
            // If the given page index is out of bounds, the renderer will
            // throw an IndexOutOfBoundsException.
            PDFRenderer renderer = new PDFRenderer(doc);
            return renderer.renderImageWithDPI(pageIndex, dpi);
        } finally {
            closeResources();
        }
    }

    @Override
    public Info readImageInfo() throws IOException {
        if (imageSize == null) {
            // This is a very inefficient method of getting the size.
            // Unfortunately, it's the only choice PDFBox offers.
            BufferedImage image = readImage();
            imageSize = new Dimension(image.getWidth(), image.getHeight());
        }
        return new Info(imageSize.width, imageSize.height,
                imageSize.width, imageSize.height, getSourceFormat());
    }

    @Override
    public void setSourceFile(Path sourceFile) {
        this.streamSource = null;
        this.sourceFile = sourceFile;
    }

    @Override
    public void setStreamSource(StreamSource streamSource) {
        this.sourceFile = null;
        this.streamSource = streamSource;
    }

    @Override
    public void validate(OperationList opList, Dimension fullSize)
            throws ValidationException, ProcessorException {
        StreamProcessor.super.validate(opList, fullSize);

        // Check the format of the "page" option, if present.
        final String pageStr = (String) opList.getOptions().get("page");
        if (pageStr != null) {
            try {
                final int page = Integer.parseInt(pageStr);
                if (page > 0) {
                    // Check that the page is actually contained in the PDF.
                    try {
                        loadDocument();
                        if (page > doc.getNumberOfPages()) {
                            throw new ValidationException(
                                    "Page number is out-of-bounds.");
                        }
                    } catch (IOException e) {
                        closeResources();
                        throw new ProcessorException(e.getMessage(), e);
                    }
                } else {
                    throw new ValidationException(
                            "Page number is out-of-bounds.");
                }
            } catch (NumberFormatException e) {
                throw new ValidationException("Invalid page number.");
            }
        }
    }

}
