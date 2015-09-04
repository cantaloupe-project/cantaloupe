package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Interface to be implemented by all image processors.
 */
public interface Processor {

    /**
     * @param inputStream An InputStream from which to read the image
     * @param imageBaseUri Base URI of the image
     * @return ImageInfo describing the image
     */
    ImageInfo getImageInfo(InputStream inputStream, String imageBaseUri);

    /**
     * @return List of output formats supported by the processor.
     */
    List<OutputFormat> getSupportedOutputFormats();

    /**
     * Uses the supplied parameters to process an image from the supplied
     * InputStream, and writes the result to the given OutputStream.
     *
     * @param p Parameters of the output image
     * @param is An InputStream from which to read the image
     * @param os An OutputStream to which to write the image
     * @throws Exception
     */
    void process(Parameters p, InputStream is, OutputStream os)
            throws Exception;

    /**
     * @return Human-readable name of the processor.
     */
    String toString();

}
