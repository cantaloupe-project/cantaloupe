package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
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
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cache using a filesystem folder, storing images and dimensions separately
 * as files in subfolders.
 */
class FilesystemCache implements Cache {

    /**
     * Returned by
     * {@link FilesystemCache#getImageOutputStream(OperationList)} when an
     * image for a given operation list can be cached.
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
     * No-op dummy stream returned by
     * {@link FilesystemCache#getImageOutputStream(OperationList)} when an
     * output stream for the same operation list has been returned in another
     * thread but has not yet been closed. Enables that thread to keep writing
     * without other threads interfering.
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
     * image dimension information in plain text files.
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

    public static final String DIRECTORY_DEPTH_CONFIG_KEY =
            "FilesystemCache.dir.depth";
    public static final String DIRECTORY_NAME_LENGTH_CONFIG_KEY =
            "FilesystemCache.dir.name_length";
    public static final String PATHNAME_CONFIG_KEY = "FilesystemCache.pathname";
    public static final String TTL_CONFIG_KEY = "FilesystemCache.ttl_seconds";

    private static final short FILENAME_MAX_LENGTH = 255;
    // https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations
    private static final Pattern FILENAME_SAFE_PATTERN =
            Pattern.compile("[^A-Za-z0-9_\\-]");
    private static final String IMAGE_FOLDER = "image";
    private static final String INFO_FOLDER = "info";
    private static final String INFO_EXTENSION = ".json";

    // serializes ImageInfo instances to JSON
    private static final ObjectMapper infoMapper = new ObjectMapper();

    /** Set of Operations for which image files are currently being purged by
     * purge(OperationList). */
    private final Set<OperationList> imagesBeingPurged =
            new ConcurrentSkipListSet<>();
    /** Set of operation lists for which image files are currently being
     * written. */
    private final Set<OperationList> imagesBeingWritten =
            new ConcurrentSkipListSet<>();
    /** Set of identifiers for which info files are currently being read. */
    private final Set<Identifier> infosBeingRead =
            new ConcurrentSkipListSet<>();
    /** Set of identifiers for which info files are currently being written. */
    private final Set<Identifier> infosBeingWritten =
            new ConcurrentSkipListSet<>();

    private final AtomicBoolean purgingInProgress = new AtomicBoolean(false);

    /** Lock objects for synchronization */
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private final Object lock3 = new Object();
    private final Object lock4 = new Object();

