package edu.illinois.library.cantaloupe.resolver;

import java.io.InputStream;

/**
 * Convenience class that provides a {@link StreamSource} for an
 * {@link InputStream}.
 */
public class InputStreamStreamSource implements StreamSource {

    private final InputStream inputStream;

    public InputStreamStreamSource(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public InputStream newInputStream() {
        return inputStream;
    }

}