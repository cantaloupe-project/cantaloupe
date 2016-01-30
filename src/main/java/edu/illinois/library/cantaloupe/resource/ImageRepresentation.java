package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.restlet.data.Disposition;
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

    public static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";

    private File file;
    private Dimension fullSize;
    private StreamSource streamSource;
    private OperationList ops;
    private SourceFormat sourceFormat;

    /**
     * Constructor for images from InputStreams.
     *
     * @param mediaType
     * @param sourceFormat
     * @param fullSize
     * @param ops
     * @param disposition
     * @param streamSource
     */
    public ImageRepresentation(final MediaType mediaType,
                               final SourceFormat sourceFormat,
                               final Dimension fullSize,
                               final OperationList ops,
                               final Disposition disposition,
                               final StreamSource streamSource) {
        super(mediaType);
        this.streamSource = streamSource;
        this.ops = ops;
        this.sourceFormat = sourceFormat;
        this.fullSize = fullSize;
        this.setDisposition(disposition);
    }

    /**
     * Constructor for images from Files.
     *
     * @param mediaType
     * @param sourceFormat
     * @param fullSize
     * @param ops
     * @param disposition
     * @param file
     */
    public ImageRepresentation(MediaType mediaType,
                               SourceFormat sourceFormat,
                               Dimension fullSize,
                               OperationList ops,
                               Disposition disposition,
                               File file) {
        super(mediaType);
        this.file = file;
        this.fullSize = fullSize;
        this.ops = ops;
        this.sourceFormat = sourceFormat;
        this.setDisposition(disposition);
    }

    /**
     * Writes the source image to the given output stream.
     *
     * @param outputStream Response body output stream supplied by Restlet
     * @throws IOException
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        final Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            OutputStream cacheOutputStream = null;
            try (InputStream inputStream =
                         cache.getImageInputStream(this.ops)) {
                if (inputStream != null) {
                    // a cached image is available; write it to the
                    // response output stream.
                    IOUtils.copy(inputStream, outputStream);
                } else {
                    // create a TeeOutputStream to write to both the
                    // response output stream and the cache simultaneously.
                    cacheOutputStream = cache.getImageOutputStream(this.ops);
                    OutputStream teeStream = new TeeOutputStream(
                            outputStream, cacheOutputStream);
                    doCacheAwareWrite(teeStream, cache);
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
    }

    /**
     * Variant of doWrite() that cleans up incomplete cached images when
     * the connection has been broken.
     *
     * @param outputStream
     * @param cache
     * @throws IOException
     */
    private void doCacheAwareWrite(OutputStream outputStream,
                                   Cache cache) throws CacheException {
        try {
            doWrite(outputStream);
        } catch (IOException e) {
            logger.info(e.getMessage());
            cache.purge(this.ops);
        }
    }

    private void doWrite(OutputStream outputStream) throws IOException {
        try {
            final long msec = System.currentTimeMillis();
            // If the operations are effectively a no-op, the source image can
            // be streamed directly.
            if (this.ops.isNoOp(this.sourceFormat)) {
                if (this.file != null) {
                    IOUtils.copy(new FileInputStream(this.file), outputStream);
                } else {
                    IOUtils.copy(streamSource.newInputStream(), outputStream);
                }
                logger.info("Streamed with no processing in {} msec: {}",
                        System.currentTimeMillis() - msec, ops);
            } else {
                Processor proc = ProcessorFactory.
                        getProcessor(this.sourceFormat);
                if (this.file != null) {
                    FileProcessor fproc = (FileProcessor) proc;
                    fproc.process(this.ops, this.sourceFormat, this.fullSize,
                            this.file, outputStream);
                } else {
                    StreamProcessor sproc = (StreamProcessor) proc;
                    sproc.process(this.ops, this.sourceFormat,
                            this.fullSize, streamSource, outputStream);
                }
                logger.info("{} processed in {} msec: {}",
                        proc.getClass().getSimpleName(),
                        System.currentTimeMillis() - msec, ops);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}