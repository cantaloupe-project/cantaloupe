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

/**
 * Simple cache using a filesystem folder. Configurable by
 * <code>FilesystemResolver.pathname</code> and
 * <code>FilesystemResolver.ttl_seconds</code> keys in the application
 * configuration.
 */
class FilesystemCache implements Cache {

    private static final String INFO_EXTENSION = ".json";

    private static ObjectMapper infoMapper = new ObjectMapper();
    private static Logger logger = LoggerFactory.getLogger(FilesystemCache.class);

    /**
     * @return Pathname of the image cache folder, or null if
     * <code>FilesystemCache.pathname</code> is not set.
     */
    private static String getImagePathname() {
        final String pathname = getPathname();
        if (pathname != null) {
            return pathname + File.separator + "image";
        }
        return null;
    }

    /**
     * @return Pathname of the info cache folder, or null if
     * <code>FilesystemCache.pathname</code> is not set.
     */
    private static String getInfoPathname() {
        final String pathname = getPathname();
        if (pathname != null) {
            return pathname + File.separator + "info";
        }
        return null;
    }

    /**
     * @return Pathname of the root cache folder, or null if
     * <code>FilesystemCache.pathname</code> is not set.
     */
    private static String getPathname() {
        return Application.getConfiguration().
                getString("FilesystemCache.pathname");
    }

    private static long getTtlMsec() {
        return 1000 * Application.getConfiguration().
                getLong("FilesystemCache.ttl_seconds", 0);
    }

    public void flush() throws IOException {
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
    }

    public void flush(Parameters params) throws IOException {
        File imageFile = getCachedImageFile(params);
        if (imageFile != null && imageFile.exists()) {
            imageFile.delete();
        }
        File dimensionFile = getCachedInfoFile(params.getIdentifier());
        if (dimensionFile != null && dimensionFile.exists()) {
            dimensionFile.delete();
        }
        logger.info("Flushed {}", params);
    }

    public void flushExpired() throws IOException {
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
    }

    public Dimension getDimension(String identifier) throws IOException {
        File cacheFile = getCachedInfoFile(identifier);
        if (cacheFile != null && cacheFile.exists()) {
            if (!isExpired(cacheFile)) {
                try {
                    logger.debug("Hit for dimension: {}", cacheFile.getName());

                    ImageInfo info = infoMapper.readValue(cacheFile,
                            ImageInfo.class);
                    return new Dimension(info.getWidth(), info.getHeight());
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
            throws IOException {
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
            final String search = "[^A-Za-z0-9._-]";
            final String replacement = "";
            final String pathname = String.format("%s%s%s_%s_%s_%s_%s.%s",
                    StringUtils.stripEnd(cachePathname, File.separator), File.separator,
                    params.getIdentifier().replaceAll(search, replacement),
                    params.getRegion().toString().replaceAll(search, replacement),
                    params.getSize().toString().replaceAll(search, replacement),
                    params.getRotation().toString().replaceAll(search, replacement),
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
            final String search = "[^A-Za-z0-9._-]";
            final String replacement = "";
            final String pathname = StringUtils.stripEnd(cachePathname, File.separator) +
                    File.separator + identifier.replaceAll(search, replacement) +
                    INFO_EXTENSION;
            return new File(pathname);
        }
        return null;
    }

    public void putDimension(String identifier, Dimension dimension)
            throws IOException {
        logger.debug("Caching dimension: {}", identifier);
        final File cacheFile = getCachedInfoFile(identifier);
        cacheFile.getParentFile().mkdirs();
        ImageInfo info = new ImageInfo();
        info.setWidth(dimension.width);
        info.setHeight(dimension.height);
        infoMapper.writeValue(cacheFile, info);
    }

    private boolean isExpired(File file) {
        long ttlMsec = getTtlMsec();
        return (ttlMsec > 0) && file.isFile() &&
                System.currentTimeMillis() - file.lastModified() >= ttlMsec;
    }

}
