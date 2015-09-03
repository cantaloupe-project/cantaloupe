package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public class ImageMagickProcessor implements Processor {

    public void process(Parameters params, OutputStream outputStream)
            throws Exception {
        String filePath = this.getResolvedPathname(params.getIdentifier());

        IMOperation op = new IMOperation();
        op.addImage("-"); // read from stdin

        // region transformation
        Region region = params.getRegion();
        if (!region.isFull()) {
            if (region.isPercent()) {
                op.crop(region.getWidth(), region.getHeight(),
                        Math.round(region.getX()), Math.round(region.getY()),
                        "%".charAt(0)); // TODO: this doesn't work
            } else {
                op.crop(region.getWidth(), region.getHeight(),
                        Math.round(region.getX()), Math.round(region.getY()));
            }
        }

        // size transformation
        Size size = params.getSize();
        if (!size.isFull()) {
            if (size.isFitWidth()) {
                op.resize(size.getWidth());
            } else if (size.isFitHeight()) {
                op.resize(null, size.getHeight());
            } else if (size.getWidth() != null && size.getHeight() != null) {
                op.resize(size.getWidth(), size.getHeight(), "!".charAt(0));
            } else if (size.getScaleToPercent() != null) {
                op.resize(Math.round(size.getScaleToPercent()),
                        Math.round(size.getScaleToPercent()),
                        "%".charAt(0));
            } else if (size.isScaleToFit()) {
                op.resize(size.getWidth(), size.getHeight());
            }
        }

        // rotation transformation
        Rotation rotation = params.getRotation();
        if (rotation.getDegrees() != 0) {
            if (rotation.shouldMirror()) {
                op.flop();
            }
            op.rotate(rotation.getDegrees().doubleValue());
        }

        // quality transformation
        Quality quality = params.getQuality();
        if (quality != Quality.COLOR && quality != Quality.DEFAULT) {
            switch (quality) {
                case GRAY:
                    op.colorspace("Gray");
                    break;
                case BITONAL:
                    op.monochrome();
                    break;
            }
        }

        // format transformation
        op.addImage(params.getFormat().getExtension() + ":-"); // write to stdout

        FileInputStream fis = new FileInputStream(filePath);
        Pipe pipeIn = new Pipe(fis, null);
        Pipe pipeOut = new Pipe(null, outputStream);

        ConvertCmd convert = new ConvertCmd();
        convert.setInputProvider(pipeIn);
        convert.setOutputConsumer(pipeOut);
        convert.run(op);
        fis.close();
    }

    public boolean resourceExists(String identifier) {
        String filePath = this.getResolvedPathname(identifier);
        File file = new File(filePath);
        return file.exists() && !file.isDirectory();
    }

    /**
     * @param identifier Identifier component of an IIIF 2.0 URL
     * @return Full filesystem path of the file corresponding to the given
     * identifier
     */
    private String getResolvedPathname(String identifier) {
        Resolver resolver = ResolverFactory.getResolver();
        return resolver.resolve(identifier);
    }

}
