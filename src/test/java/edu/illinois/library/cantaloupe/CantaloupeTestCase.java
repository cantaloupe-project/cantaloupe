package edu.illinois.library.cantaloupe;

import junit.framework.TestCase;

/**
 * <p>Base test case class for all Cantaloupe unit tests.</p>
 *
 * <p>This class is named CantaloupeTestCase and not just TestCase in order to
 * prevent bugs stemming from mistakenly importing the wrong TestCase.</p>
 */
public abstract class CantaloupeTestCase extends TestCase {

    static {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

}
