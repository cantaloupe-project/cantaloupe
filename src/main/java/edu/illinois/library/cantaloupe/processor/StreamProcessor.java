package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.resolver.StreamSource;

/**
 * Interface to be implemented by image processors that support input via
 * streams.
 */
public interface StreamProcessor extends Processor {

    /**
     * @return Source for acquiring streams from which to read the image.
     */
    StreamSource getStreamSource();

    /**
     * @param source Source for acquiring streams from which to read
     *               the image.
     */
    void setStreamSource(StreamSource source);

}
