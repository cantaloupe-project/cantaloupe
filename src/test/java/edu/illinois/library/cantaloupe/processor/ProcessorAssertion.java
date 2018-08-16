package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Rather than using {@literal assert}-style methods, many processor tests use
 * instances of this class that can be run against many different fixtures.
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
     * Tests whether the resulting image is as expected.
     */
    @Override
    public abstract void run();

}
