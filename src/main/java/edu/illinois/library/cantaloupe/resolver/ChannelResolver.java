package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Identifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.AccessDeniedException;

/**
 * Interface to be implemented by all resolvers that support access to source
 * images via NIO readable byte channels.
 */
public interface ChannelResolver extends Resolver {

    /**
     * @param identifier
     * @return Stream for reading the source image; never null.
     * @throws FileNotFoundException if the image corresponding to the given
     * identifier does not exist
     * @throws AccessDeniedException if the image corresponding to the given
     * identifier is not readable
     * @throws IOException if there is some other issue accessing the image
     */
    ReadableByteChannel getChannel(Identifier identifier) throws IOException;

}
