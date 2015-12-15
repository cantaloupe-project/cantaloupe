package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.OperationList;
import org.apache.commons.io.IOUtils;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Restlet representation for cached images.
 */
public class CachedImageRepresentation extends AbstractImageRepresentation {

    private static Logger logger = LoggerFactory.
            getLogger(CachedImageRepresentation.class);

    private InputStream inputStream;

    /**
     * Constructor for images from the cache.
     *
     * @param mediaType
     * @param ops
     * @param cacheInputStream
     */
    public CachedImageRepresentation(MediaType mediaType,
                                     OperationList ops,
                                     InputStream cacheInputStream) {
        super(mediaType, ops.getIdentifier(), ops.getOutputFormat());
        this.inputStream = cacheInputStream;
    }

    /**
     * Writes the source image to the given output stream.
     *
     * @param outputStream Response body output stream supplied by Restlet
     * @throws IOException
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        final long msec = System.currentTimeMillis();
        IOUtils.copy(this.inputStream, outputStream);
        logger.debug("Streamed from the cache without resolving in {} msec",
                System.currentTimeMillis() - msec);
    }

}