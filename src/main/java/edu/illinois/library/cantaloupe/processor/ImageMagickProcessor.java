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
import org.apache.commons.configuration.Configuration;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.Pipe;
import org.im4java.process.ProcessStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImageMagickProcessor implements Processor {

    private static Logger logger = LoggerFactory.
            getLogger(ImageMagickProcessor.class);

    private static final HashMap<SourceFormat,Set<OutputFormat>> OUTPUT_FORMATS =
            getAvailableOutputFormats();
    private static final Set<String> FORMAT_EXTENSIONS = new HashSet<String>();
    private static final Set<String> QUALITIES = new HashSet<String>();
    private static final Set<String> SUPPORTS = new HashSet<String>();
    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    static {
        // overrides the PATH; see
        // http://im4java.sourceforge.net/docs/dev-guide.html
        String binaryPath = Application.getConfiguration().
                getString("ImageMagickProcessor.path_to_binaries", "");
        if (binaryPath.length() > 0) {
            ProcessStarter.setGlobalSearchPath(binaryPath);
        }

        for (Set<OutputFormat> set : OUTPUT_FORMATS.values()) {
            for (OutputFormat format : set) {
                FORMAT_EXTENSIONS.add(format.getExtension());
            }
        }

        for (Quality quality : Quality.values()) {
            QUALITIES.add(quality.toString().toLowerCase());
        }

        SUPPORTS.add("baseUriRedirect");
        SUPPORTS.add("canonicalLinkHeader");
        SUPPORTS.add("cors");
        SUPPORTS.add("jsonldMediaType");
        SUPPORTS.add("mirroring");
        SUPPORTS.add("regionByPx");
        SUPPORTS.add("rotationArbitrary");
        SUPPORTS.add("rotationBy90s");
        SUPPORTS.add("sizeAboveFull");
        SUPPORTS.add("sizeByWhListed");
        SUPPORTS.add("sizeByForcedWh");
        SUPPORTS.add("sizeByH");
        SUPPORTS.add("sizeByPct");
        SUPPORTS.add("sizeByW");
        SUPPORTS.add("sizeWh");
    }

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    public static HashMap<SourceFormat, Set<OutputFormat>> getAvailableOutputFormats() {
        if (supportedFormats == null) {
            loadSupportedFormats();
        }
        return supportedFormats;
    }

    public static void loadSupportedFormats() {
        final Set<SourceFormat> sourceFormats = new HashSet<SourceFormat>();
        final Set<OutputFormat> outputFormats = new HashSet<OutputFormat>();

        try {
            // retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats
            Runtime runtime = Runtime.getRuntime();
            String cmdPath = "";
            Configuration config = Application.getConfiguration();
            if (config != null) {
                cmdPath = config.getString("ImageMagickProcessor.path_to_binaries", "");
            }
            String[] commands = {cmdPath + File.separator + "identify",
                    "-list", "format"};
            Process proc = runtime.exec(commands);
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String s;

            while ((s = stdInput.readLine()) != null) {
                s = s.trim();
                if (s.startsWith("JP2")) {
                    sourceFormats.add(SourceFormat.JP2);
                    outputFormats.add(OutputFormat.JP2);
                }
                if (s.startsWith("JPEG")) {
                    sourceFormats.add(SourceFormat.JPG);
                    outputFormats.add(OutputFormat.JPG);
                }
                if (s.startsWith("PNG")) {
                    sourceFormats.add(SourceFormat.PNG);
                    outputFormats.add(OutputFormat.PNG);
                }
                if (s.startsWith("PDF")) {
                    outputFormats.add(OutputFormat.PDF);
                }
                if (s.startsWith("TIFF")) {
                    sourceFormats.add(SourceFormat.TIF);
                    outputFormats.add(OutputFormat.TIF);
                }
                if (s.startsWith("WEBP")) {
                    sourceFormats.add(SourceFormat.WEBP);
                    outputFormats.add(OutputFormat.WEBP);
                }

            }
        } catch (IOException e) {
            logger.error("Failed to execute identify command");
        }

        supportedFormats = new HashMap<SourceFormat,Set<OutputFormat>>();
        for (SourceFormat sourceFormat : sourceFormats) {
            supportedFormats.put(sourceFormat, outputFormats);
        }
    }

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = OUTPUT_FORMATS.get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<OutputFormat>();
        }
        return formats;
    }

    public ImageInfo getImageInfo(File sourceFile,
                                  SourceFormat sourceFormat,
                                  String imageBaseUri) throws Exception {
        Info sourceInfo = new Info(sourceFormat.getPreferredExtension() + ":" +
                sourceFile.getAbsolutePath(), true);
        return doGetImageInfo(sourceInfo, imageBaseUri);
    }

    public ImageInfo getImageInfo(InputStream inputStream,
                                  SourceFormat sourceFormat,
                                  String imageBaseUri) throws Exception {
        Info sourceInfo = new Info(sourceFormat.getPreferredExtension() + ":-",
                inputStream, true);
        ImageInfo info = doGetImageInfo(sourceInfo, imageBaseUri);
        inputStream.close();
        return info;
    }

    private ImageInfo doGetImageInfo(Info sourceInfo, String imageBaseUri)
            throws InfoException {
        ImageInfo imageInfo = new ImageInfo();
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

    public Set<SourceFormat> getSupportedSourceFormats() {
        return OUTPUT_FORMATS.keySet();
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        File file, OutputStream outputStream) throws Exception {
        IMOperation op = new IMOperation();
        op.addImage(file.getAbsolutePath());
        op = assembleOperation(op, params);

        // format transformation
        op.addImage(params.getOutputFormat().getExtension() + ":-"); // write to stdout

        Pipe pipeOut = new Pipe(null, outputStream);

        ConvertCmd convert = new ConvertCmd();
        convert.setOutputConsumer(pipeOut);
        convert.run(op);
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        InputStream inputStream, OutputStream outputStream)
            throws Exception {
        IMOperation op = new IMOperation();
        op.addImage(sourceFormat.getPreferredExtension() + ":-"); // read from stdin
        op = assembleOperation(op, params);

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

    private IMOperation assembleOperation(IMOperation op, Parameters params) {
        // region transformation
        Region region = params.getRegion();
        if (!region.isFull()) {
            if (region.isPercent()) {
                op.crop(Math.round(region.getWidth()), Math.round(region.getHeight()),
                        Math.round(region.getX()), Math.round(region.getY()),
                        "%".charAt(0)); // TODO: this affects only width/height, not x/y
            } else {
                op.crop(Math.round(region.getWidth()), Math.round(region.getHeight()),
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

        return op;
    }

}
