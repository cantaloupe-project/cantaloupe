package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.resolver.PathStreamSource;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class TempFileDownloadTest extends BaseTest {

    private TempFileDownload instance;
    private Path tempFile;

    @Test
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("test", "tmp");
        Files.delete(tempFile);

        instance = new TempFileDownload(
                new PathStreamSource(TestUtil.getImage("jpg")),
                tempFile);
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(tempFile);
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
        assertEquals(tempFile, actualFile);
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
