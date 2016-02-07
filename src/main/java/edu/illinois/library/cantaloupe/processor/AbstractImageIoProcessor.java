package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract class that can be extended by processors that rely on the ImageIO
 * framework to read images.
 */
abstract class AbstractImageIoProcessor extends AbstractProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractImageIoProcessor.class);

    private static final HashMap<SourceFormat,Set<OutputFormat>> FORMATS =
            availableOutputFormats();

    protected ImageIoImageReader reader;
    protected File sourceFile;
    protected StreamSource streamSource;

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by ImageIO.
     */
    private static HashMap<SourceFormat, Set<OutputFormat>>
    availableOutputFormats() {
        final HashMap<SourceFormat,Set<OutputFormat>> map = new HashMap<>();
        for (SourceFormat sourceFormat : ImageIoImageReader.supportedFormats()) {
            map.put(sourceFormat, ImageIoImageWriter.supportedFormats());
        }
        return map;
    }

    public Set<OutputFormat> getAvailableOutputFormats() {
        Set<OutputFormat> formats = FORMATS.get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    public Dimension getSize() throws ProcessorException {
        try {
            return reader.getSize();
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

    public List<Dimension> getTileSizes() throws ProcessorException {
        try {
            final List<Dimension> sizes = new ArrayList<>();

            for (int i = 0, numResolutions = reader.getNumResolutions();
                 i < numResolutions; i++) {
                sizes.add(reader.getTileSize(i));
            }
            return sizes;
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
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
        reader.setSourceFormat(sourceFormat);
    }

    @Override
    public void setSourceFormat(SourceFormat sourceFormat)
            throws UnsupportedSourceFormatException{
        this.sourceFormat = sourceFormat;
        if (reader == null) {
            reader = new ImageIoImageReader();
        }
        reader.setSourceFormat(sourceFormat);
        if (getAvailableOutputFormats().size() < 1) {
            throw new UnsupportedSourceFormatException(
                    getClass().getSimpleName() + " does not support the " +
                            sourceFormat + " source format");
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
        reader.setSourceFormat(sourceFormat);
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
