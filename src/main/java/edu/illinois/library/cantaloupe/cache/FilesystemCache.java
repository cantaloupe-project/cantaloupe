package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.request.Parameters;
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
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Cache using a filesystem folder, storing images and info files separately in
 * subfolders. Configurable by <code>FilesystemCache.pathname</code> and
 * <code>FilesystemCache.ttl_seconds</code> keys in the application
 * configuration.
 */
class FilesystemCache implements Cache {

    private static final Logger logger = LoggerFactory.
            getLogger(FilesystemCache.class);

    // https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations
    private static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";
    private static final String IMAGE_FOLDER = "image";
    private static final String INFO_FOLDER = "info";
    private static final String INFO_EXTENSION = ".json";

    private static final ObjectMapper infoMapper = new ObjectMapper();

    private boolean flushingInProgress = false;

    /** Set of identifiers for which info files are currently being read. */
    private final Set<String> infosBeingRead = new ConcurrentSkipListSet<>();

    /** Set of identifiers for which info files are currently being written. */
    private final Set<String> infosBeingWritten = new ConcurrentSkipListSet<>();

    /** Set of Parameters for which image files are currently being flushed by
     * flush(Parameters). */
    private final Set<Parameters> paramsBeingFlushed =
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

    public void flush() throws IOException {
        synchronized (lock4) {
            while (flushingInProgress || !paramsBeingFlushed.isEmpty()) {
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
                final File imageDir = new File(imagePathname);
                long imageCount = 0;
                for (File file : imageDir.listFiles()) {
                    if (file.isFile() && file.delete()) {
                        imageCount++;
                    }
                }

                final File infoDir = new File(infoPathname);
                long infoCount = 0;
                for (File file : infoDir.listFiles()) {
                    if (file.isFile() && file.delete()) {
                        infoCount++;
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

    public void flush(Parameters params) throws IOException {
        synchronized (lock1) {
            while (flushingInProgress || paramsBeingFlushed.contains(params)) {
                try {
                    lock1.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            paramsBeingFlushed.add(params);
            File imageFile = getCachedImageFile(params);
            if (imageFile != null && imageFile.exists()) {
                imageFile.delete();
            }
            File dimensionFile = getCachedInfoFile(params.getIdentifier());
            if (dimensionFile != null && dimensionFile.exists()) {
                dimensionFile.delete();
            }
            logger.info("Flushed {}", params);
        } finally {
            paramsBeingFlushed.remove(params);
        }
    }

    public void flushExpired() throws IOException {
        synchronized (lock4) {
            while (flushingInProgress || !paramsBeingFlushed.isEmpty()) {
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
                final File imageDir = new File(imagePathname);
                long imageCount = 0;
                for (File file : imageDir.listFiles()) {
                    if (file.isFile() && isExpired(file) && file.delete()) {
                        imageCount++;
                    }
                }

                final File infoDir = new File(infoPathname);
                long infoCount = 0;
                for (File file : infoDir.listFiles()) {
                    if (file.isFile() && isExpired(file) && file.delete()) {
                        infoCount++;
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

    public Dimension getDimension(String identifier) throws IOException {
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
                    return new Dimension(info.getWidth(), info.getHeight());
                } else {
                    logger.debug("Deleting stale cache file: {}",
                            cacheFile.getName());
                    cacheFile.delete();
                }
            }
        } catch (FileNotFoundException e) {
            // noop
        } finally {
            infosBeingRead.remove(identifier);
        }
        return null;
    }

    public InputStream getImageInputStream(Parameters params) {
        File cacheFile = getCachedImageFile(params);
        if (cacheFile != null && cacheFile.exists()) {
            if (!isExpired(cacheFile)) {
                try {
                    logger.debug("Hit for image: {}", params);
                    return new FileInputStream(cacheFile);
                } catch (FileNotFoundException e) {
                    // noop
                }
            } else {
                logger.debug("Deleting stale cache file: {}",
                        cacheFile.getName());
                cacheFile.delete();
            }
        }
        return null;
    }

    public OutputStream getImageOutputStream(Parameters params)
            throws IOException { // TODO: make this work better concurrently
        if (getImageInputStream(params) == null) {
            logger.debug("Miss; caching {}", params);
            File cacheFile = getCachedImageFile(params);
            cacheFile.getParentFile().mkdirs();
            cacheFile.createNewFile();
            return new FileOutputStream(cacheFile);
        }
        return null;
    }

    /**
     * @param params Request parameters
     * @return File corresponding to the given parameters, or null if
     * <code>FilesystemCache.pathname</code> is not set in the configuration.
     */
    public File getCachedImageFile(Parameters params) {
        final String cachePathname = getImagePathname();
        if (cachePathname != null) {
            final String pathname = String.format("%s%s%s_%s_%s_%s_%s.%s",
                    StringUtils.stripEnd(cachePathname, File.separator),
                    File.separator,
                    params.getIdentifier().replaceAll(FILENAME_CHARACTERS, "_"),
                    params.getRegion().toString().replaceAll(FILENAME_CHARACTERS, "_"),
                    params.getSize().toString().replaceAll(FILENAME_CHARACTERS, "_"),
                    params.getRotation().toString().replaceAll(FILENAME_CHARACTERS, "_"),
                    params.getQuality().toString().toLowerCase(),
                    params.getOutputFormat().getExtension());
            return new File(pathname);
        }
        return null;
    }

    /**
     * @param identifier IIIF identifier
     * @return File corresponding to the given parameters, or null if
     * <code>FilesystemCache.pathname</code> is not set in the configuration.
     */
    public File getCachedInfoFile(String identifier) {
        final String cachePathname = getInfoPathname();
        if (cachePathname != null) {
            final String pathname =
                    StringUtils.stripEnd(cachePathname, File.separator) +
                    File.separator +
                    identifier.replaceAll(FILENAME_CHARACTERS, "_") +
                    INFO_EXTENSION;
            return new File(pathname);
        }
        return null;
    }

    public void putDimension(String identifier, Dimension dimension)
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
                info.setWidth(dimension.width);
                info.setHeight(dimension.height);
                infoMapper.writeValue(cacheFile, info);
            } else {
                throw new IOException("FilesystemCache.pathname is not set");
            }
        } finally {
            infosBeingWritten.remove(identifier);
        }
    }

}
