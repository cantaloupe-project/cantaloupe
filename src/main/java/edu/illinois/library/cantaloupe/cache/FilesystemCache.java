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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cache using a filesystem folder, storing images and dimensions separately as
 * files in subfolders.
 */
class FilesystemCache implements Cache {

    /**
     * Returned by {@link FilesystemCache#getImageOutputStream} when an image
     * for a given operation list can be cached.
     */
    private static class ConcurrentFileOutputStream extends FileOutputStream {

        private static final Logger logger = LoggerFactory.
                getLogger(ConcurrentFileOutputStream.class);

        private Set<OperationList> imagesBeingWritten;
        private OperationList opsList;

        /**
         * @param file
         * @param imagesBeingWritten Set of OperationLists for all images
         *                           currently being written.
         * @param opsList
         * @throws FileNotFoundException
         */
        public ConcurrentFileOutputStream(File file,
                                          Set<OperationList> imagesBeingWritten,
                                          OperationList opsList)
                throws FileNotFoundException {
            super(file);
            imagesBeingWritten.add(opsList);
            this.imagesBeingWritten = imagesBeingWritten;
            this.opsList = opsList;
        }

        @Override
        public void close() throws IOException {
            logger.debug("Closing stream for {}", opsList);
            imagesBeingWritten.remove(opsList);
            super.close();
        }

    }

    /**
     * Returned by {@link FilesystemCache#getImageOutputStream} when an
     * output stream for the same operation list has been returned in another
     * thread but has not yet been closed. Allows that thread to keep writing
     * without the current thread interfering.
     */
    private static class ConcurrentNullOutputStream extends OutputStream {

        private Set<OperationList> imagesBeingWritten;
        private OperationList opsList;

        /**
         * @param imagesBeingWritten Set of OperationLists for all images
         *                           currently being written.
         * @param opsList
         */
        public ConcurrentNullOutputStream(Set<OperationList> imagesBeingWritten,
                                          OperationList opsList) {
            this.imagesBeingWritten = imagesBeingWritten;
            this.opsList = opsList;
        }

        @Override
        public void close() throws IOException {
            imagesBeingWritten.remove(opsList);
            super.close();
        }

        @Override
        public void write(int b) throws IOException {
            // noop
        }

    }

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

    /**
     * @see <a href="https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations">
     *     Comparison of filename limitations</a>
     */
    private static final String FILENAME_SAFE_CHARACTERS = "[^A-Za-z0-9._-]";
    private static final String IMAGE_FOLDER = "image";
    private static final String INFO_FOLDER = "info";
    private static final String INFO_EXTENSION = ".json";
    private static final String PATHNAME_CONFIG_KEY = "FilesystemCache.pathname";

    private static final ObjectMapper infoMapper = new ObjectMapper();

    private final AtomicBoolean flushingInProgress = new AtomicBoolean(false);

    /** Set of identifiers for which info files are currently being read. */
    private final Set<Identifier> dimensionsBeingRead =
            new ConcurrentSkipListSet<>();
    /** Set of identifiers for which info files are currently being written. */
    private final Set<Identifier> dimensionsBeingWritten =
            new ConcurrentSkipListSet<>();
    /** Set of Operations for which image files are currently being flushed by
     * flush(OperationList). */
    private final Set<OperationList> imagesBeingFlushed =
            new ConcurrentSkipListSet<>();
    /** Set of operation lists for which image files are currently being
     * written. */
    private final Set<OperationList> imagesBeingWritten =
            new ConcurrentSkipListSet<>();

    /** Lock object for synchronization */
    private final Object lock1 = new Object();
    /** Lock object for synchronization */
    private final Object lock2 = new Object();
    /** Lock object for synchronization */
    private final Object lock3 = new Object();
    /** Lock object for synchronization */
    private final Object lock4 = new Object();

    private static String filenameSafe(Identifier identifier) {
        return identifier.toString().replaceAll(FILENAME_SAFE_CHARACTERS, "_");
    }

    private static String filenameSafe(Operation op) {
        return op.toString().replaceAll(FILENAME_SAFE_CHARACTERS, "_");
    }

