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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

abstract class AbstractCacheTest extends BaseTest {

    static final int ASYNC_WAIT = 3500;
    static final String IMAGE   = "jpg";

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

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 300);
    }

    /* getInfo(Identifier) */

    @Test
    void testGetInfoWithExistingValidImage() throws Exception {
        final DerivativeCache instance = newInstance();

        Identifier identifier = new Identifier("cats");
        Info info = new Info();
        instance.put(identifier, info);

        Optional<Info> actual = instance.getInfo(identifier);
        assertEquals(actual.orElseThrow(), info);
    }

    @Test
    void testGetInfoWithExistingInvalidImage() throws Exception {
        final DerivativeCache instance = newInstance();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 1);

        Identifier identifier = new Identifier("cats");
        Info info = new Info();
        instance.put(identifier, info);

        Thread.sleep(ASYNC_WAIT);

        assertFalse(instance.getInfo(identifier).isPresent());
    }

    @Test
    void testGetInfoWithNonexistentImage() throws Exception {
        final DerivativeCache instance = newInstance();
        assertFalse(instance.getInfo(new Identifier("bogus")).isPresent());
    }

    @Test
    void testGetInfoPopulatesSerializationTimestampWhenNotAlreadySet()
            throws Exception {
        // These caches don't support this feature.
        assumeFalse(this instanceof HeapCacheTest ||
                this instanceof JdbcCacheTest ||
                this instanceof RedisCacheTest);
        final DerivativeCache instance = newInstance();

        Identifier identifier = new Identifier("cats");
        Info info = new Info();
        instance.put(identifier, info);

        info = instance.getInfo(identifier).get();
        assertNotNull(info.getSerializationTimestamp());
    }

    @Test
    void testGetInfoConcurrently() {
        // This is tested in testPutConcurrently()
    }

    /* newDerivativeImageInputStream(OperationList) */

    @Test
    void testNewDerivativeImageInputStreamWithZeroTTL() throws Exception {
        final DerivativeCache instance = newInstance();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 0);

        OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Path imageFile = TestUtil.getImage(IMAGE);

        // Write an image to the cache
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(opList)) {
            Files.copy(imageFile, os);
            os.setComplete(true);
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
    void testNewDerivativeImageInputStreamWithNonzeroTTL() throws Exception {
        final DerivativeCache instance = newInstance();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 3);

        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Path fixture = TestUtil.getImage(IMAGE);

        // Add an image.
        // This method may return before data is fully (or even partially)
        // written to the cache.
        // N.B.: This method may return before data is fully (or even
        // partially) written to the cache.
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(fixture, os);
            os.setComplete(true);
        }

        // Wait for it to finish, hopefully.
        Thread.sleep(2000);

        // Assert that it has been added.
        assertExists(instance, ops);

        // Wait for it to invalidate.
        Thread.sleep(4000);

        // Assert that it has been purged.
        assertNotExists(instance, ops);
    }

    @Test
    void testNewDerivativeImageInputStreamWithNonexistentImage()
            throws Exception {
        final DerivativeCache instance = newInstance();
        final OperationList ops = new OperationList(new Identifier("cats"));

        instance.purge();
        assertNotExists(instance, ops);
    }

    @Test
    void testNewDerivativeImageInputStreamConcurrently() throws Exception {
        final DerivativeCache instance = newInstance();
        final OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        new ConcurrentReaderWriter(() -> {
            try (CompletableOutputStream os =
                         instance.newDerivativeImageOutputStream(ops)) {
                Files.copy(TestUtil.getImage(IMAGE), os);
                os.setComplete(true);
            }
            return null;
        }, () -> {
            try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
                if (is != null) {
                    //noinspection StatementWithEmptyBody
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
    void testNewDerivativeImageOutputStream() throws Exception {
        final DerivativeCache instance = newInstance();
        final OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        final Path fixture = TestUtil.getImage("jpg");

        // Assert that it's not already cached
        assertNull(instance.newDerivativeImageInputStream(ops));

        // Add it to the cache
        try (CompletableOutputStream outputStream =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(fixture, outputStream);
            outputStream.setComplete(true);
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
    void testNewDerivativeImageOutputStreamDoesNotLeaveDetritusWhenStreamIsIncompletelyWritten()
            throws Exception {
        final DerivativeCache instance = newInstance();
        final OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        final Path fixture = TestUtil.getImage("jpg");

        // Add it to the cache
        try (CompletableOutputStream outputStream =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(fixture, outputStream);
            outputStream.setComplete(false); // the whole point of the test
        }

        // Wait for it to upload
        Thread.sleep(ASYNC_WAIT);

        // Try to read it back in
        try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }
    }

    @Test
    void testNewDerivativeImageOutputStreamConcurrently() {
        // This is tested in testNewDerivativeImageInputStreamConcurrently()
    }

    @Test
    void testNewDerivativeImageOutputStreamOverwritesExistingImage() {
        // TODO: write this
    }

    /* purge() */

    @Test
    void testPurge() throws Exception {
        DerivativeCache instance = newInstance();
        Identifier identifier = new Identifier(IMAGE);
        OperationList opList = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Info info = new Info();

        // assert that a particular image doesn't exist
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }

        // assert that a particular info doesn't exist
        assertFalse(instance.getInfo(identifier).isPresent());

        // add the image
        try (CompletableOutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Path fixture = TestUtil.getImage(IMAGE);
            Files.copy(fixture, outputStream);
            outputStream.setComplete(true);
        }

        // add the info
        instance.put(identifier, info);

        Thread.sleep(ASYNC_WAIT);

        // assert that they've been added
        assertExists(instance, opList);
        assertNotNull(instance.getInfo(identifier));

        // purge everything
        instance.purge();

        // Allow time for purge but not as long as upload
        Thread.sleep(ASYNC_WAIT / 2);

        // assert that the info has been purged
        assertFalse(instance.getInfo(identifier).isPresent());

        // assert that the image has been purged
        assertNotExists(instance, opList);
    }

    /* purge(Identifier) */

    @Test
    void testPurgeWithIdentifier() throws Exception {
        DerivativeCache instance = newInstance();

        final Path fixture = TestUtil.getImage(IMAGE);

        // add an image and an info
        final Identifier id1        = new Identifier("cats");

        final OperationList opList1 = OperationList.builder()
                .withIdentifier(id1)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(opList1)) {
            Files.copy(fixture, os);
            os.setComplete(true);
        }
        instance.put(id1, new Info());

        // add another image and another info
        final Identifier id2        = new Identifier("dogs");
        final OperationList opList2 = OperationList.builder()
                .withIdentifier(id2)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(opList2)) {
            Files.copy(fixture, os);
            os.setComplete(true);
        }
        instance.put(id2, new Info());

        assertNotNull(instance.getInfo(id1));
        assertNotNull(instance.getInfo(id2));

        // purge one of the info/image pairs
        instance.purge(id1);

        Thread.sleep(2000);

        // assert that its info and image are gone
        assertFalse(instance.getInfo(id1).isPresent());

        try (InputStream is = instance.newDerivativeImageInputStream(opList1)) {
            assertNull(is);
        }

        // ... but the other one is still there
        assertNotNull(instance.getInfo(id2));
        try (InputStream is = instance.newDerivativeImageInputStream(opList2)) {
            assertNotNull(is);
        }
    }

    /* purge(OperationList) */

    @Test
    void testPurgeWithOperationList() throws Exception {
        final DerivativeCache instance = newInstance();

        // Seed a derivative image
        OperationList ops1 = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops1)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
            os.setComplete(true);
        }

        // Seed another derivative image
        OperationList ops2 = OperationList.builder()
                .withIdentifier(new Identifier("dogs"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops2)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
            os.setComplete(true);
        }

        Thread.sleep(ASYNC_WAIT);

        // Purge the first one
        instance.purge(ops1);

        // Assert that it was purged
        assertNotExists(instance, ops1);

        // Assert that the other one was NOT purged
        assertExists(instance, ops2);
    }

    /* purgeInfos() */

    @Test
    void testPurgeInfos() throws Exception {
        DerivativeCache instance = newInstance();
        Identifier identifier    = new Identifier(IMAGE);
        OperationList opList     = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Info info = new Info();

        // assert that a particular image doesn't exist
        try (InputStream is = instance.newDerivativeImageInputStream(opList)) {
            assertNull(is);
        }

        // assert that a particular info doesn't exist
        assertFalse(instance.getInfo(identifier).isPresent());

        // add the image
        try (CompletableOutputStream outputStream =
                     instance.newDerivativeImageOutputStream(opList)) {
            Path fixture = TestUtil.getImage(IMAGE);
            Files.copy(fixture, outputStream);
            outputStream.setComplete(true);
        }

        // add the info
        instance.put(identifier, info);

        Thread.sleep(ASYNC_WAIT);

        // assert that they've been added
        assertExists(instance, opList);
        assertNotNull(instance.getInfo(identifier));

        // purge infos
        instance.purgeInfos();

        // assert that the info has been purged
        assertFalse(instance.getInfo(identifier).isPresent());

        // assert that the image has NOT been purged
        assertExists(instance, opList);
    }

    /* purgeInvalid() */

    @Test
    void testPurgeInvalid() throws Exception {
        DerivativeCache instance = newInstance();
        Identifier id1           = new Identifier(IMAGE);
        OperationList ops1       = OperationList.builder()
                .withIdentifier(id1)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Info info1 = new Info();
        Configuration.getInstance().setProperty(Key.DERIVATIVE_CACHE_TTL, 2);

        // add an image
        Path fixture = TestUtil.getImage(id1.toString());
        try (CompletableOutputStream outputStream =
                     instance.newDerivativeImageOutputStream(ops1)) {
            Files.copy(fixture, outputStream);
            outputStream.setComplete(true);
        }

        // add an Info
        instance.put(id1, info1);

        // assert that they've been added
        assertNotNull(instance.getInfo(id1));
        assertExists(instance, ops1);

        // wait for them to invalidate
        Thread.sleep(2100);

        // add another image
        Path fixture2 = TestUtil.getImage("gif-rgb-64x56x8.gif");
        OperationList ops2 = OperationList.builder()
                .withIdentifier(new Identifier(fixture2.getFileName().toString()))
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        try (CompletableOutputStream outputStream =
                     instance.newDerivativeImageOutputStream(ops2)) {
            Files.copy(fixture2, outputStream);
            outputStream.setComplete(true);
        }

        // add another info
        Identifier id2 = new Identifier("cats");
        instance.put(id2, new Info());

        // assert that they've been added
        assertNotNull(instance.getInfo(id2));
        assertExists(instance, ops2);

        instance.purgeInvalid();

        // assert that one image and one info have been purged
        assertFalse(instance.getInfo(id1).isPresent());
        assertTrue(instance.getInfo(id2).isPresent());
        assertNotExists(instance, ops1);
        assertExists(instance, ops2);
    }

    /* put(Identifier, Info) */

    @Test
    void testPutWithInfo() throws Exception {
        final DerivativeCache instance = newInstance();
        final Identifier identifier    = new Identifier("cats");
        final Info info                = new Info();

        instance.put(identifier, info);

        Optional<Info> actualInfo = instance.getInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    /**
     * Tests that concurrent calls of {@link
     * DerivativeCache#put(Identifier, Info)} and {@link
     * DerivativeCache#getInfo(Identifier)} don't conflict.
     */
    @Test
    void testPutWithInfoConcurrently() throws Exception {
        final DerivativeCache instance = newInstance();
        final Identifier identifier    = new Identifier("monkeys");
        final Info info                = new Info();

        new ConcurrentReaderWriter(() -> {
            instance.put(identifier, info);
            return null;
        }, () -> {
            Optional<Info> otherInfo = instance.getInfo(identifier);
            if (otherInfo.isPresent() && !info.equals(otherInfo.get())) {
                fail();
            }
            return null;
        }).run();
    }

    @Test
    void testPutWithInfoWithNonPersistableInstance() throws Exception {
        final DerivativeCache instance = newInstance();
        final Identifier identifier    = new Identifier("incomplete");
        final Info info                = new Info();
        info.setPersistable(false);

        instance.put(identifier, info);

        Optional<Info> actualInfo = instance.getInfo(identifier);
        assertFalse(actualInfo.isPresent());
    }

    /* put(Identifier, String) */

    @Test
    void testPutWithString() throws Exception {
        final DerivativeCache instance = newInstance();
        final Identifier identifier    = new Identifier("cats");
        final Info info                = new Info();
        final String infoStr           = info.toJSON();

        instance.put(identifier, infoStr);

        Optional<Info> actualInfo = instance.getInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

    /**
     * Tests that concurrent calls of {@link
     * DerivativeCache#put(Identifier, String)} and {@link
     * DerivativeCache#getInfo(Identifier)} don't conflict.
     */
    @Test
    void testPutWithStringConcurrently() throws Exception {
        final DerivativeCache instance = newInstance();
        final Identifier identifier    = new Identifier("monkeys");
        final Info info                = new Info();
        final String infoStr           = info.toJSON();

        new ConcurrentReaderWriter(() -> {
            instance.put(identifier, infoStr);
            return null;
        }, () -> {
            Optional<Info> otherInfo = instance.getInfo(identifier);
            if (otherInfo.isPresent() && !info.equals(otherInfo.get())) {
                fail();
            }
            return null;
        }).run();
    }

}
