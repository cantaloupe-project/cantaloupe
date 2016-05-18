package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Format;
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
 * <p>Processor using the GraphicsMagick <code>gm</code> command-line tool.
 * Tested with version 1.3.21; other versions may or may not work.</p>
 *
 * <p>Does not implement <var>FileProcessor</var> because testing indicates
 * that reading from streams is significantly faster.</p>
 */
class GraphicsMagickProcessor extends Im4JavaProcessor
        implements StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(GraphicsMagickProcessor.class);

    static final String BACKGROUND_COLOR_CONFIG_KEY =
            "GraphicsMagickProcessor.background_color";
    static final String PATH_TO_BINARIES_CONFIG_KEY =
            "GraphicsMagickProcessor.path_to_binaries";

    // Lazy-initialized by getFormats()
    private static HashMap<Format, Set<Format>> supportedFormats;

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
     * based on information reported by <code>gm version</code>.
     */
    private static HashMap<Format, Set<Format>> getFormats() {
        if (supportedFormats == null) {
            final Set<Format> formats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

            // Get the output of the `gm version` command, which contains
            // a list of all optional formats.
            final ProcessBuilder pb = new ProcessBuilder();
            final List<String> command = new ArrayList<>();
            command.add(getPath("gm"));
            command.add("version");
            pb.command(command);
            final String commandString = StringUtils.join(pb.command(), " ");

            try {
                logger.info("Executing {}", commandString);
                final Process process = pb.start();

                try (final InputStream processInputStream = process.getInputStream()) {
                    BufferedReader stdInput = new BufferedReader(
                            new InputStreamReader(processInputStream));
                    String s;
                    boolean read = false;
                    while ((s = stdInput.readLine()) != null) {
                        if (s.contains("Feature Support")) {
                            read = true; // start reading
                        } else if (s.contains("Host type:")) {
                            break; // stop reading
                        }
                        if (read) {
                            s = s.trim();
                            if (s.startsWith("JPEG-2000 ") && s.endsWith(" yes")) {
                                formats.add(Format.JP2);
                                outputFormats.add(Format.JP2);
                            } else if (s.startsWith("JPEG ") && s.endsWith(" yes")) {
                                formats.add(Format.JPG);
                                outputFormats.add(Format.JPG);
                            } else if (s.startsWith("PNG ") && s.endsWith(" yes")) {
                                formats.add(Format.PNG);
                                outputFormats.add(Format.PNG);
                            } else if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                                outputFormats.add(Format.PDF);
                            } else if (s.startsWith("TIFF ") && s.endsWith(" yes")) {
                                formats.add(Format.TIF);
                                outputFormats.add(Format.TIF);
                            } else if (s.startsWith("WebP ") && s.endsWith(" yes")) {
                                formats.add(Format.WEBP);
                                outputFormats.add(Format.WEBP);
                            }
                        }
                    }
                    process.waitFor();

                    // add formats that are not listed in the output of
                    // "gm version" but are definitely available
                    // (http://www.graphicsmagick.org/formats.html)
                    formats.add(Format.BMP);
                    formats.add(Format.GIF);
                    // GIF output is buggy
                    //outputFormats.add(OutputFormat.GIF);
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
    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = getFormats().get(format);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
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
            op.addImage(format.getPreferredExtension() + ":-");
            String bgColor =
                    config.getString(BACKGROUND_COLOR_CONFIG_KEY, "black");
            assembleOperation(op, ops, imageInfo.getSize(), bgColor);

            op.addImage(ops.getOutputFormat().getPreferredExtension() + ":-"); // write to stdout

            // true = use GraphicsMagick instead of ImageMagick
            ConvertCmd convert = new ConvertCmd(true);

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
