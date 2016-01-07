package edu.illinois.library.cantaloupe.resolver;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * Provides new channels to read from.
 */
public interface ChannelSource {

    /**
     * @return New channel to read from.
     * @throws IOException
     */
    ReadableByteChannel newChannel() throws IOException;

}
