package edu.illinois.library.cantaloupe.processor;

import java.nio.file.Path;

/**
 * Interface to be implemented by processors that support input via direct
 * file access.
 */
public interface FileProcessor extends Processor {

    /**
     * @return Source image file.
     */
    Path getSourceFile();

    /**
     * @param sourceFile File from which to read the image.
     */
    void setSourceFile(Path sourceFile);

}
