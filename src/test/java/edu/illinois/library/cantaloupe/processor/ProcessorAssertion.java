package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Rather than using {@literal assert}-style methods, many processor tests use
 * instances of this class in order to encapsulate more complex testing logic.
 */
abstract class ProcessorAssertion implements Runnable {

    /**
     * Set of fixture filenames to skip.
     */
    Set<String> skippedFixtures = new HashSet<>();

    /**
     * Resulting image.
     */
    BufferedImage resultingImage;

    /**
     * Raw resulting image bytes.
     */
    byte[] resultingRawImage;

    /**
     * Operation list used to process the image.
     */
    OperationList opList;

    /**
     * Sample/component size of the source image. May be {@literal null} if the
     * fixture does not have dimensions in its filename (see the docs for
     * {@link AbstractProcessorTest}).
     */
    int sourceSampleSize;

    /**
     * Dimensions of the source image. May be {@literal null} if the fixture
     * does not have dimensions in its filename (see the docs for
     * {@link AbstractProcessorTest}).
     */
    Dimension sourceSize;

    /**
     * Tests whether the resulting image is as expected, throwing an unchecked
     * exception if not.
     */
    @Override
    public abstract void run();

}
