package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @see <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Usage_Examples-v7_7.txt">
 *     Usage Examples for the Demonstration Applications Supplied with Kakadu
 *     V7.7</a>
 */
public class KakaduProcessor implements Processor {

    private static final Set<String> QUALITIES = new HashSet<String>();
    private static final Set<String> SUPPORTS = new HashSet<String>();

    static {
        for (Quality quality : Quality.values()) {
            QUALITIES.add(quality.toString().toLowerCase());
        }

        SUPPORTS.add("baseUriRedirect");
        SUPPORTS.add("canonicalLinkHeader");
        SUPPORTS.add("cors");
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
     * based on information reported by <code>gm version</code>.
     */
    public static HashMap<SourceFormat, Set<OutputFormat>> getAvailableOutputFormats() {
        // TODO: write this
    }

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<SourceFormat> formats = new HashSet<SourceFormat>();
        if (sourceFormat == SourceFormat.JP2) {
            formats.add(SourceFormat.JPG);
        }
        // TODO: write this
    }

    public ImageInfo getImageInfo(File sourceFile, SourceFormat sourceFormat,
                           String imageBaseUri) throws Exception {
        // TODO: write this
    }

    public ImageInfo getImageInfo(InputStream inputStream,
                                  SourceFormat sourceFormat,
                                  String imageBaseUri) throws Exception {
        // TODO: write this
    }

    public Set<SourceFormat> getSupportedSourceFormats() {
        Set<SourceFormat> formats = new HashSet<SourceFormat>();
        formats.add(SourceFormat.JP2);
        return formats;
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        File file, OutputStream outputStream) throws Exception {
        File fifo = createAndGetFifo(); // kdu_expand will write to this

    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        InputStream inputStream, OutputStream outputStream)
            throws Exception {
        // noop
    }

    /**
     * Creates and returns a new FIFO with a unique name.
     *
     * @throws IOException
     */
    private File createAndGetFifo() throws IOException, ConfigurationException {
        // TODO: this is inefficient
        File temp = File.createTempFile("cantaloupe_kdu_fifo", ".tmp");
        String pathname = temp.getAbsolutePath();
        temp.delete();
        Runtime.getRuntime().exec(getMkfifoPath() + " " + pathname);
        return new File(pathname);
    }

    private String getMkfifoPath() throws ConfigurationException {
        Configuration config = Application.getConfiguration();
        final String key = "path_to_mkfifo";
        String mkFifoPath = config.getString(key);
        if (mkFifoPath == null) {
            throw new ConfigurationException("Missing configuration key: " + key);
        }
        return mkFifoPath;
    }

}
