package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class that can be extended by processors that read images using
 * ImageIO.
 */
abstract class AbstractImageIOProcessor extends AbstractProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractImageIOProcessor.class);

    private static final Map<Format,Set<Format>> FORMATS =
            availableOutputFormats();

    protected Path sourceFile;
    protected StreamFactory streamFactory;

    /**
     * Access via {@link #getReader()}.
     */
    private ImageReader reader;

    /**
     * @return Map of available output formats for all known source formats.
     */
    private static HashMap<Format, Set<Format>> availableOutputFormats() {
        final HashMap<Format,Set<Format>> map = new HashMap<>();
        for (Format format : ImageReaderFactory.supportedFormats()) {
            map.put(format, ImageWriterFactory.supportedFormats());
        }
        return map;
    }

    public void close() {
        if (reader != null) {
            reader.dispose();
        }
        reader = null;
    }

    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = FORMATS.get(getSourceFormat());
        if (formats == null) {
            formats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return formats;
    }

    /**
     * N.B.: Subclasses will need to override if they expect to {@link
     * Info#setNumResolutions(int) set the number of resolutions} to a value
     * other than {@literal 1}.
     */
    public Info readImageInfo() throws IOException {
        final Info info = new Info();
        info.getImages().clear();
        info.setSourceFormat(getSourceFormat());

        final ImageReader reader = getReader();
        final Orientation orientation = getEffectiveOrientation();
        info.setNumResolutions(reader.getNumResolutions());

        for (int i = 0, numImages = reader.getNumImages(); i < numImages; i++) {
            Info.Image image = new Info.Image();
            image.setOrientation(orientation);
            image.setSize(reader.getSize(i));
            image.setTileSize(reader.getTileSize(i));
            // JP2 tile dimensions are inverted, so swap them
            if ((image.width > image.height && image.tileWidth < image.tileHeight) ||
                    (image.width < image.height && image.tileWidth > image.tileHeight)) {
                int tmp = image.tileWidth;
                image.tileWidth = image.tileHeight;
                image.tileHeight = tmp;
            }
            info.getImages().add(image);
        }
        LOGGER.trace("readImageInfo(): {}", info.toJSON());
        return info;
    }

    /**
     * @return Effective orientation of the image, respecting the setting of
     *         {@link Key#PROCESSOR_RESPECT_ORIENTATION}. Never null.
     */
    Orientation getEffectiveOrientation() throws IOException {
        Orientation orientation = null;
        if (Configuration.getInstance().
                getBoolean(Key.PROCESSOR_RESPECT_ORIENTATION, false)) {
            orientation = getReader().getMetadata(0).getOrientation();
        }
        if (orientation == null) {
            orientation = Orientation.ROTATE_0;
        }
        return orientation;
    }

    /**
     * ({@link #setSourceFile} or {@link #setStreamFactory}) and
     * {@link #setSourceFormat(Format)} must be invoked first.
     */
    protected ImageReader getReader() throws IOException {
        if (reader == null) {
            ImageReaderFactory rf = new ImageReaderFactory();
            try {
                if (streamFactory != null) {
                    reader = rf.newImageReader(streamFactory, getSourceFormat());
                } else {
                    reader = rf.newImageReader(sourceFile, getSourceFormat());
                }
            } catch (IllegalArgumentException ignore) {
                // This will be thrown if the format provided to
                // newImageReader() is unsupported, which will be the case in
                // e.g. PdfBoxProcessor, whose source format is PDF.
                // PdfBoxProcessor, AbstractJava2dProcessor, and
                // AbstractImageIOProcessor really need to be decoupled.
                // TODO: address the above
            }
        }
        return reader;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public StreamFactory getStreamFactory() {
        return streamFactory;
    }

    public void setSourceFile(Path sourceFile) {
        close();
        this.streamFactory = null;
        this.sourceFile = sourceFile;
    }

    public void setStreamFactory(StreamFactory streamFactory) {
        close();
        this.sourceFile = null;
        this.streamFactory = streamFactory;
    }

}
