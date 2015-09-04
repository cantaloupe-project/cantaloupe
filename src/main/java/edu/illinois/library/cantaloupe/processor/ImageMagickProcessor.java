package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import org.apache.commons.configuration.ConfigurationException;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.Pipe;
import org.im4java.process.ProcessStarter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageMagickProcessor implements Processor {

    private static final List<OutputFormat> OUTPUT_FORMATS = new ArrayList<OutputFormat>();
    private static final List<String> FORMAT_EXTENSIONS = new ArrayList<String>();
    private static final List<String> QUALITIES = new ArrayList<String>();
    private static final List<String> SUPPORTS = new ArrayList<String>();

    static {
        // overrides the PATH; see
        // http://im4java.sourceforge.net/docs/dev-guide.html
        String binaryPath = getConfigurationString("ImageMagickProcessor.path_to_binaries");
        if (binaryPath.length() > 0) {
            ProcessStarter.setGlobalSearchPath(binaryPath);
        }

        for (OutputFormat outputFormat : OutputFormat.values()) {
            if (outputFormat != OutputFormat.WEBP) { // we don't support webp
                OUTPUT_FORMATS.add(outputFormat);
                FORMAT_EXTENSIONS.add(outputFormat.getExtension());
            }
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

    private static String getConfigurationString(String key) {
        String value = "";
        try {
            value = Application.getConfiguration().getString(key);
        } catch (ConfigurationException e) {
        }
        return value;
    }

    public ImageInfo getImageInfo(InputStream inputStream,
                                  String imageBaseUri) throws Exception {
        ImageInfo imageInfo = new ImageInfo();

        Info sourceInfo = new Info("-", inputStream, true);
        imageInfo.setId(imageBaseUri);
        imageInfo.setHeight(sourceInfo.getImageHeight());
        imageInfo.setWidth(sourceInfo.getImageWidth());

        Map<String,List<String>> profile = new HashMap<String, List<String>>();
        imageInfo.getProfile().add(profile);

        profile.put("formats", FORMAT_EXTENSIONS);
        profile.put("qualities", QUALITIES);
        profile.put("supports", SUPPORTS);

        return imageInfo;
    }

    public List<OutputFormat> getSupportedOutputFormats() {
        return OUTPUT_FORMATS;
    }

    public void process(Parameters params, InputStream inputStream,
                        OutputStream outputStream) throws Exception {
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
        if (size.getScaleMode() != Size.ScaleMode.FULL) {
            if (size.getScaleMode() == Size.ScaleMode.FILL_WIDTH) {
                op.resize(size.getWidth());
            } else if (size.getScaleMode() == Size.ScaleMode.FILL_HEIGHT) {
                op.resize(null, size.getHeight());
            } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FIT_INSIDE) {
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
