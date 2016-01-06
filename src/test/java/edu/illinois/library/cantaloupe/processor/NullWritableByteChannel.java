package edu.illinois.library.cantaloupe.processor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

class NullWritableByteChannel implements WritableByteChannel {

    @Override
    public void close() throws IOException {
        // noop
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return src.array().length;
    }

}
