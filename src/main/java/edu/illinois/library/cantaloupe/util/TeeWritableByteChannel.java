package edu.illinois.library.cantaloupe.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class TeeWritableByteChannel implements WritableByteChannel {

    private final WritableByteChannel channel1;
    private final WritableByteChannel channel2;

    public TeeWritableByteChannel(WritableByteChannel channel1,
                                  WritableByteChannel channel2) {
        this.channel1 = channel1;
        this.channel2 = channel2;
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel1.close();
        } finally {
            this.channel2.close();
        }
    }

    @Override
    public boolean isOpen() {
        return (channel1.isOpen() && channel2.isOpen());
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        channel1.write(src);
        return channel2.write(src); // TODO: will the channels ever have different return values?
    }
}
