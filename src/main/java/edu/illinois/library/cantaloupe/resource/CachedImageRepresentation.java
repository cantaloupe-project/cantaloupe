package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.util.IOUtils;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Restlet representation for cached images.
 */
public class CachedImageRepresentation extends AbstractImageRepresentation {

    private static Logger logger = LoggerFactory.
            getLogger(CachedImageRepresentation.class);

    private ReadableByteChannel readableChannel;

    /**
     * Constructor for images from the cache.
     *
     * @param mediaType
     * @param ops
     * @param readableChannel
     */
    public CachedImageRepresentation(MediaType mediaType,
                                     OperationList ops,
                                     ReadableByteChannel readableChannel) {
        super(mediaType, ops.getIdentifier(), ops.getOutputFormat());
        this.readableChannel = readableChannel;
    }

    /**
     * Writes the source image to the given output stream.
     *
     * @param writableChannel Response body channel supplied by Restlet
     * @throws IOException
     */
    @Override
    public void write(WritableByteChannel writableChannel) throws IOException {
        final long msec = System.currentTimeMillis();
        IOUtils.copy(readableChannel, writableChannel);
        logger.debug("Streamed from the cache without resolving in {} msec",
                System.currentTimeMillis() - msec);
    }

}