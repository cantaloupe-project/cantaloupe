package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;

abstract class AbstractSourceTest extends BaseTest {

    abstract void destroyEndpoint() throws Exception;
    abstract void initializeEndpoint() throws Exception;
    abstract Source newInstance();
    abstract void useBasicLookupStrategy();
    abstract void useScriptLookupStrategy();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        useBasicLookupStrategy();
    }

    /* checkAccess() */

    @Test
    public void testCheckAccessUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        try {
            initializeEndpoint();

            newInstance().checkAccess();
        } finally {
            destroyEndpoint();
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccessUsingBasicLookupStrategyWithMissingImage()
            throws Exception {
        try {
            initializeEndpoint();

            Source instance = newInstance();
            instance.setIdentifier(new Identifier("bogus"));
            instance.checkAccess();
        } finally {
            destroyEndpoint();
        }
    }

    @Test
    public void testCheckAccessInvokedMultipleTimes() throws Exception {
        try {
            initializeEndpoint();

            Source instance = newInstance();
            instance.checkAccess();
            instance.checkAccess();
            instance.checkAccess();
        } finally {
            destroyEndpoint();
        }
    }

    /* getFormat() */

    @Test
    public void testGetFormatInvokedMultipleTimes() throws Exception {
        try {
            initializeEndpoint();

            Source instance = newInstance();
            instance.getFormat();
            instance.getFormat();
            instance.getFormat();
        } finally {
            destroyEndpoint();
        }
    }

    /* newStreamFactory() */

    @Test
    public void testNewStreamFactoryInvokedMultipleTimes() throws Exception {
        Source instance = newInstance();
        if (instance instanceof StreamSource) {
            try {
                initializeEndpoint();

                StreamSource sres = (StreamSource) instance;
                sres.newStreamFactory();
                sres.newStreamFactory();
                sres.newStreamFactory();
            } finally {
                destroyEndpoint();
            }
        }
    }

    @Test
    public void testNewStreamFactoryReturnedInstanceIsReusable()
            throws Exception {
        Source instance = newInstance();
        if (instance instanceof StreamSource) {
            try {
                initializeEndpoint();

                StreamSource sres = (StreamSource) instance;
                StreamFactory source = sres.newStreamFactory();

                try (InputStream is = source.newInputStream();
                     OutputStream os = new NullOutputStream()) {
                    IOUtils.copy(is, os);
                }

                try (InputStream is = source.newInputStream();
                     OutputStream os = new NullOutputStream()) {
                    IOUtils.copy(is, os);
                }
            } finally {
                destroyEndpoint();
            }
        }
    }

}
