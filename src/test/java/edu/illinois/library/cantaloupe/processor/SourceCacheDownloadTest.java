package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resolver.PathStreamSource;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class SourceCacheDownloadTest extends BaseTest {

    private static final String IMAGE = "jpg";

    private SourceCache sourceCache;
    private SourceCacheDownload instance;

    @Test
    public void setUp() throws Exception {
        sourceCache = new SourceCache() {
            private Path tempFile;

            {
                tempFile = Files.createTempFile("test", "jpg");
                Files.delete(tempFile);
            }

            @Override
            public Path getSourceImageFile(Identifier identifier)
                    throws IOException {
                try {
                    return Files.size(tempFile) > 0 ? tempFile : null;
                } catch (NoSuchFileException e) {
                    return null;
                }
            }

            @Override
            public OutputStream newSourceImageOutputStream(Identifier identifier)
                    throws IOException {
                return Files.newOutputStream(tempFile);
            }

            @Override
            public void purge() throws IOException {
                Files.deleteIfExists(tempFile);
            }

            @Override
            public void purge(Identifier identifier) {}

            @Override
            public void purgeInvalid() {}
        };

        instance = new SourceCacheDownload(
                new PathStreamSource(TestUtil.getImage(IMAGE)),
                sourceCache,
                new Identifier("cats"));
    }

    @After
    public void tearDown() throws Exception {
        sourceCache.purge();
    }

    @Test
    public void testCancelWithTrueArgument() {
        assertFalse(instance.isCancelled());
        instance.cancel(true);
        assertTrue(instance.isCancelled());
    }

    @Test
    public void testCancelWithFalseArgument() {
        assertFalse(instance.isCancelled());
        instance.cancel(false);
        assertTrue(instance.isCancelled());
    }

    @Test
    public void testGet() throws Exception {
        instance.downloadAsync();

        Path actualFile = instance.get();
        assertEquals(Files.size(TestUtil.getImage("jpg")),
                Files.size(actualFile));
    }

    @Test
    public void testIsCancelled() {
        assertFalse(instance.isCancelled());
        instance.cancel(true);
        assertTrue(instance.isCancelled());
    }

    @Test
    public void testIsDone() throws Exception {
        assertFalse(instance.isDone());
        instance.downloadAsync();
        instance.get();
        assertTrue(instance.isDone());
    }

}