    /**
     * Returns a reversible, filename-safe version of the input string.
     * Use {@link java.net.URLDecoder#decode} to reverse.
     *
     * @param inputString String to make filename-safe
     * @return Filename-safe string
     */
    public static String filenameSafe(String inputString) {
        final StringBuffer sb = new StringBuffer();
        final Matcher matcher = FILENAME_SAFE_PATTERN.matcher(inputString);

        while (matcher.find()) {
            final String replacement = "%" +
                    Integer.toHexString(matcher.group().charAt(0)).toUpperCase();
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        final String encoded = sb.toString();
        final int end = Math.min(encoded.length(), FILENAME_MAX_LENGTH);
        return encoded.substring(0, end);
    }

    /**
     * @return Pathname of the root cache folder.
     * @throws CacheException if {@link #PATHNAME_CONFIG_KEY} is undefined.
     */
    private static String getRootPathname() throws CacheException {
        final String pathname = Application.getConfiguration().
                getString(PATHNAME_CONFIG_KEY);
        if (pathname == null) {
            throw new CacheException(PATHNAME_CONFIG_KEY + " is undefined.");
        }
        return pathname;
    }

    /**
     * @return Pathname of the image cache folder, or null if
     * {@link #PATHNAME_CONFIG_KEY} is not set.
     * @throws CacheException
     */
    private static String getRootImagePathname() throws CacheException {
        final String pathname = getRootPathname();
        if (pathname != null) {
            return pathname + File.separator + IMAGE_FOLDER;
        }
        return null;
    }

    /**
     * @return Pathname of the info cache folder, or null if
     * {@link #PATHNAME_CONFIG_KEY} is not set.
     * @throws CacheException
     */
    private static String getRootInfoPathname() throws CacheException {
        final String pathname = getRootPathname();
        if (pathname != null) {
            return pathname + File.separator + INFO_FOLDER;
        }
        return null;
    }

    private static boolean isExpired(File file) {
        final long ttlMsec = 1000 * Application.getConfiguration().
                getLong(TTL_CONFIG_KEY, 0);
        return (ttlMsec > 0) && file.isFile() &&
                System.currentTimeMillis() - file.lastModified() > ttlMsec;
    }

    /**
     * Does nothing, as this cache should always be "clean."
     *
     * @throws CacheException
     */
    @Override
    public void cleanUp() {
        logger.info("Cleaning up...");
    }

    @Override
    public Dimension getDimension(Identifier identifier) throws CacheException {
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
            final File cacheFile = getInfoFile(identifier);
            if (cacheFile != null && cacheFile.exists()) {
                if (!isExpired(cacheFile)) {
                    logger.info("Hit for info: {}", cacheFile.getName());

                    ImageInfo info = infoMapper.readValue(cacheFile,
                            ImageInfo.class);
                    return new Dimension(info.width, info.height);
                } else {
                    logger.info("Deleting stale cache file: {}",
                            cacheFile.getName());
                    if (!cacheFile.delete()) {
                        logger.error("Unable to delete {}", cacheFile);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.info(e.getMessage(), e);
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        } finally {
            infosBeingRead.remove(identifier);
        }
        return null;
    }

    /**
     * @param identifier
     * @return File corresponding to the given parameters.
     */
    public File getInfoFile(final Identifier identifier) throws CacheException {
        final String cachePathname = getRootInfoPathname();
        if (cachePathname != null) {
            final String cacheRoot =
                    StringUtils.stripEnd(cachePathname, File.separator);
            final String subfolderPath = StringUtils.stripEnd(
                    getIdentifierBasedSubdirectory(identifier.toString()),
                    File.separator);
            final String identifierFilename =
                    filenameSafe(identifier.toString());
            final String pathname = cacheRoot + subfolderPath +
                            File.separator + identifierFilename +
                            INFO_EXTENSION;
            return new File(pathname);
        }
        return null;
    }

    /**
     * @param uniqueString String from which to derive the path.
     * @return Directory path composed of fragments of a hash of the given
     * string.
     */
    public String getIdentifierBasedSubdirectory(final String uniqueString) {
        final StringBuilder path = new StringBuilder();
        try {
            // Use a fast algo. Collisions don't matter.
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(uniqueString.getBytes(Charset.forName("UTF8")));
            final String sum = Hex.encodeHexString(digest.digest());

            final int depth = Application.getConfiguration().
                    getInt(DIRECTORY_DEPTH_CONFIG_KEY, 3);
            final int nameLength = Application.getConfiguration().
                    getInt(DIRECTORY_NAME_LENGTH_CONFIG_KEY, 2);

            for (int i = 0; i < depth; i++) {
                final int offset = i * nameLength;
                path.append(File.separator);
                path.append(sum.substring(offset, offset + nameLength));
            }
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
        }
        return path.toString();
    }

    /**
     * Returns a File corresponding to the given operation list.
     *
     * @param ops Operation list identifying the file.
     * @return File corresponding to the given operation list, or null if
     * {@link #PATHNAME_CONFIG_KEY} is not set.
     */
    public File getImageFile(OperationList ops) throws CacheException {
        final String cachePathname = getRootImagePathname();
        if (cachePathname != null) {
            final List<String> parts = new ArrayList<>();
            final String cacheRoot =
                    StringUtils.stripEnd(cachePathname, File.separator);
            final String subfolderPath = StringUtils.stripEnd(
                    getIdentifierBasedSubdirectory(ops.getIdentifier().toString()),
                    File.separator);
            final String identifierFilename =
                    filenameSafe(ops.getIdentifier().toString());
            parts.add(cacheRoot + subfolderPath +
                    File.separator + identifierFilename);
            for (Operation op : ops) {
                if (!op.isNoOp()) {
                    parts.add(filenameSafe(op.toString()));
                }
            }
            final String baseName = StringUtils.join(parts, "_");

            return new File(baseName + "." +
                    ops.getOutputFormat().getPreferredExtension());
        }
        return null;
    }

    /**
     * @param identifier
     * @return All cached image files deriving from the image with the given
     * identifier.
     * @throws CacheException
     */
    public List<File> getImageFiles(Identifier identifier)
            throws CacheException {
        class IdentifierFilter implements FilenameFilter {
            private Identifier identifier;

            public IdentifierFilter(Identifier identifier) {
                this.identifier = identifier;
            }

            public boolean accept(File dir, String name) {
                return name.startsWith(filenameSafe(identifier.toString()));
            }
        }

        final File cacheFolder = new File(getRootImagePathname() +
                getIdentifierBasedSubdirectory(identifier.toString()));
        final File[] files =
                cacheFolder.listFiles(new IdentifierFilter(identifier));
        return new ArrayList<>(Arrays.asList(files));
    }

    @Override
    public InputStream getImageInputStream(OperationList ops) {
        try {
            final File cacheFile = getImageFile(ops);
            if (cacheFile != null && cacheFile.exists()) {
                if (!isExpired(cacheFile)) {
                    try {
                        logger.info("Hit for image: {}", ops);
                        return new FileInputStream(cacheFile);
                    } catch (FileNotFoundException e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    logger.info("Deleting stale cache file: {}",
                            cacheFile.getName());
                    if (!cacheFile.delete()) {
                        logger.error("Unable to delete {}", cacheFile);
                    }
                }
            }
        } catch (CacheException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public OutputStream getImageOutputStream(OperationList ops)
            throws CacheException {
        if (imagesBeingWritten.contains(ops)) {
            logger.info("Miss, but cache file for {} is being written in " +
                    "another thread, so not caching", ops);
            return new ConcurrentNullOutputStream(imagesBeingWritten, ops);
        }
        imagesBeingWritten.add(ops); // will be removed by ConcurrentNullOutputStream.close()
        logger.info("Miss; caching {}", ops);
        final File cacheFile = getImageFile(ops);
        try {
            if (!cacheFile.getParentFile().exists()) {
                if (!cacheFile.getParentFile().mkdirs() ||
                        !cacheFile.createNewFile()) {
                    throw new CacheException("Unable to create " + cacheFile);
                }
            }
            return new ConcurrentFileOutputStream(
                    cacheFile, imagesBeingWritten, ops);
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purge() throws CacheException {
        synchronized (lock4) {
            while (purgingInProgress.get() || !imagesBeingPurged.isEmpty() ||
                    !imagesBeingWritten.isEmpty()) {
                try {
                    lock4.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        try {
            purgingInProgress.set(true);
            final String imagePathname = getRootImagePathname();
            final String infoPathname = getRootInfoPathname();
            if (imagePathname != null && infoPathname != null) {
                logger.info("Purging image dir: {}", imagePathname);
                final File imageDir = new File(imagePathname);
                FileUtils.deleteDirectory(imageDir);

                logger.info("Purging info dir: {}", infoPathname);
                final File infoDir = new File(infoPathname);
                FileUtils.deleteDirectory(infoDir);
            } else {
                throw new IOException(PATHNAME_CONFIG_KEY + " is not set");
            }
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        } finally {
            purgingInProgress.set(false);
        }
    }

    @Override
    public void purge(Identifier identifier) throws CacheException {
        // TODO: improve concurrency
        for (File imageFile : getImageFiles(identifier)) {
            logger.info("Deleting {}", imageFile);
            if (!imageFile.delete()) {
                throw new CacheException("Failed to delete " + imageFile);
            }
        }
        final File infoFile = getInfoFile(identifier);
        if (infoFile.exists()) {
            logger.info("Deleting {}", infoFile);
            if (!infoFile.delete()) {
                throw new CacheException("Failed to delete " + infoFile);
            }
        }
    }

    @Override
    public void purge(OperationList opList) throws CacheException {
        synchronized (lock1) {
            // imagesBeingWritten may also contain this opList (meaning it is
            // currently being written in another thread), but the delete
            // we're going to do won't interrupt that.
            while (purgingInProgress.get() || imagesBeingPurged.contains(opList)) {
                try {
                    lock1.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        imagesBeingPurged.add(opList);
        logger.info("Purging {}", opList);
        try {
            final File imageFile = getImageFile(opList);
            if (imageFile != null && imageFile.exists()) {
                if (!imageFile.delete()) {
                    throw new IOException("Unable to delete " + imageFile);
                }
            }
            final File infoFile = getInfoFile(opList.getIdentifier());
            if (infoFile != null && infoFile.exists()) {
                if (!infoFile.delete()) {
                    throw new IOException("Unable to delete " + imageFile);
                }
            }
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        } finally {
            imagesBeingPurged.remove(opList);
        }
    }

    @Override
    public void purgeExpired() throws CacheException {
        synchronized (lock4) {
            while (purgingInProgress.get() || !imagesBeingPurged.isEmpty() ||
                    !imagesBeingWritten.isEmpty()) {
                try {
                    lock4.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        try {
            purgingInProgress.set(true);
            final String imagePathname = getRootImagePathname();
            final String infoPathname = getRootInfoPathname();
            if (imagePathname != null && infoPathname != null) {
                long imageCount = 0;
                final File imageDir = new File(imagePathname);
                Iterator<File> it = FileUtils.iterateFiles(imageDir, null, true);
                while (it.hasNext()) {
                    File file = it.next();
                    if (isExpired(file)) {
                        if (file.delete()) {
                            imageCount++;
                        } else {
                            throw new IOException("Unable to delete " +
                                    file.getAbsolutePath());
                        }
                    }
                }

                long infoCount = 0;
                final File infoDir = new File(infoPathname);
                it = FileUtils.iterateFiles(infoDir, null, true);
                while (it.hasNext()) {
                    File file = it.next();
                    if (isExpired(file)) {
                        if (file.delete()) {
                            infoCount++;
                        } else {
                            throw new IOException("Unable to delete " +
                                    file.getAbsolutePath());
                        }
                    }
                }
                logger.info("Purged {} expired image(s) and {} expired dimension(s)",
                        imageCount, infoCount);
            } else {
                throw new IOException(PATHNAME_CONFIG_KEY + " is not set");
            }
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        } finally {
            purgingInProgress.set(false);
        }
    }

    @Override
    public void putDimension(Identifier identifier, Dimension dimension)
            throws CacheException {
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
            final File cacheFile = getInfoFile(identifier);
            if (cacheFile != null) {
                logger.info("Caching dimension: {}", identifier);
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
                    cacheFile.delete();
                    throw new IOException("Unable to create " +
                            cacheFile.getAbsolutePath(), e);
                }
            } else {
                throw new IOException(PATHNAME_CONFIG_KEY + " is not set");
            }
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        } finally {
            infosBeingWritten.remove(identifier);
        }
    }

}
