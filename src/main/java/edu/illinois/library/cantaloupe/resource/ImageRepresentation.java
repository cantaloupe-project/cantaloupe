package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
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

    private Dimension fullSize;
    private Processor processor;
    private OperationList ops;

    /**
     * @param mediaType
     * @param fullSize
     * @param ops
     * @param disposition
     * @param processor
     */
    public ImageRepresentation(final MediaType mediaType,
                               final Dimension fullSize,
                               final Processor processor,
                               final OperationList ops,
                               final Disposition disposition) {
        super(mediaType);
        this.fullSize = fullSize;
        this.processor = processor;
        this.ops = ops;
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
            // be streamed right through.
            if (ops.isNoOp(processor.getSourceFormat())) {
                if (processor instanceof FileProcessor) {
                    final File sourceFile = ((FileProcessor) processor).getSourceFile();
                    final InputStream inputStream = new FileInputStream(sourceFile);
                    IOUtils.copy(inputStream, outputStream);
                } else {
                    final StreamSource streamSource = ((StreamProcessor) processor).getStreamSource();
                    final InputStream inputStream = streamSource.newInputStream();
                    IOUtils.copy(inputStream, outputStream);
                }
                logger.info("Streamed with no processing in {} msec: {}",
                        System.currentTimeMillis() - msec, ops);
            } else {
                processor.process(this.ops, this.fullSize, outputStream);

                logger.info("{} processed in {} msec: {}",
                        processor.getClass().getSimpleName(),
                        System.currentTimeMillis() - msec, ops);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}