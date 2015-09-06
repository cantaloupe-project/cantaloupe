package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.process.Pipe;
import org.im4java.process.ProcessStarter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphicsMagickProcessor implements Processor {

    private static final Set<OutputFormat> OUTPUT_FORMATS = new HashSet<OutputFormat>();
    private static final Set<String> FORMAT_EXTENSIONS = new HashSet<String>();
    private static final Set<String> QUALITIES = new HashSet<String>();
    private static final Set<String> SUPPORTS = new HashSet<String>();

    static {
        System.setProperty("im4java.useGM", "true");
        // overrides the PATH; see
        // http://im4java.sourceforge.net/docs/dev-guide.html
        String binaryPath = Application.getConfiguration().
                getString("GraphicsMagickProcessor.path_to_binaries");
        if (binaryPath.length() > 0) {
            ProcessStarter.setGlobalSearchPath(binaryPath);
        }

        for (OutputFormat outputFormat : OutputFormat.values()) {
            OUTPUT_FORMATS.add(outputFormat);
            FORMAT_EXTENSIONS.add(outputFormat.getExtension());
        }

        for (Quality quality : Quality.values()) {
            QUALITIES.add(quality.toString().toLowerCase());
        }

        // TODO: is this list accurate?
        SUPPORTS.add("baseUriRedirect");
        SUPPORTS.add("mirroring");
        SUPPORTS.add("regionByPx");
        SUPPORTS.add("rotationArbitrary");
        SUPPORTS.add("rotationBy90s");
        SUPPORTS.add("sizeByWhListed");
        SUPPORTS.add("sizeByForcedWh");
        SUPPORTS.add("sizeByH");
        SUPPORTS.add("sizeByPct");
        SUPPORTS.add("sizeByW");
        SUPPORTS.add("sizeWh");
    }

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        return OUTPUT_FORMATS;
    }

    public ImageInfo getImageInfo(InputStream inputStream,
                                  SourceFormat sourceFormat,
                                  String imageBaseUri) throws Exception {
        ImageInfo imageInfo = new ImageInfo();

        Info sourceInfo = new Info(sourceFormat.getExtension() + ":-",
                inputStream, true);
        imageInfo.setId(imageBaseUri);
        imageInfo.setHeight(sourceInfo.getImageHeight());
        imageInfo.setWidth(sourceInfo.getImageWidth());

        imageInfo.getProfile().add("http://iiif.io/api/image/2/level2.json");
        Map<String,Set<String>> profile = new HashMap<String, Set<String>>();
        imageInfo.getProfile().add(profile);

        profile.put("formats", FORMAT_EXTENSIONS);
        profile.put("qualities", QUALITIES);
        profile.put("supports", SUPPORTS);

        return imageInfo;
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        InputStream inputStream, OutputStream outputStream)
            throws Exception {
        IMOperation op = new IMOperation();
        op.addImage(sourceFormat.getExtension() + ":-"); // read from stdin

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
        if (size.getScaleMode() != Size.ScaleMode.FULL) {
            if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                op.resize(size.getWidth());
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                op.resize(null, size.getHeight());
            } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FILL) {
                op.resize(size.getWidth(), size.getHeight(), "!".charAt(0));
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                op.resize(size.getWidth(), size.getHeight());
            } else if (size.getPercent() != null) {
                op.resize(Math.round(size.getPercent()),
                        Math.round(size.getPercent()),
                        "%".charAt(0));
            }
        }

        // rotation transformation
        Rotation rotation = params.getRotation();
        if (rotation.shouldMirror()) {
            op.flop();
        }
        if (rotation.getDegrees() != 0) {
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
        op.addImage(params.getOutputFormat().getExtension() + ":-"); // write to stdout

        Pipe pipeIn = new Pipe(inputStream, null);
        Pipe pipeOut = new Pipe(null, outputStream);

        ConvertCmd convert = new ConvertCmd();
        convert.setInputProvider(pipeIn);
        convert.setOutputConsumer(pipeOut);
        convert.run(op);
        inputStream.close();
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }

}
