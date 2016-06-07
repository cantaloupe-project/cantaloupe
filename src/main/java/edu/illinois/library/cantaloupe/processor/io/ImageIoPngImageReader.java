package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import java.io.File;
import java.io.IOException;

class ImageIoPngImageReader extends AbstractImageIoImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    ImageIoPngImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.PNG);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    ImageIoPngImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.PNG);
    }

}
