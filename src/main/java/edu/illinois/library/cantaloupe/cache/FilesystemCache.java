package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cache using a filesystem folder, storing images and infos separately
 * as files in subfolders.
 */
class FilesystemCache implements Cache {

    /**
     * Used by {@link Files#walkFileTree} to delete all temp files within a
     * directory tree.
     */
    private static class CacheCleaner extends SimpleFileVisitor<Path> {

        private static final Logger logger = LoggerFactory.
                getLogger(CacheCleaner.class);

        // Any file more than 10 minutes old is fair game for deletion.
        private static final long MIN_AGE_MSEC = 1000 * 60 * 10;

        private final PathMatcher matcher;
        private int numDeleted = 0;

        public CacheCleaner() {
            matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:*" + TEMP_EXTENSION);
        }

        private void done() {
            logger.info("Cleaned {} temp files.", numDeleted);
        }

        private void test(Path path) {
            final Path name = path.getFileName();
            final File file = path.toFile();
            // Since we have not overridden preVisitDirectory(), "file" will
            // always be a file.
            if (name != null && matcher.matches(name)) {
                // Try to avoid matching temp files that may still be open for
                // writing by assuming that files last modified long enough ago
                // are closed.
                if (System.currentTimeMillis() - file.lastModified() > MIN_AGE_MSEC) {
                    try {
                        FileUtils.forceDelete(file);
                        numDeleted++;
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        }

        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attrs) {
            test(file);
            return java.nio.file.FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            logger.warn(e.getMessage());
            return java.nio.file.FileVisitResult.CONTINUE;
        }
    }

    /**
     * Returned by
     * {@link FilesystemCache#getImageOutputStream(OperationList)} when an
     * image for a given operation list can be cached. Points to a temp file
     * that will be moved into place when closed.
     */
    private static class ConcurrentFileOutputStream extends FileOutputStream {

        private static final Logger logger = LoggerFactory.
                getLogger(ConcurrentFileOutputStream.class);

        private File destinationFile;
        private Set<OperationList> imagesBeingWritten;
        private OperationList opsList;
        private File tempFile;

        /**
         * @param tempFile Pathname of the temp file to write to.
         * @param destinationFile Pathname to move tempFile to when it is done
         *                        being written.
         * @param imagesBeingWritten Set of OperationLists for all images
         *                           currently being written.
         * @param opsList
         * @throws FileNotFoundException
         */
        public ConcurrentFileOutputStream(File tempFile,
                                          File destinationFile,
                                          Set<OperationList> imagesBeingWritten,
                                          OperationList opsList)
                throws FileNotFoundException {
            super(tempFile);
            imagesBeingWritten.add(opsList);
            this.tempFile = tempFile;
            this.destinationFile = destinationFile;
            this.imagesBeingWritten = imagesBeingWritten;
            this.opsList = opsList;
        }

        @Override
        public void close() throws IOException {
            try {
                logger.debug("Moving {} to {}",
                        tempFile, destinationFile.getName());
                // tempFile may not actually exist, but let's avoid calling
                // File.exists() in the interest of performance.
                FileUtils.moveFile(tempFile, destinationFile);
            } catch (IOException e) {
                logger.debug("Failed to move {} to {}; this is probably not an issue.",
                        tempFile, destinationFile);
            } finally {
                logger.debug("Closing stream for {}", opsList);
                imagesBeingWritten.remove(opsList);
                super.close();
            }
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
    private static final String TEMP_EXTENSION = ".tmp";

    /** Serializes ImageInfo instances to JSON. */
    private static final ObjectMapper infoMapper = new ObjectMapper();

    /** Set of Operations for which image files are currently being purged by
     * purge(OperationList) from any thread. */
    private final Set<OperationList> imagesBeingPurged =
            new ConcurrentSkipListSet<>();

    /** Set of operation lists for which image files are currently being
     * written from any thread. */
    private final Set<OperationList> imagesBeingWritten =
            new ConcurrentSkipListSet<>();

    /** Set of identifiers for which info files are currently being read in
     * any thread. */
    private final Set<Identifier> infosBeingRead =
            new ConcurrentSkipListSet<>();

    /** Set of identifiers for which info files are currently being written
     * from any thread. */
    private final Set<Identifier> infosBeingWritten =
            new ConcurrentSkipListSet<>();

    private final AtomicBoolean cleaningInProgress = new AtomicBoolean(false);
    private final AtomicBoolean purgingInProgress = new AtomicBoolean(false);

    /** Lock objects for synchronization */
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private final Object lock3 = new Object();
    private final Object lock4 = new Object();
    private final Object lock5 = new Object();

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
        return getRootPathname() + File.separator + IMAGE_FOLDER;
    }

    /**
     * @return Pathname of the info cache folder, or null if
     * {@link #PATHNAME_CONFIG_KEY} is not set.
     * @throws CacheException
     */
    private static String getRootInfoPathname() throws CacheException {
        return getRootPathname() + File.separator + INFO_FOLDER;
    }

    private static boolean isExpired(File file) {
        final long ttlMsec = 1000 * Application.getConfiguration().
                getLong(TTL_CONFIG_KEY, 0);
        return ttlMsec > 0 && file.isFile() &&
                System.currentTimeMillis() - file.lastModified() > ttlMsec;
    }

    /**
     * Cleans up temp files.
     *
     * @throws CacheException
     */
    @Override
    public void cleanUp() throws CacheException {
        synchronized (lock5) {
            while (cleaningInProgress.get()) {
                try {
                    lock5.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        try {
            cleaningInProgress.set(true);

            final String[] pathnamesToClean = { getRootImagePathname(),
                    getRootInfoPathname() };

            for (String pathname : pathnamesToClean) {
                logger.info("Cleaning directory: {}", pathname);
                CacheCleaner cleaner = new CacheCleaner();
                Files.walkFileTree(Paths.get(pathname), cleaner);
                cleaner.done();
            }
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        } finally {
            cleaningInProgress.set(false);
        }
    }

    @Override
    public ImageInfo getImageInfo(Identifier identifier) throws CacheException {
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
                    logger.info("Hit for image info: {}", cacheFile.getName());
                    return ImageInfo.fromJson(cacheFile);
                } else {
                    logger.info("Deleting stale cache file: {}",
                            cacheFile.getName());
                    if (!cacheFile.delete()) {
                        logger.warn("Unable to delete {}", cacheFile);
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
     * @param uniqueString String from which to derive the path.
     * @return Directory path composed of fragments of a hash of the given
     * string.
     */
    public String getHashedStringBasedSubdirectory(final String uniqueString) {
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
     * @return File corresponding to the given operation list.
     */
    public File getImageFile(OperationList ops) throws CacheException {
        final List<String> parts = new ArrayList<>();
        final String cacheRoot =
                StringUtils.stripEnd(getRootImagePathname(), File.separator);
        final String subfolderPath = StringUtils.stripEnd(
                getHashedStringBasedSubdirectory(ops.getIdentifier().toString()),
                File.separator);
        final String identifierFilename =
                filenameSafe(ops.getIdentifier().toString());
        parts.add(cacheRoot + subfolderPath + File.separator +
                identifierFilename);
        for (Operation op : ops) {
            if (!op.isNoOp()) {
                parts.add(filenameSafe(op.toString()));
            }
        }
        final String baseName = StringUtils.join(parts, "_");

        return new File(baseName + "." +
                ops.getOutputFormat().getPreferredExtension());
    }

    /**
     * @param identifier
     * @return All cached image files deriving from the image with the given
     * identifier.
     * @throws CacheException
     */
    public Collection<File> getImageFiles(Identifier identifier)
            throws CacheException {
        class IdentifierFilter implements FilenameFilter {
            private Identifier identifier;

            public IdentifierFilter(Identifier identifier) {
                this.identifier = identifier;
            }

            public boolean accept(File dir, String name) {
                // TODO: when the identifier is "cats", "catsup" will also match
                return name.startsWith(filenameSafe(identifier.toString()));
            }
        }

        final File cacheFolder = new File(getRootImagePathname() +
                getHashedStringBasedSubdirectory(identifier.toString()));
        final File[] files =
                cacheFolder.listFiles(new IdentifierFilter(identifier));
        return new ArrayList<>(Arrays.asList(files));
    }

    @Override
    public InputStream getImageInputStream(OperationList ops)
            throws CacheException {
        InputStream inputStream = null;
        final File cacheFile = getImageFile(ops);
        if (cacheFile != null && cacheFile.exists()) {
            if (!isExpired(cacheFile)) {
                try {
                    logger.info("Hit for image: {}", ops);
                    inputStream = new FileInputStream(cacheFile);
                } catch (FileNotFoundException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.info("Deleting stale cache file: {}",
                        cacheFile.getName());
                if (!cacheFile.delete()) {
                    logger.warn("Unable to delete {}", cacheFile);
                }
            }
        }
        return inputStream;
    }

    @Override
    public OutputStream getImageOutputStream(OperationList ops)
            throws CacheException {
        // If the image is already being written in another thread, it will
        // exist in the imagesBeingWritten set. If so, return a dummy output
        // stream to avoid interfering.
        if (imagesBeingWritten.contains(ops)) {
            logger.info("Miss, but cache file for {} is being written in " +
                    "another thread, so not caching", ops);
            return new ConcurrentNullOutputStream(imagesBeingWritten, ops);
        }

        imagesBeingWritten.add(ops);

        // If the image is being written simultaneously in another process,
        // there will be a temp file on the filesystem. If so, return a dummy
        // output stream to avoid interfering.
        final File tempFile = getImageTempFile(ops);
        if (tempFile.exists()) {
            logger.info("Miss, but a temp file for {} already exists, " +
                    "so not caching", ops);
            return new ConcurrentNullOutputStream(imagesBeingWritten, ops);
        }

        logger.info("Miss; caching {}", ops);

        try {
            if (!tempFile.getParentFile().exists()) {
                if (!tempFile.getParentFile().mkdirs() ||
                        !tempFile.createNewFile()) {
                    throw new CacheException("Unable to create " + tempFile);
                }
            }
            final File destFile = getImageFile(ops);
            return new ConcurrentFileOutputStream(
                    tempFile, destFile, imagesBeingWritten, ops);
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    /**
     * Returns a temp file corresponding to the given operation list.
     *
     * @param ops Operation list identifying the file.
     * @return File corresponding to the given operation list.
     */
    public File getImageTempFile(OperationList ops) throws CacheException {
        File tempFile = new File(getImageFile(ops).getAbsolutePath() +
                TEMP_EXTENSION);
        tempFile.deleteOnExit();
        return tempFile;
    }

    /**
     * @param identifier
     * @return File corresponding to the given parameters.
     */
    public File getInfoFile(final Identifier identifier) throws CacheException {
        final String cacheRoot =
                StringUtils.stripEnd(getRootInfoPathname(), File.separator);
        final String subfolderPath = StringUtils.stripEnd(
                getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator);
        final String identifierFilename = filenameSafe(identifier.toString());
        final String pathname = cacheRoot + subfolderPath + File.separator +
                identifierFilename + INFO_EXTENSION;
        return new File(pathname);
    }

    public File getInfoTempFile(final Identifier identifier)
            throws CacheException {
        return new File(getInfoFile(identifier).getAbsolutePath() +
                TEMP_EXTENSION);
    }

    /**
     * <p>Crawls the image directory, deleting all files (but not folders)
     * within it (including temp files), and then does the same in the info
     * directory.</p>
     *
     * <p>If purging is in progress in another thread, this method will wait
     * for it to finish before proceeding.</p>
     *
     * @throws CacheException
     */
    @Override
    public void purge() throws CacheException {
        synchronized (lock4) {
            while (purgingInProgress.get() || !imagesBeingPurged.isEmpty()) {
                try {
                    lock4.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        purgingInProgress.set(true);

        String[] pathnamesToPurge = { getRootImagePathname(),
                getRootInfoPathname() };
        for (String pathname : pathnamesToPurge) {
            try {
                logger.info("Purging image dir: {}", pathname);
                FileUtils.cleanDirectory(new File(pathname));
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
            try {
                logger.info("Purging info dir: {}", pathname);
                FileUtils.cleanDirectory(new File(pathname));
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
        }
        purgingInProgress.set(false);
    }

    /**
     * <p>Deletes all files associated with the given identifier.</p>
     *
     * @throws CacheException
     */
    @Override
    public void purge(Identifier identifier) throws CacheException {
        // TODO: improve concurrency
        for (File imageFile : getImageFiles(identifier)) {
            try {
                logger.info("Deleting {}", imageFile);
                FileUtils.forceDelete(imageFile);
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
        }
        final File infoFile = getInfoFile(identifier);
        if (infoFile.exists()) {
            try {
                logger.info("Deleting {}", infoFile);
                FileUtils.forceDelete(infoFile);
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
        }
    }

    /**
     * <p>Deletes all files associated with the given operation list.</p>
     *
     * <p>If purging is in progress in another thread, this method will wait
     * for it to finish before proceeding.</p>
     *
     * @throws CacheException
     */
    @Override
    public void purge(OperationList opList) throws CacheException {
        synchronized (lock1) {
            while (purgingInProgress.get() || imagesBeingPurged.contains(opList)) {
                try {
                    lock1.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        imagesBeingPurged.add(opList);
        logger.info("Purging {}...", opList);

        final File[] filesToDelete = {
                getImageFile(opList),
                getImageTempFile(opList),
                getInfoFile(opList.getIdentifier()),
                getInfoTempFile(opList.getIdentifier()) };
        for (File file : filesToDelete) {
            if (file != null && file.exists()) {
                try {
                    FileUtils.forceDelete(file);
                } catch (IOException e) {
                    logger.warn("Unable to delete {}", file);
                }
            }
        }
        imagesBeingPurged.remove(opList);
    }

    /**
     * <p>Crawls the image directory, deleting all expired files within it
     * (temporary or not), and then does the same in the info directory.</p>
     *
     * <p>If purging is in progress in another thread, this method will wait
     * for it to finish before proceeding.</p>
     *
     * @throws CacheException
     */
    @Override
    public void purgeExpired() throws CacheException {
        synchronized (lock4) {
            while (purgingInProgress.get() || !imagesBeingPurged.isEmpty()) {
                try {
                    lock4.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        logger.info("Purging expired items...");

        purgingInProgress.set(true);
        final String imagePathname = getRootImagePathname();
        final String infoPathname = getRootInfoPathname();

        long imageCount = 0;
        final File imageDir = new File(imagePathname);
        Iterator<File> it = FileUtils.iterateFiles(imageDir, null, true);
        while (it.hasNext()) {
            File file = it.next();
            if (isExpired(file)) {
                try {
                    FileUtils.forceDelete(file);
                    imageCount++;
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                }
            }
        }

        long infoCount = 0;
        final File infoDir = new File(infoPathname);
        it = FileUtils.iterateFiles(infoDir, null, true);
        while (it.hasNext()) {
            File file = it.next();
            if (isExpired(file)) {
                try {
                    FileUtils.forceDelete(file);
                    infoCount++;
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                }
            }
        }
        purgingInProgress.set(false);
        logger.info("Purged {} expired image(s) and {} expired infos(s)",
                imageCount, infoCount);
    }

    @Override
    public void putImageInfo(Identifier identifier, ImageInfo imageInfo)
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
            final File tempFile = getInfoTempFile(identifier);

            logger.info("Caching image info: {}", identifier);
            if (!tempFile.getParentFile().exists() &&
                    !tempFile.getParentFile().mkdirs()) {
                throw new IOException("Unable to create directory: " +
                        tempFile.getParentFile());
            }

            try {
                infoMapper.writeValue(tempFile, imageInfo);
            } catch (IOException e) {
                tempFile.delete();
                throw new IOException("Unable to create " +
                        tempFile.getAbsolutePath(), e);
            }

            final File destFile = getInfoFile(identifier);
            logger.debug("Moving {} to {}", tempFile, destFile.getName());
            FileUtils.moveFile(tempFile, destFile);
        } catch (IOException e) {
            throw new CacheException(e.getMessage(), e);
        } finally {
            infosBeingWritten.remove(identifier);
        }
    }

}
