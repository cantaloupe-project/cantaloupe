package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;

abstract class AbstractResolverTest extends BaseTest {

    abstract void destroyEndpoint() throws Exception;
    abstract void initializeEndpoint() throws Exception;
    abstract Resolver newInstance();
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

            Resolver instance = newInstance();
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

            Resolver instance = newInstance();
            instance.checkAccess();
            instance.checkAccess();
            instance.checkAccess();
        } finally {
            destroyEndpoint();
        }
    }

    /* getSourceFormat() */

    @Test
    public void testGetSourceFormatInvokedMultipleTimes() throws Exception {
        try {
            initializeEndpoint();

            Resolver instance = newInstance();
            instance.getSourceFormat();
            instance.getSourceFormat();
            instance.getSourceFormat();
        } finally {
            destroyEndpoint();
        }
    }

    /* newStreamSource() */

    @Test
    public void testNewStreamSourceInvokedMultipleTimes() throws Exception {
        Resolver instance = newInstance();
        if (instance instanceof StreamResolver) {
            try {
                initializeEndpoint();

                StreamResolver sres = (StreamResolver) instance;
                sres.newStreamSource();
                sres.newStreamSource();
                sres.newStreamSource();
            } finally {
                destroyEndpoint();
            }
        }
    }

    @Test
    public void testNewStreamSourceReturnedInstanceIsReusable()
            throws Exception {
        Resolver instance = newInstance();
        if (instance instanceof StreamResolver) {
            try {
                initializeEndpoint();

                StreamResolver sres = (StreamResolver) instance;
                StreamSource source = sres.newStreamSource();

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
