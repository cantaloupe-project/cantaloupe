package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
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
    private static Map<Format, Set<Format>> availableOutputFormats() {
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
    public Info readInfo() throws IOException {
        final Info info = new Info();
        info.getImages().clear();
        info.setSourceFormat(getSourceFormat());

        final ImageReader reader = getReader();
        info.setNumResolutions(reader.getNumResolutions());

        for (int i = 0, numImages = reader.getNumImages(); i < numImages; i++) {
            Info.Image image = new Info.Image();
            image.setSize(reader.getSize(i));
            image.setTileSize(reader.getTileSize(i));
            // JP2 tile dimensions are inverted, so swap them
            if ((image.width > image.height && image.tileWidth < image.tileHeight) ||
                    (image.width < image.height && image.tileWidth > image.tileHeight)) {
                int tmp = image.tileWidth;
                //noinspection SuspiciousNameCombination
                image.tileWidth = image.tileHeight;
                image.tileHeight = tmp;
            }
            info.getImages().add(image);
        }

        try {
            final Metadata metadata = reader.getMetadata(0);
            info.setMetadata(metadata);
        } catch (IOException e) {
            // Some Image I/O readers can be picky with some images (for
            // example, JPEGImageReader with YCCK JPEGs). But an Info instance
            // without metadata is still useful.
            LOGGER.debug("readInfo(): {}", e.getMessage());
        }

        LOGGER.trace("readInfo(): {}", info.toJSON());
        return info;
    }

    /**
     * ({@link #setSourceFile} or {@link #setStreamFactory}) and
     * {@link #setSourceFormat(Format)} must be invoked first.
     */
    protected ImageReader getReader() throws IOException {
        if (reader == null) {
            ImageReaderFactory rf = new ImageReaderFactory();
            if (streamFactory != null) {
                reader = rf.newImageReader(getSourceFormat(), streamFactory);
            } else {
                reader = rf.newImageReader(getSourceFormat(), sourceFile);
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

    public boolean isSeeking() {
        ImageReaderFactory rf = new ImageReaderFactory();
        ImageReader reader = rf.newImageReader(getSourceFormat());
        try {
            return reader.canSeek();
        } finally {
            reader.dispose();
        }
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
