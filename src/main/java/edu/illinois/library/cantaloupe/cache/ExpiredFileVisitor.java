package edu.illinois.library.cantaloupe.cache;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.library.cantaloupe.config.Configuration;

/**
 * Used by {@link Files#walkFileTree} to delete all expired files within
 * a directory.
 */
class ExpiredFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExpiredFileVisitor.class);

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
        FilesystemCache filesystemCache = new FilesystemCache(Configuration.getInstance());
        try {
            final boolean delete =
                    (Files.isRegularFile(path) && filesystemCache.isExpired(path));

            LOGGER.trace("{}: last accessed: {}; last modified; {}; " +
                            "effective last accessed: {}; delete? {}",
                    path,
                    Files.getAttribute(path, "lastAccessTime"),
                    Files.getLastModifiedTime(path),
                    FilesystemCache.getLastAccessedTime(path),
                    delete);

            if (delete) {
                final long size = Files.size(path);
                Files.deleteIfExists(path);
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
