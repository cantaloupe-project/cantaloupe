package edu.illinois.library.cantaloupe.resolver;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Convenience class that provides a {@link StreamSource} for a {@link Path}.
 */
public class PathStreamSource implements StreamSource {

    private final Path path;

    public PathStreamSource(Path path) {
        this.path = path;
    }

    @Override
    public ImageInputStream newImageInputStream() throws IOException {
        return ImageIO.createImageInputStream(path.toFile());
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return Files.newInputStream(path);
    }

}