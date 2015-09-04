package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This processor works only with local filesystem resolvers because the Kakadu
 * binaries are unable to read from any other source.
 */
public class KakaduProcessor implements Processor {

    private static final List<OutputFormat> OUTPUT_FORMATS = new ArrayList<OutputFormat>();
    private static final List<String> FORMAT_EXTENSIONS = new ArrayList<String>();
    private static final List<String> QUALITIES = new ArrayList<String>();
    private static final List<String> SUPPORTS = new ArrayList<String>();

    static {
        for (OutputFormat outputFormat : OutputFormat.values()) {
            if (outputFormat != OutputFormat.WEBP &&
                    outputFormat != OutputFormat.PDF) {
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

    /**
     * This implementation simply wraps ImageMagickProcessor's implementation,
     * because the `kdu_jp2info` command can't read from stdin or a FIFO, which
     * are the only practical ways to pass data to it from Java.
     *
     * @param inputStream An InputStream from which to read the image
     * @param imageBaseUri Base URI of the image
     * @return
     */
    public ImageInfo getImageInfo(InputStream inputStream,
                                  String imageBaseUri) {
        Processor proc = new ImageMagickProcessor();
        return proc.getImageInfo(inputStream, imageBaseUri);
    }

    public List<OutputFormat> getSupportedOutputFormats() {
        return OUTPUT_FORMATS;
    }

    public void process(Parameters params, InputStream inputStream,
                        OutputStream outputStream) throws Exception {
        String fifoPathname = createFifo();

    }

    public String toString() {
        return this.getClass().getSimpleName();
    }

    private String getMkfifoPath() throws ConfigurationException {
        Configuration config = Application.getConfiguration();
        String mkFifoPath = config.getString("mkfifo_path");
        if (mkFifoPath == null) {
            throw new ConfigurationException("Missing configuration key: mkfifo_path");
        }
        return mkFifoPath;
    }

    /**
     * Creates a new FIFO with a unique name.
     *
     * @return Pathname of the FIFO
     * @throws IOException
     * @throws ConfigurationException
     */
    private String createFifo() throws IOException, ConfigurationException {
        // TODO: this is inefficient
        File temp = File.createTempFile("cantaloupe_kdu_fifo", ".tmp");
        String pathname = temp.getAbsolutePath();
        temp.delete();
        Runtime.getRuntime().exec(getMkfifoPath() + " " + pathname);
        return pathname;
    }

}
