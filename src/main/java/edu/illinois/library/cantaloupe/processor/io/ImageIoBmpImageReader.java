package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import java.io.File;
import java.io.IOException;

class ImageIoBmpImageReader extends AbstractImageIoImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    ImageIoBmpImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.BMP);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    ImageIoBmpImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.BMP);
    }

}
