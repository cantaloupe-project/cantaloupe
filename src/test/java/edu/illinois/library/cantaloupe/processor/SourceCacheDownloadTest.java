package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SourceCacheDownloadTest extends BaseTest {

    private static final String IMAGE = "jpg";

    private SourceCache sourceCache;
    private SourceCacheDownload instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        sourceCache = new SourceCache() {
            private Path tempFile;

            {
                tempFile = Files.createTempFile("test", "jpg");
                Files.delete(tempFile);
            }

            @Override
            public Optional<Path> getSourceImageFile(Identifier identifier)
                    throws IOException {
                try {
                    if (Files.size(tempFile) > 0) {
                        return Optional.of(tempFile);
                    }
                } catch (NoSuchFileException ignore) {
                }
                return Optional.empty();
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
                new PathStreamFactory(TestUtil.getImage(IMAGE)),
                sourceCache,
                new Identifier("cats"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        sourceCache.purge();
    }

    @Test
    void testCancelWithTrueArgument() {
        assertFalse(instance.isCancelled());
        instance.cancel(true);
        assertTrue(instance.isCancelled());
    }

    @Test
    void testCancelWithFalseArgument() {
        assertFalse(instance.isCancelled());
        instance.cancel(false);
        assertTrue(instance.isCancelled());
    }

    @Test
    void testGet() throws Exception {
        instance.downloadAsync();

        Path actualFile = instance.get();
        assertEquals(Files.size(TestUtil.getImage("jpg")),
                Files.size(actualFile));
    }

    @Test
    void testIsCancelled() {
        assertFalse(instance.isCancelled());
        instance.cancel(true);
        assertTrue(instance.isCancelled());
    }

    @Test
    void testIsDone() throws Exception {
        assertFalse(instance.isDone());
        instance.downloadAsync();
        instance.get();
        assertTrue(instance.isDone());
    }

}
