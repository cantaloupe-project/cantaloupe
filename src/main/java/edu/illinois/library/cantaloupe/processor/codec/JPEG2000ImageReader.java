package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

final class JPEG2000ImageReader implements ImageReader {

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
        return new NullMetadata();
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
