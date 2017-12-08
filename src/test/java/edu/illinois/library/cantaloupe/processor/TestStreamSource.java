package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class TestStreamSource implements StreamSource {

    private Path path;

    TestStreamSource(Path path) {
        this.path = path;
    }

    @Override
    public ImageInputStream newImageInputStream() throws IOException {
        return new FileImageInputStream(path.toFile());
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return Files.newInputStream(path);
    }

}
