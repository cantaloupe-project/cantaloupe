package edu.illinois.library.cantaloupe.resolver;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
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
    public ImageInputStream newImageInputStream() throws IOException {
        return ImageIO.createImageInputStream(inputStream);
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return inputStream;
    }

}