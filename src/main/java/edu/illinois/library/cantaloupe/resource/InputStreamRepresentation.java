package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Writes an {@link InputStream} directly to the response.
 */
public class InputStreamRepresentation implements Representation {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InputStreamRepresentation.class);

    private InputStream inputStream;

    public InputStreamRepresentation(InputStream inputStream) {
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
