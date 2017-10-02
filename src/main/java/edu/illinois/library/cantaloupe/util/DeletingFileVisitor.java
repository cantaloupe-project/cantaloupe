package edu.illinois.library.cantaloupe.util;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Enables {@link Files#walkFileTree(Path, FileVisitor)} to recursively delete
 * a directory's contents.
 */
public class DeletingFileVisitor extends SimpleFileVisitor<Path> {

    private boolean deleteRootDir = true;
    private Logger logger;
    private long deletedFileCount = 0;
    private long deletedFileSize = 0;
    private Path rootPath;

    /**
     * @return Total number of deleted files.
     */
    public long getDeletedFileCount() {
        return deletedFileCount;
    }

    /**
     * @return Total byte size of all deleted files.
     */
    public long getDeletedFileSize() {
        return deletedFileSize;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * @param path Root path to exclude from the deletion.
     */
    public void setRootPathToExclude(Path path) {
        this.deleteRootDir = false;
        this.rootPath = path;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
            throws IOException {
        if (attributes.isRegularFile()) {
            if (logger != null) {
                logger.debug("Deleting file: {}", file);
            }
            final long size = Files.size(file);
            Files.delete(file);
            deletedFileSize += size;
            deletedFileCount++;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e)
            throws IOException {
        if (!deleteRootDir && dir.equals(rootPath)) {
            return FileVisitResult.TERMINATE;
        }
        if (logger != null) {
            logger.debug("Deleting {}", dir);
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e)
            throws IOException {
        if (logger != null) {
            logger.warn("Failed to delete file: {}", e.getMessage());
        }
        return FileVisitResult.CONTINUE;
    }

}