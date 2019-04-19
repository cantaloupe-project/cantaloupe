package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractSourceTest extends BaseTest {

    abstract void destroyEndpoint() throws Exception;
    abstract void initializeEndpoint() throws Exception;
    abstract Source newInstance();
    abstract void useBasicLookupStrategy();
    abstract void useScriptLookupStrategy();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        useBasicLookupStrategy();
    }

    /* checkAccess() */

    @Test
    void testCheckAccessUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        try {
            initializeEndpoint();

            newInstance().checkAccess();
        } finally {
            destroyEndpoint();
        }
    }

    @Test
    void testCheckAccessUsingBasicLookupStrategyWithMissingImage()
            throws Exception {
        try {
            initializeEndpoint();

            Source instance = newInstance();
            instance.setIdentifier(new Identifier("bogus"));
            assertThrows(NoSuchFileException.class, instance::checkAccess);
        } finally {
            destroyEndpoint();
        }
    }

    @Test
    void testCheckAccessInvokedMultipleTimes() throws Exception {
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
    void testGetFormatInvokedMultipleTimes() throws Exception {
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
    void testNewStreamFactoryInvokedMultipleTimes() throws Exception {
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
    void testNewStreamFactoryReturnedInstanceIsReusable()
            throws Exception {
        Source instance = newInstance();
        if (instance instanceof StreamSource) {
            try {
                initializeEndpoint();

                StreamSource sres = (StreamSource) instance;
                StreamFactory source = sres.newStreamFactory();

                try (InputStream is = source.newInputStream();
                     OutputStream os = OutputStream.nullOutputStream()) {
                    is.transferTo(os);
                }

                try (InputStream is = source.newInputStream();
                     OutputStream os = OutputStream.nullOutputStream()) {
                    is.transferTo(os);
                }
            } finally {
                destroyEndpoint();
            }
        }
    }

}
