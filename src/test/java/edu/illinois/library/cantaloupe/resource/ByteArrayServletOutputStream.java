package edu.illinois.library.cantaloupe.resource;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class ByteArrayServletOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream wrappedStream = new ByteArrayOutputStream();

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
    }

    @Override
    public void write(int b) throws IOException {
        wrappedStream.write(b);
    }

    byte[] toByteArray() {
        return wrappedStream.toByteArray();
    }

}
