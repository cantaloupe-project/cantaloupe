package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.io.ImageIoImageReader;
import edu.illinois.library.cantaloupe.processor.io.ImageIoImageWriter;
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

    protected ImageIoImageReader reader;
    protected File sourceFile;
    protected StreamSource streamSource;

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
        if (reader == null) {
            reader = new ImageIoImageReader();
        }
        try {
            reader.setSource(sourceFile);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        reader.setFormat(format);
    }

    @Override
    public void setSourceFormat(Format format)
            throws UnsupportedSourceFormatException{
        this.format = format;
        if (reader == null) {
            reader = new ImageIoImageReader();
        }
        reader.setFormat(format);
        if (getAvailableOutputFormats().size() < 1) {
            throw new UnsupportedSourceFormatException(
                    getClass().getSimpleName() + " does not support the " +
                            format + " source format");
        }
    }

    public void setStreamSource(StreamSource streamSource) {
        disposeReader();
        this.sourceFile = null;
        this.streamSource = streamSource;
        if (reader == null) {
            reader = new ImageIoImageReader();
        }
        try {
            reader.setSource(streamSource);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        reader.setFormat(format);
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
