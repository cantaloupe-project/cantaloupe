package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import org.restlet.data.Form;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface to be implemented by image processors that support input via
 * streams.
 */
public interface StreamProcessor extends Processor {

    /**
     * @param inputStream Source image. Implementations should not close it.
     * @param sourceFormat Format of the source image
     * @return Size of the source image in pixels.
     * @throws ProcessorException
     */
    Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException;

    /**
     * <p>Uses the supplied parameters to process an image from the supplied
     * InputStream, and writes the result to the given OutputStream.</p>
     *
     * <p>Implementations should use the sourceSize parameter and not their
     * own <code>getSize()</code> method to avoid reusing a potentially
     * unreusable InputStream.</p>
     *
     * @param params Parameters of the output image
     * @param query The URL query. This enables processors to support custom
     *              options and operations not available in the parameters, in
     *              a non-standard way.
     * @param sourceFormat Format of the source image
     * @param sourceSize Size of the source image
     * @param inputStream Stream from which to read the image. Implementations
     *                    should not close it.
     * @param outputStream Stream to which to write the image. Implementations
     *                     should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(Parameters params, Form query, SourceFormat sourceFormat,
                 Dimension sourceSize, InputStream inputStream,
                 OutputStream outputStream) throws ProcessorException;

}
