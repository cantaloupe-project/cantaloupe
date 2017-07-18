package edu.illinois.library.cantaloupe.resolver;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Convenience class that provides a {@link StreamSource} for an
 * {@link File}.
 */
public class FileInputStreamStreamSource implements StreamSource {

    private final File file;

    public FileInputStreamStreamSource(File file) {
        this.file = file;
    }

    @Override
    public ImageInputStream newImageInputStream() throws IOException {
        return ImageIO.createImageInputStream(file);
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return new FileInputStream(file);
    }

}