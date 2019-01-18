package edu.illinois.library.cantaloupe.source;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Convenience class that provides a {@link StreamFactory} for a {@link Path}.
 */
public class PathStreamFactory implements StreamFactory {

    private final Path path;

    public PathStreamFactory(Path path) {
        this.path = path;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public ImageInputStream newSeekableStream() throws IOException {
        return new FileImageInputStream(path.toFile());
    }

    @Override
    public boolean isSeekingDirect() {
        return true;
    }

}