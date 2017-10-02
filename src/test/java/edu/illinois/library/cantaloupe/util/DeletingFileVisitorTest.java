package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.*;

public class DeletingFileVisitorTest {

    private DeletingFileVisitor instance;
    private Path tempPath;

    @Before
    public void setUp() throws IOException {
        tempPath = TestUtil.getTempFolder().toPath();
        instance = new DeletingFileVisitor();
        instance.setRootPathToExclude(tempPath);
    }

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(tempPath, instance);
    }

    @Test
    public void testGetDeletedFileCount() throws IOException {
        assertEquals(0, instance.getDeletedFileCount());

        Files.createDirectory(tempPath.resolve("cats"));
        Files.createFile(tempPath.resolve("cats/file"));
        Files.createDirectory(tempPath.resolve("dogs"));
        Files.createFile(tempPath.resolve("dogs/file"));
        Files.walkFileTree(tempPath, instance);

        assertEquals(2, instance.getDeletedFileCount());
    }

    @Test
    public void testGetDeletedFileSize() throws IOException {
        assertEquals(0, instance.getDeletedFileSize());

        Files.createDirectory(tempPath.resolve("cats"));
        Files.write(tempPath.resolve("cats/file"), "bla".getBytes("UTF-8"));
        Files.createDirectory(tempPath.resolve("dogs"));
        Files.write(tempPath.resolve("dogs/file"), "bla".getBytes("UTF-8"));
        Files.walkFileTree(tempPath, instance);

        assertEquals(6, instance.getDeletedFileSize());
    }

    @Test
    public void testVisitFile() throws IOException {
        Path file = tempPath.resolve("file");
        Files.createFile(file);

        instance.visitFile(file, fileAttributesForFile());
        assertFalse(Files.exists(file));
    }

    @Test
    public void testPostVisitDirectory() throws IOException {
        Path dir = tempPath.resolve("dir");
        Files.createDirectory(dir);

        FileVisitResult result = instance.postVisitDirectory(dir,
                new IOException());
        assertEquals(FileVisitResult.CONTINUE, result);
        assertFalse(Files.exists(dir));
    }

    @Test
    public void testPostVisitDirectoryWithRootDirWhenExcludingRootDir()
            throws IOException {
        instance.setRootPathToExclude(tempPath);

        FileVisitResult result = instance.postVisitDirectory(tempPath,
                new IOException());
        assertEquals(FileVisitResult.TERMINATE, result);
        assertTrue(Files.exists(tempPath));
    }

    private BasicFileAttributes fileAttributesForDirectory() {
        return new BasicFileAttributes() {
            @Override
            public FileTime lastModifiedTime() {
                return null;
            }

            @Override
            public FileTime lastAccessTime() {
                return null;
            }

            @Override
            public FileTime creationTime() {
                return null;
            }

            @Override
            public boolean isRegularFile() {
                return false;
            }

            @Override
            public boolean isDirectory() {
                return true;
            }

            @Override
            public boolean isSymbolicLink() {
                return false;
            }

            @Override
            public boolean isOther() {
                return false;
            }

            @Override
            public long size() {
                return 0;
            }

            @Override
            public Object fileKey() {
                return null;
            }
        };
    }

    private BasicFileAttributes fileAttributesForFile() {
        return new BasicFileAttributes() {
            @Override
            public FileTime lastModifiedTime() {
                return null;
            }

            @Override
            public FileTime lastAccessTime() {
                return null;
            }

            @Override
            public FileTime creationTime() {
                return null;
            }

            @Override
            public boolean isRegularFile() {
                return true;
            }

            @Override
            public boolean isDirectory() {
                return false;
            }

            @Override
            public boolean isSymbolicLink() {
                return false;
            }

            @Override
            public boolean isOther() {
                return false;
            }

            @Override
            public long size() {
                return 0;
            }

            @Override
            public Object fileKey() {
                return null;
            }
        };
    }

}
