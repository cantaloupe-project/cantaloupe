package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFacade;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.DefaultResourceCache;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Processor using the <a href="https://pdfbox.apache.org">Apache PDFBox</a>
 * library to render source PDFs, and Java 2D to perform post-rasterization
 * processing steps.
 */
class PdfBoxProcessor extends AbstractProcessor
        implements FileProcessor, StreamProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfBoxProcessor.class);

    private static final int DEFAULT_DPI = 150;

    private PDDocument doc;
    private Metadata metadata;
    private Path sourceFile;
    private StreamFactory streamFactory;

    static {
        // This "fixes" the rendering of several PDFs -- hopefully it doesn't
        // break others...
        // The docs say that this "may improve the performance of rendering
        // PDFs on some systems especially if there are a lot of images on a
        // page."
        // See: https://github.com/cantaloupe-project/cantaloupe/issues/198
        // Also see: https://pdfbox.apache.org/2.0/getting-started.html
        System.setProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion", "true");
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(doc);
        doc = null;
        metadata = null;
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        return (Format.PDF.equals(getSourceFormat())) ?
                ImageWriterFactory.supportedFormats() :
                Collections.unmodifiableSet(Collections.emptySet());
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public StreamFactory getStreamFactory() {
        return streamFactory;
    }

    @Override
    public void process(OperationList opList,
                        Info imageInfo,
                        OutputStream outputStream) throws FormatException, ProcessorException {
        try {
            super.process(opList, imageInfo, outputStream);

            final ScaleConstraint scaleConstraint = opList.getScaleConstraint();

            final Set<ReaderHint> hints = EnumSet.noneOf(ReaderHint.class);
            ScaleByPercent scale = (ScaleByPercent) opList.getFirst(ScaleByPercent.class);
            if (scale == null) {
                scale = new ScaleByPercent();
            }

            double pct = scale.getPercent();
            ReductionFactor reductionFactor = ReductionFactor.forScale(pct);

            // This processor supports a "page" URI query argument.
            int page = getPageNumber(opList.getOptions());

            BufferedImage image = readImage(
                    page - 1, reductionFactor, scaleConstraint);
            image = Java2DPostProcessor.postProcess(
                    image, hints, opList, imageInfo, reductionFactor);
            ImageWriterFacade.write(image,
                    (Encode) opList.getFirst(Encode.class),
                    outputStream);
        } catch (SourceFormatException e) {
            throw e;
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            close();
        }
    }

    /**
     * @param options Operation list options map.
     * @return Page number from the given options map, or {@code 1} if not
     *         found.
     */
    private int getPageNumber(Map<String,Object> options) {
        int page = 1;
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

    @Override
    public boolean isSeeking() {
        return false;
    }

    private void readDocument() throws IOException {
        if (doc == null) {
            final Stopwatch watch = new Stopwatch();

            if (sourceFile != null) {
                doc = PDDocument.load(sourceFile.toFile());
            } else {
                try (InputStream is = streamFactory.newInputStream()) {
                    doc = PDDocument.load(is);
                } catch (IOException e) {
                    throw new SourceFormatException();
                }
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

            metadata = new Metadata();
            { // Read the document's native metadata.
                PDDocumentInformation info = doc.getDocumentInformation();
                Map<String, String> pdfMetadata = new HashMap<>();
                for (String key : info.getMetadataKeys()) {
                    if (info.getPropertyStringValue(key) != null) {
                        pdfMetadata.put(key, info.getPropertyStringValue(key).toString());
                    }
                }
                metadata.setNativeMetadata(pdfMetadata);
            }
            { // Read the document's XMP metadata.
                PDMetadata pdfMetadata = doc.getDocumentCatalog().getMetadata();
                if (pdfMetadata != null) {
                    try (InputStream is = pdfMetadata.exportXMPMetadata()) {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        is.transferTo(os);

                        metadata.setXMP(os.toByteArray());
                    }
                }
            }

            LOGGER.debug("Loaded document in {}", watch);
        }
    }

    /**
     * @return Rasterized page of the PDF.
     */
    private BufferedImage readImage(int pageIndex,
                                    ReductionFactor rf,
                                    ScaleConstraint scaleConstraint) throws IOException {
        double dpi = new RasterizationHelper().getDPI(
                rf.factor, scaleConstraint);
        return readImage(pageIndex, dpi);
    }

    /**
     * @return Rasterized page of the PDF.
     * @throws IllegalArgumentException if the given page index is out of
     *                                   bounds.
     */
    private BufferedImage readImage(int pageIndex,
                                    double dpi) throws IOException {
        LOGGER.debug("DPI: {}", dpi);

        readDocument();
        PDFRenderer renderer = new PDFRenderer(doc);
        return renderer.renderImageWithDPI(pageIndex, (float) dpi);
    }

    @Override
    public Info readInfo() throws IOException {
        readDocument();

        final Configuration config = Configuration.getInstance();
        final int dpi = config.getInt(Key.PROCESSOR_DPI, DEFAULT_DPI);
        final float scale = dpi / 72f;
        final Info info = Info.builder()
                .withFormat(getSourceFormat())
                .withMetadata(metadata)
                .withNumResolutions(1)
                .build();
        info.getImages().clear();

        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            // PDF doesn't have native dimensions, so figure out the dimensions
            // at the current DPI setting.
            final PDPage page         = doc.getPage(i);
            final PDRectangle cropBox = page.getCropBox();
            final float widthPt       = cropBox.getWidth();
            final float heightPt      = cropBox.getHeight();
            final int rotationAngle   = page.getRotation();

            int widthPx  = Math.round(widthPt * scale);
            int heightPx = Math.round(heightPt * scale);
            if (rotationAngle == 90 || rotationAngle == 270) {
                int tmp  = widthPx;
                //noinspection SuspiciousNameCombination
                widthPx  = heightPx;
                heightPx = tmp;
            }

            Dimension size   = new Dimension(widthPx, heightPx);
            Info.Image image = new Info.Image();
            image.setSize(size);
            image.setTileSize(size);
            info.getImages().add(image);
        }
        return info;
    }

    @Override
    public void setSourceFile(Path sourceFile) {
        this.streamFactory = null;
        this.sourceFile = sourceFile;
    }

    @Override
    public void setStreamFactory(StreamFactory streamFactory) {
        this.sourceFile = null;
        this.streamFactory = streamFactory;
    }

    @Override
    public void validate(OperationList opList, Dimension fullSize)
            throws ValidationException, ProcessorException {
        StreamProcessor.super.validate(opList, fullSize);

        // The "page" argument, if present, was validated in the overridden
        // method, but we also want to make sure the page is actually contained
        // in the PDF.
        final String pageStr = (String) opList.getOptions().get("page");
        if (pageStr != null) {
            final int page = Integer.parseInt(pageStr);
            try {
                readDocument();
                if (page > doc.getNumberOfPages()) {
                    close();
                    throw new ValidationException(
                            "Page number is out-of-bounds.");
                }
            } catch (IOException e) {
                close();
                throw new ProcessorException(e.getMessage(), e);
            }
        }
    }

}
