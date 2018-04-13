package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.DefaultResourceCache;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
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
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Processor using the <a href="https://pdfbox.apache.org">Apache PDFBox</a>
 * library to render source PDFs, and Java 2D to perform post-rasterization
 * processing steps.
 */
class PdfBoxProcessor extends AbstractJava2DProcessor
        implements FileProcessor, StreamProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfBoxProcessor.class);

    private static final int FALLBACK_DPI = 150;

    private PDDocument doc;
    private InputStream docInputStream;
    private Path sourceFile;
    private StreamSource streamSource;

    @Override
    public void close() {
        IOUtils.closeQuietly(docInputStream);
        docInputStream = null;
        IOUtils.closeQuietly(doc);
        doc = null;
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        return (Format.PDF.equals(format)) ?
                ImageWriterFactory.supportedFormats() :
                Collections.unmodifiableSet(Collections.emptySet());
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public StreamSource getStreamSource() {
        return streamSource;
    }

    @Override
    public void process(OperationList opList,
                        Info imageInfo,
                        OutputStream outputStream) throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        final Set<ReaderHint> hints =
                EnumSet.noneOf(ReaderHint.class);
        Scale scale = (Scale) opList.getFirst(Scale.class);
        if (scale == null) {
            scale = new Scale();
        }
        // If the op list contains a scale operation that is not
        // NON_ASPECT_FILL, we can use a scale-appropriate rasterization DPI
        // and omit the scale step.
        if (!Scale.Mode.NON_ASPECT_FILL.equals(scale.getMode())) {
            hints.add(ReaderHint.IGNORE_SCALE);
        }

        ReductionFactor reductionFactor = new ReductionFactor();
        Float pct = scale.getResultingScale(imageInfo.getSize());
        if (pct != null) {
            reductionFactor = ReductionFactor.forScale(pct);
        }

        // This processor supports a "page" URI query argument.
        int page = getPageNumber(opList.getOptions());

        try {
            BufferedImage image =
                    readImage(page - 1, scale, imageInfo.getSize());

            postProcess(image, hints, opList, imageInfo, reductionFactor,
                    outputStream);
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            close();
        }
    }

    /**
     * @param options Operation list options map.
     * @return Page number from the given options map, or {@literal 1} if not
     *         found.
     */
    private int getPageNumber(Map<String,Object> options) {
        Integer page = 1;
        String pageStr = (String) options.get("page");
        if (pageStr != null) {
            try {
                page = Integer.parseInt(pageStr);
            } catch (NumberFormatException e) {
                LOGGER.info("Page number from URI query string is not " +
                        "an integer; using page 1.");
            }
        }
        return Math.max(page, 1);
    }

    private void readDocument() throws IOException {
        if (doc == null) {
            final Stopwatch watch = new Stopwatch();

            if (sourceFile != null) {
                doc = PDDocument.load(sourceFile.toFile());
            } else {
                docInputStream = streamSource.newInputStream();
                doc = PDDocument.load(docInputStream);
            }

            // Disable the document's cache of PDImageXObjects
            // See: https://pdfbox.apache.org/2.0/faq.html#outofmemoryerror
            // This cache has never proven to be a problem, but it's not needed.
            doc.setResourceCache(new DefaultResourceCache() {
                @Override
                public void put(COSObject indirect, PDXObject xobject) {
                    // no-op
                }
            });

            LOGGER.debug("Loaded document in {}", watch);
        }
    }

    /**
     * @return Rasterized page of the PDF.
     */
    private BufferedImage readImage(int pageIndex,
                                    Scale scale,
                                    Dimension fullSize) throws IOException {
        float dpi = new RasterizationHelper().getDPI(scale, fullSize);
        return readImage(pageIndex, dpi);
    }

    /**
     * @return Rasterized page of the PDF.
     * @throws IndexOutOfBoundsException if the given page index is out of
     *                                   bounds.
     */
    private BufferedImage readImage(int pageIndex,
                                    float dpi) throws IOException {
        LOGGER.debug("DPI: {}", dpi);

        readDocument();
        PDFRenderer renderer = new PDFRenderer(doc);
        return renderer.renderImageWithDPI(pageIndex, dpi);
    }

    @Override
    public Info readImageInfo() throws IOException {
        readDocument();

        final Configuration config = Configuration.getInstance();
        final int dpi = config.getInt(Key.PROCESSOR_DPI, FALLBACK_DPI);
        final float scale = dpi / 72f;
        final Info info = Info.builder()
                .withFormat(getSourceFormat())
                .withNumResolutions(1)
                .build();
        info.getImages().clear();

        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            // PDF doesn't have native dimensions, so figure out the dimensions
            // at the current DPI setting.
            final PDPage page = doc.getPage(i);
            final PDRectangle cropBox = page.getCropBox();
            final float widthPt = cropBox.getWidth();
            final float heightPt = cropBox.getHeight();
            final int rotationAngle = page.getRotation();

            int widthPx = Math.round(widthPt * scale);
            int heightPx = Math.round(heightPt * scale);
            if (rotationAngle == 90 || rotationAngle == 270) {
                int tmp = widthPx;
                widthPx = heightPx;
                heightPx = tmp;
            }

            Dimension size = new Dimension(widthPx, heightPx);
            Info.Image image = new Info.Image();
            image.setSize(size);
            image.setTileSize(size);
            info.getImages().add(image);
        }
        return info;
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
    public void validate(OperationList opList,
                         Dimension fullSize) throws ProcessorException {
        StreamProcessor.super.validate(opList, fullSize);

        // Check the format of the "page" argument, if present.
        final String pageStr = (String) opList.getOptions().get("page");
        if (pageStr != null) {
            try {
                final int page = Integer.parseInt(pageStr);
                if (page > 0) {
                    // Check that the page is actually contained in the PDF.
                    try {
                        readDocument();
                        if (page > doc.getNumberOfPages()) {
                            close();
                            throw new IndexOutOfBoundsException(
                                    "Page number is out-of-bounds.");
                        }
                    } catch (IOException e) {
                        close();
                        throw new ProcessorException(e.getMessage(), e);
                    }
                } else {
                    throw new IndexOutOfBoundsException(
                            "Page number is out-of-bounds.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid page number.");
            }
        }
    }

}
