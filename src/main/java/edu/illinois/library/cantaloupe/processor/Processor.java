package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Interface to be implemented by all image processors.
 */
public interface Processor {

    /**
     * @param inputStream An InputStream from which to read the image
     * @param sourceFormat Format of the image
     * @param imageBaseUri Base URI of the image
     * @return ImageInfo describing the image
     * @throws Exception
     */
    ImageInfo getImageInfo(InputStream inputStream, SourceFormat sourceFormat,
                           String imageBaseUri) throws Exception;

    /**
     * @return Output formats supported by the processor.
     */
    Set<OutputFormat> getSupportedOutputFormats();

    /**
     * Uses the supplied parameters to process an image from the supplied
     * InputStream, and writes the result to the given OutputStream.
     *
     * @param params Parameters of the output image
     * @param sourceFormat Format of the source image
     * @param inputStream An InputStream from which to read the image
     * @param outputStream An OutputStream to which to write the image
     * @throws Exception
     */
    void process(Parameters params, SourceFormat sourceFormat,
                 InputStream inputStream, OutputStream outputStream)
            throws Exception;

    /**
     * @return Human-readable name of the processor.
     */
    String toString();

}
