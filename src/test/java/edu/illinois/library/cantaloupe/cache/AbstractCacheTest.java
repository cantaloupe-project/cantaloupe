package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConcurrentReaderWriter;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

abstract class AbstractCacheTest extends BaseTest {

    private static final int ASYNC_WAIT = 3000;
    static final String IMAGE = "jpg";

    abstract DerivativeCache newInstance();

    static void assertExists(DerivativeCache cache,
                             OperationList opList) {
        try (InputStream is = cache.newDerivativeImageInputStream(opList)) {
            assertNotNull(is);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    static void assertNotExists(DerivativeCache cache,
                                OperationList opList) {
        try (InputStream is = cache.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 300);
    }

    /* getImageInfo(Identifier) */

    @Test
    public void testGetImageInfoWithExistingValidImage() throws Exception {
        final DerivativeCache instance = newInstance();

        Identifier identifier = new Identifier("cats");
        Info info = new Info();
        instance.put(identifier, info);

        Info actual = instance.getImageInfo(identifier);
        assertEquals(actual, info);
    }

    @Test
    public void testGetImageInfoWithExistingInvalidImage() throws Exception {
        final DerivativeCache instance = newInstance();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 1);

        Identifier identifier = new Identifier("cats");
        Info info = new Info();
        instance.put(identifier, info);

        Thread.sleep(ASYNC_WAIT);

        assertNull(instance.getImageInfo(identifier));
    }

    @Test
    public void testGetImageInfoWithNonexistentImage() throws Exception {
        final DerivativeCache instance = newInstance();
        assertNull(instance.getImageInfo(new Identifier("bogus")));
    }

    @Test
    public void testGetImageInfoConcurrently() {
        // This is tested in testPutConcurrently()
    }

    /* newDerivativeImageInputStream(OperationList) */

    @Test
    public void testNewDerivativeImageInputStreamWithZeroTTL()
            throws Exception {
        final DerivativeCache instance = newInstance();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 0);

        OperationList opList = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        Path imageFile = TestUtil.getImage(IMAGE);

        // Write an image to the cache
        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile, os);
        }

        // Wait for it to upload
        Thread.sleep(ASYNC_WAIT);

        // Read it back in
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            is.transferTo(os);
            os.close();
            assertEquals(Files.size(imageFile), os.toByteArray().length);
        }
    }

    @Test
    public void testNewDerivativeImageInputStreamWithNonzeroTTL()
            throws Exception {
        final DerivativeCache instance = newInstance();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 3);

        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        Path fixture = TestUtil.getImage(IMAGE);

        // Add an image.
        // N.B.: This method may return before data is fully (or even
        // partially) written to the cache.
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(fixture, os);
        }

        // Wait for it to finish, hopefully.
        Thread.sleep(2000);

        // Assert that it has been added.
        assertExists(instance, ops);

        // Wait for it to invalidate.
        Thread.sleep(3000);

        // Assert that it has been purged.
        assertNotExists(instance, ops);
    }

    @Test
    public void testNewDerivativeImageInputStreamWithNonexistentImage()
            throws Exception {
        final DerivativeCache instance = newInstance();
        final OperationList ops = new OperationList(new Identifier("cats"));

        instance.purge();
        assertNotExists(instance, ops);
    }

    @Test
    public void testNewDerivativeImageInputStreamConcurrently()
            throws Exception {
        final DerivativeCache instance = newInstance();
        final OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));

        new ConcurrentReaderWriter(() -> {
            try (OutputStream os =
                         instance.newDerivativeImageOutputStream(ops)) {
                Files.copy(TestUtil.getImage(IMAGE), os);
            }
            return null;
        }, () -> {
            try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
                if (is != null) {
                    while (is.read() != -1) {
                        // consume the stream fully
                    }
                }
            }
            return null;
        }).run();
    }

    /* newDerivativeImageOutputStream() */

    @Test
    public void testNewDerivativeImageOutputStream() throws Exception {
        final DerivativeCache instance = newInstance();
        final OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        final Path fixture = TestUtil.getImage("jpg");

        // Assert that it's not already cached
        assertNull(instance.newDerivativeImageInputStream(ops));

        // Add it to the cache
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(fixture, outputStream);
        }

        // Wait for it to upload
        Thread.sleep(ASYNC_WAIT);

        // Read it back in
        try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            is.transferTo(os);
            os.close();
            assertEquals(Files.size(fixture), os.toByteArray().length);
        }
    }

    @Test
    public void testNewDerivativeImageOutputStreamConcurrently() {
        // This is tested in testNewDerivativeImageInputStreamConcurrently()
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        DerivativeCache instance = newInstance();
        Identifier identifier = new Identifier(IMAGE);
        OperationList opList = new OperationList(
                identifier, new Encode(Format.JPG));
        Info info = new Info();

        // assert that a particular image doesn't exist
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }

        // assert that a particular info doesn't exist
        assertNull(instance.getImageInfo(identifier));

        // add the image
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Path fixture = TestUtil.getImage(IMAGE);
            Files.copy(fixture, outputStream);
        }

        // add the info
        instance.put(identifier, info);

        Thread.sleep(ASYNC_WAIT);

        // assert that they've been added
        assertExists(instance, opList);
        assertNotNull(instance.getImageInfo(identifier));

        // purge everything
        instance.purge();

        // assert that the info has been purged
        assertNull(instance.getImageInfo(identifier));

        // assert that the image has been purged
        assertNotExists(instance, opList);
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        DerivativeCache instance = newInstance();
        Identifier id1 = new Identifier(IMAGE);
        OperationList opList = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        Info info = new Info();

        // add an image
        Path fixture = TestUtil.getImage(id1.toString());
        try (OutputStream os = instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(fixture, os);
        }

        // add an Info
        instance.put(id1, info);

        // add another Info
        Identifier id2 = new Identifier("dogs");
        instance.put(id2, new Info());

        assertNotNull(instance.getImageInfo(id1));
        assertNotNull(instance.getImageInfo(id2));

        // purge one of them
        instance.purge(id1);

        assertNull(instance.getImageInfo(id1));
        assertNotNull(instance.getImageInfo(id2));
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        final DerivativeCache instance = newInstance();

        // Seed a derivative image
        OperationList ops1 = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops1)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }

        // Seed another derivative image
        OperationList ops2 = new OperationList(
                new Identifier("dogs"), new Encode(Format.JPG));
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops2)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }

        Thread.sleep(ASYNC_WAIT);

        // Purge the first one
        instance.purge(ops1);

        // Assert that it was purged
        assertNotExists(instance, ops1);

        // Assert that the other one was NOT purged
        assertExists(instance, ops2);
    }

    /* purgeInvalid() */

    @Test
    public void testPurgeInvalid() throws Exception {
        DerivativeCache instance = newInstance();
        Identifier id1 = new Identifier(IMAGE);
        OperationList ops1 = new OperationList(id1, new Encode(Format.JPG));
        Info info1 = new Info();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 2);

        // add an image
        Path fixture = TestUtil.getImage(id1.toString());
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(ops1)) {
            Files.copy(fixture, outputStream);
        }

        // add an Info
        instance.put(id1, info1);

        // assert that they've been added
        assertNotNull(instance.getImageInfo(id1));
        assertExists(instance, ops1);

        // wait for them to invalidate
        Thread.sleep(2100);

        // add another image
        Path fixture2 = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList ops2 = new OperationList(
                new Identifier(fixture2.getFileName().toString()),
                new Encode(Format.JPG));

        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(ops2)) {
            Files.copy(fixture2, outputStream);
        }

        // add another info
        Identifier id2 = new Identifier("cats");
        instance.put(id2, new Info());

        // assert that they've been added
        assertNotNull(instance.getImageInfo(id2));
        assertExists(instance, ops2);

        instance.purgeInvalid();

        // assert that one image and one info have been purged
        assertNull(instance.getImageInfo(id1));
        assertNotNull(instance.getImageInfo(id2));
        assertNotExists(instance, ops1);
        assertExists(instance, ops2);
    }

    /* put(Identifier, Info) */

    @Test
    public void testPut() throws Exception {
        final DerivativeCache instance = newInstance();
        final Identifier identifier = new Identifier("cats");
        final Info info = new Info();

        instance.put(identifier, info);

        Info actualInfo = instance.getImageInfo(identifier);
        assertEquals(info, actualInfo);
    }

    /**
     * Tests that concurrent calls of {@link
     * DerivativeCache#put(Identifier, Info)} and {@link
     * DerivativeCache#getImageInfo(Identifier)} don't conflict.
     */
    @Test
    public void testPutConcurrently() throws Exception {
        final DerivativeCache instance = newInstance();
        final Identifier identifier = new Identifier("monkeys");
        final Info info = new Info();

        new ConcurrentReaderWriter(() -> {
            instance.put(identifier, info);
            return null;
        }, () -> {
            Info otherInfo = instance.getImageInfo(identifier);
            if (otherInfo != null && !info.equals(otherInfo)) {
                fail();
            }
            return null;
        }).run();
    }

}
