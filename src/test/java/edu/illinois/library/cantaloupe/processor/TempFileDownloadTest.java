package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TempFileDownloadTest extends BaseTest {

    private TempFileDownload instance;
    private Path tempFile;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        tempFile = Files.createTempFile("test", "tmp");
        Files.delete(tempFile);

        instance = new TempFileDownload(
                new PathStreamFactory(TestUtil.getImage("jpg")),
                tempFile);
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        Files.deleteIfExists(tempFile);
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
        assertEquals(tempFile, actualFile);
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
