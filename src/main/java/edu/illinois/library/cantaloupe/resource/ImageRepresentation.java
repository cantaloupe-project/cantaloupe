package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Restlet representation for images.
 */
public class ImageRepresentation extends OutputRepresentation {

    private static Logger logger = LoggerFactory.
            getLogger(ImageRepresentation.class);

    File file;
    Dimension fullSize;
    InputStream inputStream;
    Operations params;
    SourceFormat sourceFormat;

    /**
     * Constructor for images from InputStreams.
     *
     * @param mediaType
     * @param sourceFormat
     * @param fullSize
     * @param params
     * @param inputStream
     */
    public ImageRepresentation(MediaType mediaType,
                               SourceFormat sourceFormat,
                               Dimension fullSize,
                               Operations params,
                               InputStream inputStream) {
        super(mediaType);
        this.inputStream = inputStream;
        this.params = params;
        this.sourceFormat = sourceFormat;
        this.fullSize = fullSize;
    }

    /**
     * Constructor for images from Files.
     *
     * @param mediaType
     * @param sourceFormat
     * @param params
     * @param file
     */
    public ImageRepresentation(MediaType mediaType,
                               SourceFormat sourceFormat,
                               Operations params, File file) {
        super(mediaType);
        this.file = file;
        this.params = params;
        this.sourceFormat = sourceFormat;
    }

    /**
     * Writes the source image to the given output stream.
     *
     * @param outputStream Response body output stream supplied by Restlet
     * @throws IOException
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        Cache cache = CacheFactory.getInstance();
        try {
            if (cache != null) {
                OutputStream cacheOutputStream = null;
                try (InputStream cacheInputStream =
                             cache.getImageInputStream(this.params)) {
                    if (cacheInputStream != null) {
                        IOUtils.copy(cacheInputStream, outputStream);
                    } else {
                        cacheOutputStream = cache.
                                getImageOutputStream(this.params);
                        TeeOutputStream tos = new TeeOutputStream(
                                outputStream, cacheOutputStream);
                        doCacheAwareWrite(tos, cache);
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                } finally {
                    if (cacheOutputStream != null) {
                        cacheOutputStream.close();
                    }
                }
            } else {
                doWrite(outputStream);
            }
        } finally {
            /*
            TODO: doesn't work with Java2dProcessor.process() - try in release()?
            try {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } */
        }
    }

    /**
     * Variant of doWrite() that cleans up incomplete cached images when
     * the connection has been interrupted.
     *
     * @param outputStream
     * @param cache
     * @throws IOException
     */
    private void doCacheAwareWrite(TeeOutputStream outputStream,
                                   Cache cache) throws IOException {
        try {
            doWrite(outputStream);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            cache.flush(this.params);
        }
    }

    private void doWrite(OutputStream outputStream) throws IOException {
        try {
            final long msec = System.currentTimeMillis();
            // if the parameters request an unmodified source image, it can
            // be streamed right through
            if (this.params.isRequestingUnmodifiedSource()) {
                if (this.file != null) {
                    IOUtils.copy(new FileInputStream(this.file),
                            outputStream);
                } else {
                    IOUtils.copy(this.inputStream, outputStream);
                }
                logger.debug("Streamed with no processing in {} msec",
                        System.currentTimeMillis() - msec);
            } else {
                Processor proc = ProcessorFactory.
                        getProcessor(this.sourceFormat);
                if (this.file != null) {
                    FileProcessor fproc = (FileProcessor) proc;
                    Dimension size = fproc.getSize(this.file,
                            this.sourceFormat);
                    fproc.process(this.params, this.sourceFormat, size,
                            this.file, outputStream);
                } else {
                    StreamProcessor sproc = (StreamProcessor) proc;
                    sproc.process(this.params, this.sourceFormat,
                            this.fullSize, this.inputStream, outputStream);
                }
                logger.debug("{} processed in {} msec",
                        proc.getClass().getSimpleName(),
                        System.currentTimeMillis() - msec);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}