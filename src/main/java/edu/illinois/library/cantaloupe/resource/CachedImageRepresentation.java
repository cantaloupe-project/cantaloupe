package edu.illinois.library.cantaloupe.resource;

import org.apache.commons.io.IOUtils;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Restlet representation for cached images.
 */
public class CachedImageRepresentation extends OutputRepresentation {

    private static Logger logger = LoggerFactory.
            getLogger(CachedImageRepresentation.class);

    private InputStream inputStream;

    /**
     * Constructor for images from the cache.
     *
     * @param mediaType
     * @param disposition,
     * @param inputStream
     */
    public CachedImageRepresentation(MediaType mediaType,
                                     Disposition disposition,
                                     InputStream inputStream) {
        super(mediaType);
        this.inputStream = inputStream;
        setDisposition(disposition);
    }

    /**
     * Writes the source image to the given output stream.
     *
     * @param outputStream Response body stream supplied by Restlet
     * @throws IOException
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        final long msec = System.currentTimeMillis();
        IOUtils.copy(inputStream, outputStream);
        logger.info("Streamed from the cache without resolving in {} msec",
                System.currentTimeMillis() - msec);
    }

}