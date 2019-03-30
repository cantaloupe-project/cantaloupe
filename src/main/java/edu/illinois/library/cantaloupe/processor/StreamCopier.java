package edu.illinois.library.cantaloupe.processor;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Writes an {@link InputStream} to an {@link OutputStream}.
 */
class StreamCopier implements Runnable {

    private static Logger logger = LoggerFactory.
            getLogger(StreamCopier.class);

    private final InputStream inputStream;
    private final OutputStream outputStream;

    StreamCopier(InputStream in, OutputStream out) {
        inputStream = in;
        outputStream = out;
    }

    public void run() {
        try {
            inputStream.transferTo(outputStream);
        } catch (IOException e) {
            if (!e.getMessage().startsWith("Broken pipe")) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
