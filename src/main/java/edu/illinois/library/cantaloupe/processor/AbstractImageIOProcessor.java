package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class that can be extended by processors that rely on the ImageIO
 * framework to read images.
 */
abstract class AbstractImageIOProcessor extends AbstractProcessor {

    private static final Map<Format,Set<Format>> FORMATS =
            availableOutputFormats();

    protected Path sourceFile;
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
        for (Format format : ImageReaderFactory.supportedFormats()) {
            map.put(format, ImageWriterFactory.supportedFormats());
        }
        return map;
    }

    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = FORMATS.get(format);
        if (formats == null) {
            formats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return formats;
    }

    public Info readImageInfo() throws IOException {
        final Info info = new Info();
        info.setSourceFormat(getSourceFormat());

        final ImageReader reader = getReader();
        final Orientation orientation = getEffectiveOrientation();
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
     * ({@link #setSourceFile} or {@link #setStreamSource}) and
     * {@link #setSourceFormat(Format)} must be invoked first.
     */
    protected ImageReader getReader() throws IOException {
        if (reader == null) {
            ImageReaderFactory rf = new ImageReaderFactory();

            if (streamSource != null) {
                reader = rf.newImageReader(streamSource, getSourceFormat());
            } else {
                reader = rf.newImageReader(sourceFile, getSourceFormat());
            }
        }
        return reader;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public StreamSource getStreamSource() {
        return streamSource;
    }

    public void setSourceFile(Path sourceFile) {
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
