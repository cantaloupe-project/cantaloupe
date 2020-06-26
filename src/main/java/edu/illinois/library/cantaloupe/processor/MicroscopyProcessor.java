package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.*;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.processor.codec.microscopy.MicroscopyImageReader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Processor using the Bio-Formats library.</p>
 */
public class MicroscopyProcessor extends AbstractProcessor implements FileProcessor {

    private Path sourceFile;
    private final ImageReader reader = new MicroscopyImageReader();

    @Override
    public void close() {
        reader.dispose();
    }

    @Override
    public void process(final OperationList ops,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws FormatException, ProcessorException {

        super.process(ops, imageInfo, outputStream);

        final ReductionFactor rf = new ReductionFactor();
        final Set<ReaderHint> hints = EnumSet.noneOf(ReaderHint.class);

        try {
            Crop crop = (Crop) ops.getFirst(Crop.class);
            Scale scale = (Scale) ops.getFirst(Scale.class);
            ScaleConstraint sc = ops.getScaleConstraint();

            BufferedImage image = reader.read(crop, scale, sc, rf, hints);
            image = Java2DPostProcessor.postProcess(image, hints, ops, imageInfo, rf);

            Encode encode = (Encode) ops.getFirst(Encode.class);
            ImageWriter writer = new ImageWriterFactory().newImageWriter(encode);
            writer.write(image, outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        return ImageWriterFactory.supportedFormats();
    }

    @Override
    public Info readInfo() throws IOException {
        final Info info = new Info();
        info.getImages().clear();
        info.setSourceFormat(getSourceFormat());
        info.setNumResolutions(reader.getNumResolutions());

        for (int i = 0; i < reader.getNumImages(); i++) {
            Info.Image image = new Info.Image();
            image.setSize(reader.getSize(i));
            image.setTileSize(reader.getTileSize(i));
            info.getImages().add(image);
        }

        return info;
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public void setSourceFile(Path sourceFile) {
        this.sourceFile = sourceFile;
        try {
            reader.setSource(sourceFile);
        } catch (IOException e) {
            e.printStackTrace();
            this.sourceFile = null;
        }
    }
}