    /**
     * @return Pathname of the root cache folder.
     */
    private static String getCachePathname() {
        return Application.getConfiguration().getString(PATHNAME_CONFIG_KEY);
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
            while (flushingInProgress.get() || !imagesBeingFlushed.isEmpty()) {
                try {
                    lock4.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            flushingInProgress.set(true);
            final String imagePathname = getImagePathname();
            final String infoPathname = getInfoPathname();
            if (imagePathname != null && infoPathname != null) {
                long imageCount = 0;
                final File imageDir = new File(imagePathname);
                if (imageDir.isDirectory()) {
                    for (File file : imageDir.listFiles()) {
                        if (file.isFile()) {
                            if (file.delete()) {
                                imageCount++;
                            } else {
                                throw new IOException("Unable to delete " +
                                        file.getAbsolutePath());
                            }
                        }
                    }
                }
                long infoCount = 0;
                final File infoDir = new File(infoPathname);
                if (infoDir.isDirectory()) {
                    for (File file : infoDir.listFiles()) {
                        if (file.isFile()) {
                            if (file.delete()) {
                                infoCount++;
                            } else {
                                throw new IOException("Unable to delete " +
                                        file.getAbsolutePath());
                            }
                        }
                    }
                }
                logger.info("Flushed {} images and {} dimensions", imageCount,
                        infoCount);
            } else {
                throw new IOException(PATHNAME_CONFIG_KEY + " is not set");
            }
        } finally {
            flushingInProgress.set(false);
        }
    }

    @Override
    public void flush(Identifier identifier) throws IOException {
        for (File imageFile : getImageFiles(identifier)) {
            logger.debug("Deleting {}", imageFile);
            if (!imageFile.delete()) {
                throw new IOException("Failed to delete " + imageFile);
            }
        }
        File dimensionFile = getDimensionFile(identifier);
        if (dimensionFile.exists()) {
            logger.debug("Deleting {}", dimensionFile);
            if (!dimensionFile.delete()) {
                throw new IOException("Failed to delete " + dimensionFile);
            }
        }
    }

    @Override
    public void flush(OperationList ops) throws IOException {
        synchronized (lock1) {
            while (flushingInProgress.get() || imagesBeingFlushed.contains(ops)) {
                try {
                    lock1.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            imagesBeingFlushed.add(ops);
            File imageFile = getImageFile(ops);
            if (imageFile != null && imageFile.exists()) {
                if (!imageFile.delete()) {
                    throw new IOException("Unable to delete " + imageFile);
                }
            }
            File dimensionFile = getDimensionFile(ops.getIdentifier());
            if (dimensionFile != null && dimensionFile.exists()) {
                if (!dimensionFile.delete()) {
                    throw new IOException("Unable to delete " + imageFile);
                }
            }
            logger.info("Flushed {}", ops);
        } finally {
            imagesBeingFlushed.remove(ops);
        }
    }

    @Override
    public void flushExpired() throws IOException {
        synchronized (lock4) {
            while (flushingInProgress.get() || !imagesBeingFlushed.isEmpty()) {
                try {
                    lock4.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            flushingInProgress.set(true);
            final String imagePathname = getImagePathname();
            final String infoPathname = getInfoPathname();
            if (imagePathname != null && infoPathname != null) {
                long imageCount = 0;
                final File imageDir = new File(imagePathname);
                if (imageDir.isDirectory()) {
                    for (File file : imageDir.listFiles()) {
                        if (file.isFile() && isExpired(file)) {
                            if (file.delete()) {
                                imageCount++;
                            } else {
                                throw new IOException("Unable to delete " +
                                        file.getAbsolutePath());
                            }
                        }
                    }
                }
                long infoCount = 0;
                final File infoDir = new File(infoPathname);
                if (infoDir.isDirectory()) {
                    for (File file : infoDir.listFiles()) {
                        if (file.isFile() && isExpired(file)) {
                            if (file.delete()) {
                                infoCount++;
                            } else {
                                throw new IOException("Unable to delete " +
                                        file.getAbsolutePath());
                            }
                        }
                    }
                }
                logger.info("Flushed {} expired images and {} expired dimensions",
                        imageCount, infoCount);
            } else {
                throw new IOException(PATHNAME_CONFIG_KEY + " is not set");
            }
        } finally {
            flushingInProgress.set(false);
        }
    }

    @Override
    public Dimension getDimension(Identifier identifier) throws IOException {
        synchronized (lock2) {
            while (dimensionsBeingWritten.contains(identifier)) {
                try {
                    lock2.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            dimensionsBeingRead.add(identifier);
            File cacheFile = getDimensionFile(identifier);
            if (cacheFile != null && cacheFile.exists()) {
                if (!isExpired(cacheFile)) {
                    logger.debug("Hit for dimension: {}", cacheFile.getName());

                    ImageInfo info = infoMapper.readValue(cacheFile,
                            ImageInfo.class);
                    return new Dimension(info.width, info.height);
                } else {
                    logger.debug("Deleting stale cache file: {}",
                            cacheFile.getName());
                    if (!cacheFile.delete()) {
                        // TODO: should this be an IOException?
                        logger.error("Unable to delete {}", cacheFile);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.debug(e.getMessage(), e);
        } finally {
            dimensionsBeingRead.remove(identifier);
        }
        return null;
    }

    /**
     * @param identifier
     * @return File corresponding to the given parameters, or null if
     * <code>FilesystemCache.pathname</code> is not set in the configuration.
     */
    public File getDimensionFile(Identifier identifier) {
        final String cachePathname = getInfoPathname();
        if (cachePathname != null) {
            final String pathname =
                    StringUtils.stripEnd(cachePathname, File.separator) +
                            File.separator + filenameSafe(identifier) +
                            INFO_EXTENSION;
            return new File(pathname);
        }
        return null;
    }

    /**
     * @param ops
     * @return File corresponding to the given operation list, or null if
     * <code>FilesystemCache.pathname</code> is not set in the configuration.
     */
    public File getImageFile(OperationList ops) {
        final String cachePathname = getImagePathname();
        if (cachePathname != null) {
            List<String> parts = new ArrayList<>();
            parts.add(StringUtils.stripEnd(cachePathname, File.separator) +
                    File.separator + filenameSafe(ops.getIdentifier()));
            for (Operation op : ops) {
                if (!op.isNoOp()) {
                    parts.add(filenameSafe(op));
                }
            }
            final String baseName = StringUtils.join(parts, "_");
            return new File(baseName + "." +
                    ops.getOutputFormat().getExtension());
        }
        return null;
    }

    /**
     * @param identifier
     * @return All cached image files deriving from the image with the given
     * identifier.
     */
    public List<File> getImageFiles(Identifier identifier) {
        class IdentifierFilter implements FilenameFilter {
            private Identifier identifier;

            public IdentifierFilter(Identifier identifier) {
                this.identifier = identifier;
            }

            public boolean accept(File dir, String name) {
                return name.startsWith(filenameSafe(identifier));
            }
        }

        final File cachePathname = new File(getImagePathname());
        final File[] files = cachePathname.
                listFiles(new IdentifierFilter(identifier));
        return new ArrayList<>(Arrays.asList(files));
    }

    @Override
    public InputStream getImageInputStream(OperationList ops) {
        File cacheFile = getImageFile(ops);
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
                if (!cacheFile.delete()) {
                    // TODO: should this be an IOException?
                    logger.error("Unable to delete {}", cacheFile);
                }
            }
        }
        return null;
    }

    @Override
    public OutputStream getImageOutputStream(OperationList ops)
            throws IOException {
        if (imagesBeingWritten.contains(ops)) {
            logger.debug("Miss, but cache file for {} is being written in " +
                    "another thread, so not caching", ops);
            return new ConcurrentNullOutputStream(imagesBeingWritten, ops);
        }
        imagesBeingWritten.add(ops); // will be removed by ConcurrentNullOutputStream.close()
        logger.debug("Miss; caching {}", ops);
        File cacheFile = getImageFile(ops);
        if (!cacheFile.getParentFile().exists()) {
            if (!cacheFile.getParentFile().mkdirs() ||
                    !cacheFile.createNewFile()) {
                throw new IOException("Unable to create " + cacheFile);
            }
        }
        return new ConcurrentFileOutputStream(cacheFile, imagesBeingWritten,
                ops);
    }

    @Override
    public void putDimension(Identifier identifier, Dimension dimension)
            throws IOException {
        synchronized (lock3) {
            while (dimensionsBeingWritten.contains(identifier) ||
                    dimensionsBeingRead.contains(identifier)) {
                try {
                    lock3.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            dimensionsBeingWritten.add(identifier);
            final File cacheFile = getDimensionFile(identifier);
            if (cacheFile != null) {
                logger.debug("Caching dimension: {}", identifier);
                if (!cacheFile.getParentFile().exists() &&
                        !cacheFile.getParentFile().mkdirs()) {
                    throw new IOException("Unable to create directory: " +
                            cacheFile.getParentFile());
                }
                ImageInfo info = new ImageInfo();
                info.width = dimension.width;
                info.height = dimension.height;
                try {
                    infoMapper.writeValue(cacheFile, info);
                } catch (IOException e) {
                    throw new IOException("Unable to create " +
                            cacheFile.getAbsolutePath(), e);
                }
            } else {
                throw new IOException(PATHNAME_CONFIG_KEY + " is not set");
            }
        } finally {
            dimensionsBeingWritten.remove(identifier);
        }
    }

}
