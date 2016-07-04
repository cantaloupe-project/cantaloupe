package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.imageio.ImageIoImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageIoImageWriter;
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
abstract class AbstractImageIoProcessor extends AbstractProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractImageIoProcessor.class);

    private static final HashMap<Format,Set<Format>> FORMATS =
            availableOutputFormats();

    protected File sourceFile;
    protected StreamSource streamSource;

    /**
     * Access via {@link #getReader()}.
     */
    private ImageIoImageReader reader;

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by ImageIO.
     */
    private static HashMap<Format, Set<Format>> availableOutputFormats() {
        final HashMap<Format,Set<Format>> map = new HashMap<>();
        for (Format format : ImageIoImageReader.supportedFormats()) {
            map.put(format, ImageIoImageWriter.supportedFormats());
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

    public ImageInfo getImageInfo() throws ProcessorException {
        try {
            final ImageInfo info = new ImageInfo();
            info.setSourceFormat(getSourceFormat());

            final ImageIoImageReader reader = getReader();
            for (int i = 0, numResolutions = reader.getNumResolutions();
                 i < numResolutions; i++) {
                ImageInfo.Image image = new ImageInfo.Image();
                image.setSize(reader.getSize(i));
                image.setTileSize(reader.getTileSize(i));
                info.getImages().add(image);
            }
            return info;
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * {@link #setSourceFile(File)} and {@link #setSourceFormat(Format)} must
     * be invoked first.
     */
    protected ImageIoImageReader getReader() {
        if (reader == null) {
            try {
                if (streamSource != null) {
                    reader = new ImageIoImageReader(streamSource, format);
                } else {
                    reader = new ImageIoImageReader(sourceFile, format);
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
            try {
                reader.dispose();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        reader = null;
    }

}
