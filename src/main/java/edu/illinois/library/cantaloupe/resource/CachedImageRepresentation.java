package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.OutputFormat;
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
     * @param ops
     * @param cacheInputStream
     */
    public CachedImageRepresentation(MediaType mediaType,
                                     Operations ops,
                                     InputStream cacheInputStream) {
        super(mediaType);
        this.inputStream = cacheInputStream;
        initialize(ops.getIdentifier(), ops.getOutputFormat());
    }

    // TODO: duplicated in ImageRepresentation
    private void initialize(Identifier identifier, OutputFormat format) {
        Disposition disposition = new Disposition();
        switch (Application.getConfiguration().
                getString(ImageRepresentation.CONTENT_DISPOSITION_CONFIG_KEY, "none")) {
            case "inline":
                disposition.setType(Disposition.TYPE_INLINE);
                this.setDisposition(disposition);
                break;
            case "attachment":
                disposition.setType(Disposition.TYPE_ATTACHMENT);
                disposition.setFilename(
                        identifier.toString().replaceAll(
                                ImageRepresentation.FILENAME_CHARACTERS, "_") +
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
        final long msec = System.currentTimeMillis();
        IOUtils.copy(this.inputStream, outputStream);
        logger.debug("Streamed from the cache without resolving in {} msec",
                System.currentTimeMillis() - msec);
    }

}