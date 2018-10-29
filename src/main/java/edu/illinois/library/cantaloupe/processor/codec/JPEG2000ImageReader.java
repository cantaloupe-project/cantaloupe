package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

final class JPEG2000ImageReader implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEG2000ImageReader.class);

    private JPEG2000MetadataReader wrappedReader;
    private ImageInputStream inputStream;

    JPEG2000ImageReader() {
        wrappedReader = new JPEG2000MetadataReader();
    }

    @Override
    public void dispose() {
        if (inputStream != null) {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public Compression getCompression(int imageIndex) {
        return Compression.JPEG2000;
    }

    @Override
    public Metadata getMetadata(int imageIndex) {
        return new Metadata() {
            @Override
            public IIOMetadataNode getAsTree() {
                return null;
            }

            @Override
            public Object getEXIF() {
                return null;
            }

            @Override
            public Object getIPTC() {
                return null;
            }

            @Override
            public Orientation getOrientation() {
                return null;
            }

            @Override
            public String getXMP() {
                try {
                    return wrappedReader.getXMP();
                } catch (IOException e) {
                    LOGGER.warn("getXMP(): {}", e.getMessage());
                }
                return null;
            }
        };
    }

    @Override
    public int getNumImages() {
        return 1;
    }

    @Override
    public int getNumResolutions() throws IOException {
        return wrappedReader.getNumDecompositionLevels() + 1;
    }

    @Override
    public Dimension getSize(int imageIndex) throws IOException {
        return new Dimension(wrappedReader.getWidth(),
                wrappedReader.getHeight());
    }

    @Override
    public Dimension getTileSize(int imageIndex) throws IOException {
        return new Dimension(wrappedReader.getTileWidth(),
                wrappedReader.getTileHeight());
    }

    /**
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public BufferedImage read() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public BufferedImage read(OperationList ops,
                              Orientation orientation,
                              ReductionFactor reductionFactor,
                              Set<ReaderHint> hints) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public RenderedImage readRendered(OperationList ops,
                                      Orientation orientation,
                                      ReductionFactor reductionFactor,
                                      Set<ReaderHint> hints) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public BufferedImageSequence readSequence() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSource(Path imageFile) throws IOException {
        setSource(ImageIO.createImageInputStream(imageFile.toFile()));
    }

    @Override
    public void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
        wrappedReader.setSource(inputStream);
    }

    @Override
    public void setSource(StreamFactory streamFactory) throws IOException {
        setSource(streamFactory.newImageInputStream());
    }

}
