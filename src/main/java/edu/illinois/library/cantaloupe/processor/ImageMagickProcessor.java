package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import org.apache.commons.lang3.StringUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processor using the ImageMagick `convert` and `identify` command-line tools.
 */
class ImageMagickProcessor extends Im4JavaProcessor implements StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(ImageMagickProcessor.class);

    static final String BACKGROUND_COLOR_CONFIG_KEY =
            "ImageMagickProcessor.background_color";
    static final String PATH_TO_BINARIES_CONFIG_KEY =
            "ImageMagickProcessor.path_to_binaries";

    // Lazy-initialized by getFormats()
    protected static HashMap<Format, Set<Format>> supportedFormats;

    @Override
    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = getFormats().get(format);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    /**
     * @param binaryName Name of an executable
     * @return
     */
    private static String getPath(String binaryName) {
        String path = Configuration.getInstance().
                getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null && path.length() > 0) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    private static HashMap<Format, Set<Format>> getFormats() {
        if (supportedFormats == null) {
            final Set<Format> formats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

            // Retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats.
            final ProcessBuilder pb = new ProcessBuilder();
            final List<String> command = new ArrayList<>();
            command.add(getPath("identify"));
            command.add("-list");
            command.add("format");
            pb.command(command);
            final String commandString = StringUtils.join(pb.command(), " ");

            try {
                logger.info("Executing {}", commandString);
                final Process process = pb.start();

                try (final InputStream processInputStream = process.getInputStream()) {
                    BufferedReader stdInput = new BufferedReader(
                            new InputStreamReader(processInputStream));
                    String s;
                    while ((s = stdInput.readLine()) != null) {
                        s = s.trim();
                        if (s.startsWith("JP2")) {
                            formats.add(Format.JP2);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.JP2);
                            }
                        }
                        if (s.startsWith("JPEG")) {
                            formats.add(Format.JPG);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.JPG);
                            }
                        }
                        if (s.startsWith("PNG")) {
                            formats.add(Format.PNG);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.PNG);
                            }
                        }
                        if (s.startsWith("PDF") && s.contains(" rw")) {
                            outputFormats.add(Format.PDF);
                        }
                        if (s.startsWith("TIFF")) {
                            formats.add(Format.TIF);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.TIF);
                            }
                        }
                        if (s.startsWith("WEBP")) {
                            formats.add(Format.WEBP);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.WEBP);
                            }
                        }
                    }
                    process.waitFor();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }

            supportedFormats = new HashMap<>();
            for (Format format : formats) {
                supportedFormats.put(format, outputFormats);
            }
        }
        return supportedFormats;
    }

    @Override
    public void process(final OperationList ops,
                        final ImageInfo imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        final Configuration config = Configuration.getInstance();
        try {
            IMOperation op = new IMOperation();
            op.addImage(format.getPreferredExtension() + ":-"); // read from stdin
            String bgColor =
                    config.getString(BACKGROUND_COLOR_CONFIG_KEY, "black");
            assembleOperation(op, ops, imageInfo.getSize(), bgColor);

            op.addImage(ops.getOutputFormat().getPreferredExtension() + ":-"); // write to stdout

            ConvertCmd convert = new ConvertCmd();

            String binaryPath =
                    config.getString(PATH_TO_BINARIES_CONFIG_KEY, "");
            if (binaryPath.length() > 0) {
                convert.setSearchPath(binaryPath);
            }

            try (InputStream inputStream = streamSource.newInputStream()) {
                convert.setInputProvider(new Pipe(inputStream, null));
                convert.setOutputConsumer(new Pipe(null, outputStream));
                convert.run(op);
            }
        } catch (InterruptedException | IM4JavaException | IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
