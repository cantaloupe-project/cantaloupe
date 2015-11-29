package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Cache using a filesystem folder, storing images and info files separately in
 * subfolders.
 */
class FilesystemCache implements Cache {

    /**
     * Class whose instances are intended to be serialized to JSON for storing
     * image dimension information.
     *
     * @see <a href="https://github.com/FasterXML/jackson-databind">jackson-databind
     * docs</a>
     */
    @JsonPropertyOrder({ "width", "height" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ImageInfo {
        public int height;
        public int width;
    }

    private static final Logger logger = LoggerFactory.
            getLogger(FilesystemCache.class);

    // https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations
    private static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";
    private static final String IMAGE_FOLDER = "image";
    private static final String INFO_FOLDER = "info";
    private static final String INFO_EXTENSION = ".json";

    private static final ObjectMapper infoMapper = new ObjectMapper();

    private boolean flushingInProgress = false; // TODO: make an AtomicBoolean

    /** Set of identifiers for which info files are currently being read. */
    private final Set<Identifier> infosBeingRead = new ConcurrentSkipListSet<>();

    /** Set of identifiers for which info files are currently being written. */
    private final Set<Identifier> infosBeingWritten = new ConcurrentSkipListSet<>();

    /** Set of Operations for which image files are currently being flushed by
     * flush(OperationList). */
    private final Set<OperationList> opsBeingFlushed =
            new ConcurrentSkipListSet<>();

    /** Lock object for synchronization */
    private final Object lock1 = new Object();
    /** Lock object for synchronization */
    private final Object lock2 = new Object();
    /** Lock object for synchronization */
    private final Object lock3 = new Object();
    /** Lock object for synchronization */
    private final Object lock4 = new Object();

    /**
     * @return Pathname of the root cache folder.
     */
    private static String getCachePathname() {
        return Application.getConfiguration().
                getString("FilesystemCache.pathname");
    }

    /**
     * @return Pathname of the image cache folder, or null if
     * <code>FilesystemCache.pathname</code> is not set.
     */
    private static String getImagePathname() {
        final String pathname = getCachePathname();
        if (pathname != null) {
            return pathname + File.separator + IMAGE_FOLDER;
        }
        return null;
    }

    /**
     * @return Pathname of the info cache folder, or null if
     * <code>FilesystemCache.pathname</code> is not set.
     */
    private static String getInfoPathname() {
        final String pathname = getCachePathname();
        if (pathname != null) {
            return pathname + File.separator + INFO_FOLDER;
        }
        return null;
    }

    private static boolean isExpired(File file) {
        final long ttlMsec = 1000 * Application.getConfiguration().
                getLong("FilesystemCache.ttl_seconds", 0);
        return (ttlMsec > 0) && file.isFile() &&
                System.currentTimeMillis() - file.lastModified() >= ttlMsec;
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock4) {
            while (flushingInProgress || !opsBeingFlushed.isEmpty()) {
                try {
                    lock4.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            flushingInProgress = true;
            final String imagePathname = getImagePathname();
            final String infoPathname = getInfoPathname();
            if (imagePathname != null && infoPathname != null) {
                long imageCount = 0;
                final File imageDir = new File(imagePathname);
                if (imageDir.isDirectory()) {
                    for (File file : imageDir.listFiles()) {
                        if (file.isFile() && file.delete()) {
                            imageCount++;
                        }
                    }
                }
                long infoCount = 0;
                final File infoDir = new File(infoPathname);
                if (infoDir.isDirectory()) {
                    for (File file : infoDir.listFiles()) {
                        if (file.isFile() && file.delete()) {
                            infoCount++;
                        }
                    }
                }
                logger.info("Flushed {} images and {} dimensions", imageCount,
                        infoCount);
            } else {
                throw new IOException("FilesystemCache.pathname is not set");
            }
        } finally {
            flushingInProgress = false;
        }
    }

    @Override
    public void flush(OperationList ops) throws IOException {
        synchronized (lock1) {
            while (flushingInProgress || opsBeingFlushed.contains(ops)) {
                try {
                    lock1.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            opsBeingFlushed.add(ops);
            File imageFile = getCachedImageFile(ops);
            if (imageFile != null && imageFile.exists()) {
                imageFile.delete();
            }
            File dimensionFile = getCachedInfoFile(ops.getIdentifier());
            if (dimensionFile != null && dimensionFile.exists()) {
                dimensionFile.delete();
            }
            logger.info("Flushed {}", ops);
        } finally {
            opsBeingFlushed.remove(ops);
        }
    }

    @Override
    public void flushExpired() throws IOException {
        synchronized (lock4) {
            while (flushingInProgress || !opsBeingFlushed.isEmpty()) {
                try {
                    lock4.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            flushingInProgress = true;
            final String imagePathname = getImagePathname();
            final String infoPathname = getInfoPathname();
            if (imagePathname != null && infoPathname != null) {
                long imageCount = 0;
                final File imageDir = new File(imagePathname);
                if (imageDir.isDirectory()) {
                    for (File file : imageDir.listFiles()) {
                        if (file.isFile() && isExpired(file) && file.delete()) {
                            imageCount++;
                        }
                    }
                }
                long infoCount = 0;
                final File infoDir = new File(infoPathname);
                if (infoDir.isDirectory()) {
                    for (File file : infoDir.listFiles()) {
                        if (file.isFile() && isExpired(file) && file.delete()) {
                            infoCount++;
                        }
                    }
                }
                logger.info("Flushed {} expired images and {} expired dimensions",
                        imageCount, infoCount);
            } else {
                throw new IOException("FilesystemCache.pathname is not set");
            }
        } finally {
            flushingInProgress = false;
        }
    }

    @Override
    public Dimension getDimension(Identifier identifier) throws IOException {
        synchronized (lock2) {
            while (infosBeingWritten.contains(identifier)) {
                try {
                    lock2.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            infosBeingRead.add(identifier);
            File cacheFile = getCachedInfoFile(identifier);
            if (cacheFile != null && cacheFile.exists()) {
                if (!isExpired(cacheFile)) {
                    logger.debug("Hit for dimension: {}", cacheFile.getName());

                    ImageInfo info = infoMapper.readValue(cacheFile,
                            ImageInfo.class);
                    return new Dimension(info.width, info.height);
                } else {
                    logger.debug("Deleting stale cache file: {}",
                            cacheFile.getName());
                    cacheFile.delete();
                }
            }
        } catch (FileNotFoundException e) {
            logger.debug(e.getMessage(), e);
        } finally {
            infosBeingRead.remove(identifier);
        }
        return null;
    }

    @Override
    public InputStream getImageInputStream(OperationList ops) {
        File cacheFile = getCachedImageFile(ops);
        if (cacheFile != null && cacheFile.exists()) {
            if (!isExpired(cacheFile)) {
                try {
                    logger.debug("Hit for image: {}", ops);
                    return new FileInputStream(cacheFile);
                } catch (FileNotFoundException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.debug("Deleting stale cache file: {}",
                        cacheFile.getName());
                cacheFile.delete();
            }
        }
        return null;
    }

    @Override
    public OutputStream getImageOutputStream(OperationList ops)
            throws IOException { // TODO: make this work better concurrently
        logger.debug("Miss; caching {}", ops);
        File cacheFile = getCachedImageFile(ops);
        cacheFile.getParentFile().mkdirs();
        cacheFile.createNewFile();
        return new FileOutputStream(cacheFile);
    }

    /**
     * @param ops Request parameters
     * @return File corresponding to the given parameters, or null if
     * <code>FilesystemCache.pathname</code> is not set in the configuration.
     */
    public File getCachedImageFile(OperationList ops) {
        final String cachePathname = getImagePathname();
        if (cachePathname != null) {
            List<String> parts = new ArrayList<>();
            parts.add(StringUtils.stripEnd(cachePathname, File.separator) +
                    File.separator +
                    ops.getIdentifier().toString().replaceAll(FILENAME_CHARACTERS, "_"));
            for (Operation op : ops) {
                parts.add(op.toString().replaceAll(FILENAME_CHARACTERS, "_"));
            }
            final String baseName = StringUtils.join(parts, "_");
            return new File(baseName + "." +
                    ops.getOutputFormat().getExtension());
        }
        return null;
    }

    /**
     * @param identifier IIIF identifier
     * @return File corresponding to the given parameters, or null if
     * <code>FilesystemCache.pathname</code> is not set in the configuration.
     */
    public File getCachedInfoFile(Identifier identifier) {
        final String cachePathname = getInfoPathname();
        if (cachePathname != null) {
            final String pathname =
                    StringUtils.stripEnd(cachePathname, File.separator) +
                    File.separator +
                    identifier.toString().replaceAll(FILENAME_CHARACTERS, "_") +
                    INFO_EXTENSION;
            return new File(pathname);
        }
        return null;
    }

    @Override
    public void putDimension(Identifier identifier, Dimension dimension)
            throws IOException {
        synchronized (lock3) {
            while (infosBeingWritten.contains(identifier) ||
                    infosBeingRead.contains(identifier)) {
                try {
                    lock3.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            infosBeingWritten.add(identifier);
            final File cacheFile = getCachedInfoFile(identifier);
            if (cacheFile != null) {
                logger.debug("Caching dimension: {}", identifier);
                cacheFile.getParentFile().mkdirs();
                ImageInfo info = new ImageInfo();
                info.width = dimension.width;
                info.height = dimension.height;
                infoMapper.writeValue(cacheFile, info);
            } else {
                throw new IOException("FilesystemCache.pathname is not set");
            }
        } finally {
            infosBeingWritten.remove(identifier);
        }
    }

}
