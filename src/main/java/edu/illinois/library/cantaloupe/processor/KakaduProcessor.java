package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import gov.lanl.adore.djatoka.io.reader.PNMReader;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the kdu_expand tool.
 *
 * @see <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Usage_Examples-v7_7.txt">
 *     Usage Examples for the Demonstration Applications Supplied with Kakadu
 *     V7.7</a>
 */
class KakaduProcessor implements Processor {

    private static Logger logger = LoggerFactory.getLogger(KakaduProcessor.class);

    private static final String KDU_EXPAND = Application.getConfiguration().
            getString("KakaduProcessor.path_to_kdu_expand");
    private static final String STDOUT = "/dev/stdout";
    private static final String STDIN = "/dev/stdin";
    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();

    private static String[] envParams;

    static {
        if (System.getProperty("os.name").startsWith("Mac")) {
            envParams = new String[] {
                    "DYLD_LIBRARY_PATH=" + System.getProperty("DYLD_LIBRARY_PATH")
            };
        } else if (System.getProperty("os.name").startsWith("Linux") ||
                System.getProperty("os.name").startsWith("Solaris")) {
            envParams = new String[] {
                    "LD_LIBRARY_PATH=" + System.getProperty("LD_LIBRARY_PATH")
            };
        }

        SUPPORTED_QUALITIES.add(Quality.BITONAL);
        SUPPORTED_QUALITIES.add(Quality.COLOR);
        SUPPORTED_QUALITIES.add(Quality.DEFAULT);
        SUPPORTED_QUALITIES.add(Quality.GRAY);

        SUPPORTED_FEATURES.add(ProcessorFeature.MIRRORING);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PIXELS);
        SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_ARBITRARY);
        SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_BY_90S);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_ABOVE_FULL);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
    }

    private static String quote(String path) {
        if (path.contains(" ")) {
            path = "\"" + path + "\"";
        }
        return path;
    }

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = new HashSet<>();
        if (sourceFormat == SourceFormat.JP2) {
            formats.add(OutputFormat.JPG);
            formats.add(OutputFormat.TIF);
        }
        return formats; // TODO: any more formats?
    }

    public Dimension getSize(ImageInputStream inputStream,
                             SourceFormat sourceFormat) throws Exception {
        // TODO: write this
        return new Dimension(4096, 4096);
    }

    public Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat) {
        return SUPPORTED_FEATURES;
    }

    public Set<Quality> getSupportedQualities(SourceFormat sourceFormat) {
        return SUPPORTED_QUALITIES;
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        ImageInputStream inputStream, OutputStream outputStream)
            throws Exception {
        final Dimension fullSize = getSize(inputStream, sourceFormat);
        final String command = getKduCommand(params, fullSize, STDIN, STDOUT);
        System.out.println(command);
        final Process process = Runtime.getRuntime().exec(command, envParams,
                new File(KDU_EXPAND).getParentFile());
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final ExecuteStreamHandler streamHandler = new PumpStreamHandler(stdout,
                //stderr, new ImageInputStreamWrapper(inputStream));
                stderr, new FileInputStream(new File("/Volumes/Data/alexd/Sites/iiif-test/orion-hubble-4096.jp2")));
        try {
            streamHandler.setProcessInputStream(process.getOutputStream());
            streamHandler.setProcessOutputStream(process.getInputStream());
            streamHandler.setProcessErrorStream(process.getErrorStream());
        } catch (IOException e) {
            process.getInputStream().close();
            process.getOutputStream().close();
            process.getErrorStream().close();
            process.destroy();
            throw e;
        }

        streamHandler.start();

        try {
            process.waitFor();
            final String error = stderr.toString();
            if (error != null && error.length() > 0) {
                throw new Exception(error);
            }
            final ByteArrayInputStream bais = new ByteArrayInputStream(stdout.toByteArray());
            //BufferedImage image = new PNMReader().open(bais);
            //ImageIO.write(image, params.getOutputFormat().getExtension(),
            //        outputStream);
            streamHandler.stop();
        } catch (InterruptedException|ThreadDeath e) {
            logger.error(e.getMessage(), e);
            process.destroy();
            throw e;
        } finally {
            process.getInputStream().close();
            //process.getOutputStream().close();
            process.getErrorStream().close();
            process.destroy();
        }
    }

    /**
     * Gets a kdu_expand command corresponding to the given parameters.
     *
     * @param params
     * @param input absolute file path of JPEG 2000 image file.
     * @param output absolute file path of PGM output image
     * @return Command string
     */
    private String getKduCommand(Parameters params, Dimension fullSize,
                                 String input, String output) {
        StringBuilder command = new StringBuilder(KDU_EXPAND);
        if (!input.equals(STDIN)) { // TODO: boolean is inverted
            command.append(" -no_seek");
        }
        //command.append(" -quiet ");
        command.append(" -i ");
        command.append(quote(new File(input).getAbsolutePath()));
        //command.append("/Users/alexd/Sites/iiif-test/orion-hubble-4096.jp2");
        command.append(" -o ");
        command.append("/Users/alexd/Desktop/out.tif");
        //command.append(quote(new File(output).getAbsolutePath()));

        Region region = params.getRegion();
        if (!region.isFull()) {
            double x = region.getX() / fullSize.width;
            double y = region.getY() / fullSize.height;
            double width = region.getWidth() / fullSize.width;
            double height = region.getHeight() / fullSize.height;
            command.append(" -region ").append(
                    String.format("\"{%f,%f},{%f,%f}\"", y, x, height, width));
        }
        return command.toString();
    }

}
