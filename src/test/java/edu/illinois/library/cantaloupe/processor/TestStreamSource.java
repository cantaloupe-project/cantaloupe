package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class TestStreamSource implements StreamSource {

    private File file;

    public TestStreamSource(File file) {
        this.file = file;
    }

    @Override
    public ImageInputStream newImageInputStream() throws IOException {
        return new FileImageInputStream(file);
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return new FileInputStream(file);
    }

}
