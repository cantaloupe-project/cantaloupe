package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
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

    public static final String CONTENT_DISPOSITION_CONFIG_KEY =
            "http.content_disposition";
    public static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";

    private File file;
    private Dimension fullSize;
    private InputStream inputStream;
    private OperationList ops;
    private SourceFormat sourceFormat;

    /**
     * Constructor for images from InputStreams.
     *
     * @param mediaType
     * @param sourceFormat
     * @param fullSize
     * @param ops
     * @param inputStream
     */
    public ImageRepresentation(MediaType mediaType,
                               SourceFormat sourceFormat,
                               Dimension fullSize,
                               OperationList ops,
                               InputStream inputStream) {
        super(mediaType);
        this.inputStream = inputStream;
        this.ops = ops;
        this.sourceFormat = sourceFormat;
        this.fullSize = fullSize;
        initialize(ops.getIdentifier(), ops.getOutputFormat());

    }

    /**
     * Constructor for images from Files.
     *
     * @param mediaType
     * @param sourceFormat
     * @param ops
     * @param file
     */
    public ImageRepresentation(MediaType mediaType,
                               SourceFormat sourceFormat,
                               OperationList ops, File file) {
        super(mediaType);
        this.file = file;
        this.ops = ops;
        this.sourceFormat = sourceFormat;
        initialize(ops.getIdentifier(), ops.getOutputFormat());
    }

    private void initialize(Identifier identifier, OutputFormat format) {
        Disposition disposition = new Disposition();
        switch (Application.getConfiguration().
                getString(CONTENT_DISPOSITION_CONFIG_KEY, "none")) {
            case "inline":
                disposition.setType(Disposition.TYPE_INLINE);
                this.setDisposition(disposition);
                break;
            case "attachment":
                disposition.setType(Disposition.TYPE_ATTACHMENT);
                disposition.setFilename(
                        identifier.toString().replaceAll(FILENAME_CHARACTERS, "_") +
                                "." + format.getExtension());
                this.setDisposition(disposition);
                break;
        }
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
                             cache.getImageInputStream(this.ops)) {
                    if (cacheInputStream != null) {
                        // a cached image is available; write it to the
                        // response output stream.
                        IOUtils.copy(cacheInputStream, outputStream);
                    } else {
                        // create a TeeOutputStream to write to both the
                        // response output stream and the cache simultaneously.
                        cacheOutputStream = cache.
                                getImageOutputStream(this.ops);
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
     * the connection has been broken.
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
            cache.flush(this.ops);
        }
    }

    private void doWrite(OutputStream outputStream) throws IOException {
        try {
            final long msec = System.currentTimeMillis();
            // if the parameters request an unmodified source image, it can
            // be streamed right through
            if (this.ops.isNoOp()) {
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
                    fproc.process(this.ops, this.sourceFormat, size,
                            this.file, outputStream);
                } else {
                    StreamProcessor sproc = (StreamProcessor) proc;
                    sproc.process(this.ops, this.sourceFormat,
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