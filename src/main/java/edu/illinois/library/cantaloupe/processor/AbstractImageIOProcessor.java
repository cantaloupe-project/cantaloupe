package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract class that can be extended by processors that rely on the ImageIO
 * framework to read images.
 */
abstract class AbstractImageIOProcessor extends AbstractProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractImageIOProcessor.class);

    private static final HashMap<Format,Set<Format>> FORMATS =
            availableOutputFormats();

    protected File sourceFile;
    protected StreamSource streamSource;

    /**
     * Access via {@link #getReader()}.
     */
    private ImageReader reader;

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by ImageIO.
     */
    private static HashMap<Format, Set<Format>> availableOutputFormats() {
        final HashMap<Format,Set<Format>> map = new HashMap<>();
        for (Format format : ImageReader.supportedFormats()) {
            map.put(format, ImageWriter.supportedFormats());
        }
        return map;
    }

    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = FORMATS.get(format);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    public Info readImageInfo() throws ProcessorException {
        try {
            final Info info = new Info();
            info.setSourceFormat(getSourceFormat());

            final ImageReader reader = getReader();
            final Orientation orientation = getEffectiveOrientation();
            for (int i = 0, numResolutions = reader.getNumResolutions();
                 i < numResolutions; i++) {
                Info.Image image = new Info.Image();
                image.setSize(reader.getSize(i));
                image.setTileSize(reader.getTileSize(i));
                image.setOrientation(orientation);
                info.getImages().add(image);
            }
            return info;
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * @return Effective orientation of the image, respecting the setting of
     *         {@link Processor#RESPECT_ORIENTATION_CONFIG_KEY}. Never null.
     */
    protected Orientation getEffectiveOrientation() throws IOException {
        Orientation orientation = null;
        if (ConfigurationFactory.getInstance().
                getBoolean(Processor.RESPECT_ORIENTATION_CONFIG_KEY, false)) {
            orientation = reader.getMetadata(0).getOrientation();
        }
        if (orientation == null) {
            orientation = Orientation.ROTATE_0;
        }
        return orientation;
    }

    /**
     * ({@link #setSourceFile(File)} or {@link #setStreamSource(StreamSource)})
     * and {@link #setSourceFormat(Format)} must be invoked first.
     */
    protected ImageReader getReader() {
        if (reader == null) {
            try {
                if (streamSource != null) {
                    reader = new ImageReader(streamSource, getSourceFormat());
                } else {
                    reader = new ImageReader(sourceFile, getSourceFormat());
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return reader;
    }

    public File getSourceFile() {
        return this.sourceFile;
    }

    public StreamSource getStreamSource() {
        return this.streamSource;
    }

    public void setSourceFile(File sourceFile) {
        disposeReader();
        this.streamSource = null;
        this.sourceFile = sourceFile;
    }

    public void setStreamSource(StreamSource streamSource) {
        disposeReader();
        this.sourceFile = null;
        this.streamSource = streamSource;
    }

    private void disposeReader() {
        if (reader != null) {
            reader.dispose();
        }
        reader = null;
    }

}
