package edu.illinois.library.cantaloupe.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Used by {@link Files#walkFileTree} to delete all info files within a
 * directory.
 */
class InfoFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InfoFileVisitor.class);

    private long deletedFileCount = 0;
    private long deletedFileSize = 0;

    long getDeletedFileCount() {
        return deletedFileCount;
    }

    long getDeletedFileSize() {
        return deletedFileSize;
    }

    @Override
    public FileVisitResult visitFile(Path path,
                                     BasicFileAttributes attrs) {
        try {
            if (path.toString().endsWith(FilesystemCache.INFO_EXTENSION)) {
                long size = Files.size(path);
                Files.delete(path);
                deletedFileCount++;
                deletedFileSize += size;
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        LOGGER.warn("visitFileFailed(): {}", e.getMessage());
        return FileVisitResult.CONTINUE;
    }

}
