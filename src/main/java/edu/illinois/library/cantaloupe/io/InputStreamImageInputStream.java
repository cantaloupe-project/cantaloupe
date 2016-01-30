package edu.illinois.library.cantaloupe.io;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an {@link InputStream} in an {@link ImageInputStream}.
 */
public class InputStreamImageInputStream extends ImageInputStreamImpl {

    private InputStream inputStream;

    public InputStreamImageInputStream(InputStream is) {
        this.inputStream = is;
    }

    @Override
    public int read() throws IOException {
        return this.inputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return this.inputStream.read(b, off, len);
    }

}
