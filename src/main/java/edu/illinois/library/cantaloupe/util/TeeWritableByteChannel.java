package edu.illinois.library.cantaloupe.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * WritableByteChannel that wraps two WritableByteChannels, for
 * pseudo-simultaneous writing.
 */
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
            channel1.close();
        } finally {
            channel2.close();
        }
    }

    /**
     * @return True if either of the wrapped channels is open.
     */
    @Override
    public boolean isOpen() {
        return (channel1.isOpen() || channel2.isOpen());
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        final int position = src.position();
        try {
            return channel1.write(src);
        } finally {
            src.position(position);
            channel2.write(src);
        }
    }
}
