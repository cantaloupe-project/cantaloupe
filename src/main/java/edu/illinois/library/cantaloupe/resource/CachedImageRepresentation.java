package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Representation that writes an {@link InputStream} to the response.
 */
public class CachedImageRepresentation implements Representation {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CachedImageRepresentation.class);

    private InputStream inputStream;

    public CachedImageRepresentation(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try {
            final Stopwatch watch = new Stopwatch();
            inputStream.transferTo(outputStream);
            LOGGER.debug("Streamed from the cache without resolving in {}",
                    watch);
        } finally {
            inputStream.close();
        }
    }

}
