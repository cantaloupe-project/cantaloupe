package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.util.DeletingFileVisitor;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * <p>Cache using a filesystem, storing source images, derivative images,
 * and infos in separate top-level subdirectories.</p>
 *
 * <h1>Tree structure</h1>
 *
 * <ul>
 *     <li>{@link Key#FILESYSTEMCACHE_PATHNAME}/
 *         <ul>
 *             <li>source/
 *                 <ul>
 *                     <li>Intermediate subdirectories (see [1])
 *                         <ul>
 *                             <li>{identifier hash (see [2])} (see [3])</li>
 *                         </ul>
 *                     </li>
 *                 </ul>
 *             </li>
 *             <li>image/
 *                 <ul>
 *                     <li>Intermediate subdirectories (see [1])
 *                         <ul>
 *                             <li>{identifier hash (see [2])}{operation list
 *                             string representation}.{derivative format
 *                             extension} (see [3])</li>
 *                         </ul>
 *                     </li>
 *                 </ul>
 *             </li>
 *             <li>info/
 *                 <ul>
 *                     <li>Intermediate subdirectories (see [1])
 *                         <ul>
 *                             <li>{identifier hash (see [2])}.json (see
 *                             [3])</li>
 *                         </ul>
 *                     </li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <ol>
 *     <li>Subdirectories are based on identifier MD5 hash, configurable by
 *     {@link Key#FILESYSTEMCACHE_DIRECTORY_DEPTH} and
 *     {@link Key#FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH}</li>
 *     <li>The hash algorithm is specified by {@link #HASH_ALGORITHM}.</li>
 *     <li>Identifiers in filenames are hashed in order to allow for identifiers
 *     longer than the filesystem's filename length limit.</li>
 *     <li>Cache files are created with a .tmp extension and moved into place
 *     when closed for writing.</li>
 * </ol>
 *
 * <h1>Notes</h1>
 *
 * <ul>
 *     <li>The root directory is configurable via
 *     {@link Key#FILESYSTEMCACHE_PATHNAME}.</li>
 *     <li>All needed subdirectories will be created automatically if they don't
 *     already exist.</li>
 *     <li>Symbolic links are followed.</li>
 *     <li>This implementation is both thread- and process-safe.</li>
 * </ul>
 */
class FilesystemCache implements SourceCache, DerivativeCache {

    /**
     * <p>Writes images to a temp file that will be moved into place when
     * closed.</p>
     *
     * <p>{@link T} may be either an {@link Identifier} corresponding to a
     * source image, or an {@link OperationList} corresponding to a derivative
     * image.</p>
     */
    private static class ConcurrentFileOutputStream<T> extends OutputStream {

        private static final Logger CFOS_LOGGER = LoggerFactory.
                getLogger(ConcurrentFileOutputStream.class);

        private final Path destinationFile;
        private boolean isClosed = false;
        private final Object lock;
        private final Path tempFile;
        private T toRemove;
        private OutputStream wrappedOutputStream;

        /**
         * @param tempFile Pathname of the temp file to write to.
         * @param destinationFile Pathname to move tempFile to when it is done
         *                        being written.
         * @param toRemove Object to remove from the set when done.
         * @param lock Object to perform notification upon closure.
         */
        ConcurrentFileOutputStream(Path tempFile,
                                   Path destinationFile,
                                   T toRemove,
                                   Object lock) throws IOException {
            imagesBeingWritten.add(toRemove);
            this.tempFile = tempFile;
            this.destinationFile = destinationFile;
            this.toRemove = toRemove;
            this.lock = lock;
            this.wrappedOutputStream = Files.newOutputStream(tempFile);
        }

        /**
         * N.B.: This implementation can cope with being called multiple times,
         * which is important. See inline doc in
         * {@link edu.illinois.library.cantaloupe.resource.ImageRepresentation#write}
         * for more info.
         */
        @Override
        public void close() {
            if (!isClosed) {
                isClosed = true;
                try {
                    // Close super.
                    try {
                        super.close();
                    } catch (IOException e) {
                        CFOS_LOGGER.warn("close(): failed to close super: {}",
                                e.getMessage());
                    }

                    // Close the wrapped stream in order to release its handle
                    // on tempFile.
                    try {
                        CFOS_LOGGER.debug("close(): closing stream for {}",
                                toRemove);
                        wrappedOutputStream.close();
                    } catch (IOException e) {
                        CFOS_LOGGER.warn("close(): failed to close the " +
                            "wrapped output stream: {}", e.getMessage());
                    }

                    // If the written file isn't empty, move it into place.
                    // Otherwise, delete it.
                    if (Files.size(tempFile) > 0) {
                        CFOS_LOGGER.debug("close(): moving {} to {}",
                                tempFile, destinationFile);
                        Files.move(tempFile, destinationFile);
                    } else {
                        CFOS_LOGGER.debug("close(): deleting zero-byte file: {}",
                                tempFile);
                        Files.delete(tempFile);
                    }
                } catch (IOException e) {
                    CFOS_LOGGER.warn("close(): {}", e.getMessage(), e);
                } finally {
                    imagesBeingWritten.remove(toRemove);

                    // Release other threads waiting on this image to be
                    // written.
                    synchronized(lock) {
                        lock.notifyAll();
                    }
                }
            }
        }

        @Override
        public void flush() throws IOException {
            wrappedOutputStream.flush();
        }

        @Override
        public void write(int b) throws IOException {
            wrappedOutputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            wrappedOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wrappedOutputStream.write(b, off, len);
        }

    }

    /**
     * Used by {@link Files#walkFileTree} to delete all stale temporary and
     * zero-byte files within a directory.
     */
    private static class DetritalFileVisitor extends SimpleFileVisitor<Path> {

        private static final Logger DFV_LOGGER = LoggerFactory.
                getLogger(DetritalFileVisitor.class);

        private long deletedFileCount = 0;
        private long deletedFileSize = 0;
        private final PathMatcher matcher;
        private final long minCleanableAge;

        DetritalFileVisitor(long minCleanableAge) {
            this.minCleanableAge = minCleanableAge;
            matcher = FileSystems.getDefault().
                    getPathMatcher("glob:*" + TEMP_EXTENSION);
        }

        private void delete(Path path) {
            try {
                final long size = Files.size(path);
                Files.deleteIfExists(path);
                deletedFileCount++;
                deletedFileSize += size;
            } catch (IOException e) {
                DFV_LOGGER.warn(e.getMessage(), e);
            }
        }

        long getDeletedFileCount() {
            return deletedFileCount;
        }

        long getDeletedFileSize() {
            return deletedFileSize;
        }

        private void test(Path path) {
            try {
                // Try to avoid matching temp files that may still be open for
                // writing by assuming that files last modified long enough ago
                // are closed.
                if (System.currentTimeMillis()
                        - Files.getLastModifiedTime(path).toMillis() > minCleanableAge) {
                    // Delete temp files.
                    if (matcher.matches(path.getFileName())) {
                        delete(path);
                    } else {
                        // Delete zero-byte files.
                        if (Files.size(path) == 0) {
                            delete(path);
                        }
                    }
                }
            } catch (IOException e) {
                DFV_LOGGER.error(e.getMessage(), e);
            }
        }

        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attrs) {
            test(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            DFV_LOGGER.warn("visitFileFailed(): {}", e.getMessage());
            return FileVisitResult.CONTINUE;
        }

    }

    /**
     * Used by {@link Files#walkFileTree} to delete all expired files within
     * a directory.
     */
    private static class ExpiredFileVisitor extends SimpleFileVisitor<Path> {

        private static final Logger EFV_LOGGER = LoggerFactory.
                getLogger(ExpiredFileVisitor.class);

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
                if (Files.isRegularFile(path) && isExpired(path)) {
                    final long size = Files.size(path);
                    Files.deleteIfExists(path);
                    deletedFileCount++;
                    deletedFileSize += size;
                }
            } catch (IOException e) {
                EFV_LOGGER.warn(e.getMessage(), e);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            EFV_LOGGER.warn("visitFileFailed(): {}", e.getMessage());
            return FileVisitResult.CONTINUE;
        }

    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(FilesystemCache.class);

    // Algorithm used for hashing identifiers to create filenames & pathnames.
    // Will be passed to MessageDigest.getInstance(). MD5 is chosen for its
    // efficiency. A collision here and there is not a big deal.
    private static final String HASH_ALGORITHM = "MD5";
    private static final String DERIVATIVE_IMAGE_FOLDER = "image";
    private static final String INFO_FOLDER = "info";
    private static final String SOURCE_IMAGE_FOLDER = "source";

    private static final String INFO_EXTENSION = ".json";
    private static final String TEMP_EXTENSION = ".tmp";

    /** Set of {@link Identifier}s or {@link OperationList}s) for which image
     * files are currently being written from any thread. */
    private static final Set<Object> imagesBeingWritten =
            ConcurrentHashMap.newKeySet();

    /** Set of Operations for which image files are currently being purged by
     * purge(OperationList) from any thread. */
    private final Set<OperationList> imagesBeingPurged =
            ConcurrentHashMap.newKeySet();

    /** Set of identifiers for which info files are currently being purged by
     * purge(Identifier) from any thread. */
    private final Set<Identifier> infosBeingPurged =
            ConcurrentHashMap.newKeySet();

    /** Toggled by purge() and purgeInvalid(). */
    private final AtomicBoolean isGlobalPurgeInProgress =
            new AtomicBoolean(false);

    private long minCleanableAge = 1000 * 60 * 10;

    /** Several different lock objects for context-dependent synchronization.
     * Reduces contention for the instance. */
    private final Object derivativeImageWriteLock = new Object();
    private final Object imagePurgeLock = new Object();
    private final Object infoPurgeLock = new Object();
    private final Object sourceImageWriteLock = new Object();

    /** Rather than using a global lock, per-identifier locks allow for
     * simultaneous writes to different infos. Map entries are added on demand
     * and never removed. */
    private final Map<Identifier,ReadWriteLock> infoLocks =
            new ConcurrentHashMap<>();

    /**
     * Returns the last-accessed time of the given file. On some OS/filesystem
     * combinations, this may be unreliable, in which case the last-modified
     * time is returned instead.
     *
     * @param file File to check.
     * @return Last-accessed time of the given file, if available, or the
     *         last-modified time otherwise.
     * @throws NoSuchFileException If the given file does not exist.
     * @throws IOException If there is some other error.
     */
    private static FileTime getLastAccessedTime(Path file) throws IOException {
        try {
            // Last-accessed time is not reliable on macOS+APFS as of 10.13.2.
            // TODO: what about HFS+?
            if (SystemUtils.IS_OS_MAC) {
                LOGGER.debug("macOS detected; using last-modified time " +
                        "instead of last-accessed time.");
                return Files.getLastModifiedTime(file);
            }
            return (FileTime) Files.getAttribute(file, "lastAccessTime");
        } catch (UnsupportedOperationException e) {
            LOGGER.error("getLastAccessedTime(): {}", e.getMessage(), e);
            return Files.getLastModifiedTime(file);
        }
    }

    /**
     * @param uniqueString String from which to derive the path.
     * @return Directory path composed of fragments of a hash of the given
     *         string.
     */
    static String hashedPathFragment(String uniqueString) {
        final List<String> components = new ArrayList<>();
        try {
            final MessageDigest digest =
                    MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(uniqueString.getBytes(Charset.forName("UTF8")));
            final String sum = Hex.encodeHexString(digest.digest());

            final Configuration config = Configuration.getInstance();
            final int depth = config.getInt(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 3);
            final int nameLength =
                    config.getInt(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH, 2);

            for (int i = 0; i < depth; i++) {
                final int offset = i * nameLength;
                components.add(sum.substring(offset, offset + nameLength));
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return String.join("/", components);
    }

    /**
     * @param file Path to check.
     * @return Whether the given file is expired based on
     *         {@link Key#CACHE_SERVER_TTL} and its last-accessed time. If
     *         {@link Key#CACHE_SERVER_TTL} is 0, <code>false</code> will be
     *         returned.
     */
    private static boolean isExpired(Path file) throws IOException {
        final long ttlMsec = 1000 * Configuration.getInstance().
                getLong(Key.CACHE_SERVER_TTL, 0);
        final long age = System.currentTimeMillis()
                - getLastAccessedTime(file).toMillis();
        LOGGER.debug("Age of {}: {} msec", file.getFileName(), age);
        return (ttlMsec > 0 && age > ttlMsec);
    }

    /**
     * @return Path of the root cache directory.
     */
    private static Path rootPath() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME, "");
        if (pathname.isEmpty()) {
            LOGGER.error("{} is not set.", Key.FILESYSTEMCACHE_PATHNAME);
        }
        return Paths.get(pathname);
    }

    /**
     * @return Path of the derivative image cache folder, or
     *         <code>null</code> if {@link Key#FILESYSTEMCACHE_PATHNAME} is
     *         not set.
     */
    static Path rootDerivativeImagePath() {
        return rootPath().resolve(DERIVATIVE_IMAGE_FOLDER);
    }

    /**
     * @return Path of the image info cache folder, or <code>null</code> if
     *         {@link Key#FILESYSTEMCACHE_PATHNAME} is not set.
     */
    static Path rootInfoPath() {
        return rootPath().resolve(INFO_FOLDER);
    }

    /**
     * @return Path of the source image cache folder, or <code>null</code> if
     *         {@link Key#FILESYSTEMCACHE_PATHNAME} is not set.
     */
    static Path rootSourceImagePath() {
        return rootPath().resolve(SOURCE_IMAGE_FOLDER);
    }

    /**
     * @param ops Operation list identifying the file.
     * @return Path corresponding to the given operation list.
     */
    static Path derivativeImageFile(OperationList ops) {
        return rootDerivativeImagePath()
                .resolve(hashedPathFragment(ops.getIdentifier().toString()))
                .resolve(ops.toFilename());
    }

    /**
     * @param ops Operation list identifying the file.
     * @return Temp file corresponding to the given operation list. Clients
     *         should delete it when they are done with it.
     */
    static Path derivativeImageTempFile(OperationList ops) {
        return rootDerivativeImagePath()
                .resolve(hashedPathFragment(ops.getIdentifier().toString()))
                .resolve(ops.toFilename() + tempFileSuffix());
    }

    /**
     * @param identifier
     * @return Path of an info file corresponding to the image with the given
     *         identifier.
     */
    static Path infoFile(final Identifier identifier) {
        return rootInfoPath()
                .resolve(hashedPathFragment(identifier.toString()))
                .resolve(StringUtil.filesystemSafe(identifier.toString())
                        + INFO_EXTENSION);
    }

    /**
     * @param identifier
     * @return Temporary info file corresponding to the image with the given
     *         identifier.
     */
    static Path infoTempFile(final Identifier identifier) {
        return rootInfoPath()
                .resolve(hashedPathFragment(identifier.toString()))
                .resolve(StringUtil.filesystemSafe(identifier.toString())
                        + INFO_EXTENSION + tempFileSuffix());
    }

    /**
     * @param identifier Identifier identifying the file.
     * @return Path corresponding to the given identifier.
     */
    static Path sourceImageFile(Identifier identifier) {
        return rootSourceImagePath()
                .resolve(hashedPathFragment(identifier.toString()))
                .resolve(StringUtil.filesystemSafe(identifier.toString()));
    }

    /**
     * @param identifier Identifier identifying the file.
     * @return Temp file corresponding to a source image with the given
     *         identifier. Clients should delete it when they are done with it.
     */
    static Path sourceImageTempFile(Identifier identifier) {
        return rootSourceImagePath()
                .resolve(hashedPathFragment(identifier.toString()))
                .resolve(StringUtil.filesystemSafe(identifier.toString())
                        + tempFileSuffix());
    }

    static String tempFileSuffix() {
        return "_" + Thread.currentThread().getName() + TEMP_EXTENSION;
    }

    private ReadWriteLock acquireInfoLock(final Identifier identifier) {
        ReadWriteLock lock = infoLocks.get(identifier);
        if (lock == null) {
            infoLocks.putIfAbsent(identifier, new ReentrantReadWriteLock());
            lock = infoLocks.get(identifier);
        }
        return lock;
    }

    /**
     * Cleans up temporary and zero-byte files.
     */
    @Override
    public void cleanUp() throws IOException {
        final Path path = rootPath();

        LOGGER.info("cleanUp(): cleaning directory: {}", path);
        DetritalFileVisitor visitor = new DetritalFileVisitor(minCleanableAge);

        Files.walkFileTree(path,
                EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE,
                visitor);
        LOGGER.info("cleanUp(): cleaned {} item(s) totaling {} bytes",
                visitor.getDeletedFileCount(),
                visitor.getDeletedFileSize());
    }

    /**
     * @param identifier
     * @return All cached image files deriving from the image with the given
     *         identifier.
     */
    Set<Path> getDerivativeImageFiles(Identifier identifier)
            throws IOException {
        final Path cachePath = rootDerivativeImagePath().resolve(
                hashedPathFragment(identifier.toString()));
        final String expectedNamePrefix =
                StringUtil.filesystemSafe(identifier.toString());
        return Files.list(cachePath)
                .filter(p -> p.getFileName().toString().startsWith(expectedNamePrefix))
                .collect(Collectors.toSet());
    }

    @Override
    public Info getImageInfo(Identifier identifier) throws IOException {
        final ReadWriteLock lock = acquireInfoLock(identifier);
        lock.readLock().lock();

        try {
            final Path cacheFile = infoFile(identifier);
            if (!isExpired(cacheFile)) {
                LOGGER.info("getImageInfo(): hit: {}", cacheFile);
                return Info.fromJSON(cacheFile.toFile());
            } else {
                purgeAsync(cacheFile);
            }
        } catch (NoSuchFileException | FileNotFoundException e) {
            LOGGER.info("getImageInfo(): not found: {}", e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    @Override
    public Path getSourceImageFile(Identifier identifier) throws IOException {
        synchronized (sourceImageWriteLock) {
            while (imagesBeingWritten.contains(identifier)) {
                try {
                    LOGGER.debug("getSourceImageFile(): waiting on {}...",
                            identifier);
                    sourceImageWriteLock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        Path file = null;
        final Path cacheFile = sourceImageFile(identifier);

        if (Files.exists(cacheFile)) {
            if (!isExpired(cacheFile)) {
                LOGGER.info("getSourceImageFile(): hit: {} ({})",
                        identifier, cacheFile);
                file = cacheFile;
            } else {
                purgeAsync(cacheFile);
            }
        }
        return file;
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList ops)
            throws IOException {
        InputStream inputStream = null;
        final Path cacheFile = derivativeImageFile(ops);

        if (Files.exists(cacheFile)) {
            if (!isExpired(cacheFile)) {
                try {
                    LOGGER.info("newDerivativeImageInputStream(): " +
                                    "hit: {} ({})", ops, cacheFile);
                    inputStream = Files.newInputStream(cacheFile);
                } catch (NoSuchFileException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } else {
                purgeAsync(cacheFile);
            }
        }
        return inputStream;
    }

    /**
     * @param ops Operation list representing the image to write to.
     * @return An output stream to write to. The stream will write to a
     *         temporary file and then move it into place when closed. It may
     *         also write to nothing if an output stream for the same operation
     *         list has been returned to another thread but not yet closed.
     * @throws IOException If anything goes wrong.
     */
    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList ops)
            throws IOException {
        return newOutputStream(ops, derivativeImageTempFile(ops),
                derivativeImageFile(ops), derivativeImageWriteLock);
    }

    /**
     * @param identifier Identifier representing the image to write to.
     * @return An output stream to write to. The stream will generally write to
     *         a temp file and then move it into place when closed. It may also
     *         write to nothing if an output stream for the same operation list
     *         has been returned to another thread but not yet closed.
     * @throws IOException If anything goes wrong.
     */
    @Override
    public OutputStream newSourceImageOutputStream(Identifier identifier)
            throws IOException {
        return newOutputStream(identifier, sourceImageTempFile(identifier),
                sourceImageFile(identifier), sourceImageWriteLock);
    }

    /**
     * @param imageIdentifier {@link Identifier} or {@link OperationList}
     * @param tempFile Temporary file to write to.
     * @param destFile Destination file that tempFile will be moved to when
     *                 writing is complete.
     * @param notifyObj Object to perform notification upon stream closure.
     * @return Output stream for writing.
     * @throws IOException IF anything goes wrong.
     */
    private OutputStream newOutputStream(Object imageIdentifier,
                                         Path tempFile,
                                         Path destFile,
                                         Object notifyObj) throws IOException {
        // If the image is being written in another thread, it may (or may not)
        // be present in the imagesBeingWritten set. If so, return a null
        // output stream to avoid interfering.
        if (imagesBeingWritten.contains(imageIdentifier)) {
            LOGGER.info("newOutputStream(): miss, but cache file for {} is " +
                    "being written in another thread, so returning a {}",
                    imageIdentifier, NullOutputStream.class.getSimpleName());
            return new NullOutputStream();
        }

        LOGGER.info("newOutputStream(): miss; caching {}", imageIdentifier);

        try {
            // Create the containing directory. This may throw a
            // FileAlreadyExistsException for concurrent invocations with the
            // same argument.
            Files.createDirectories(tempFile.getParent());

            return new ConcurrentFileOutputStream<>(tempFile, destFile,
                    imageIdentifier, notifyObj);
        } catch (FileAlreadyExistsException e) {
            // The image either already exists in its complete form, or is
            // being written by another thread/process. Either way, there is no
            // need to write over it.
            LOGGER.debug("newOutputStream(): {} already exists; returning a {}",
                    tempFile.getParent(),
                    NullOutputStream.class.getSimpleName());
            return new NullOutputStream();
        }
    }

    /**
     * <p>Crawls the cache directory, deleting all files (but not folders)
     * within it (including temp files).</p>
     *
     * <p>Will do nothing and return immediately if a global purge is in
     * progress in another thread.</p>
     */
    @Override
    public void purge() throws IOException {
        if (isGlobalPurgeInProgress.get()) {
            LOGGER.info("purge() called with a purge already in progress. " +
                    "Aborting.");
            return;
        }
        synchronized (imagePurgeLock) {
            while (!imagesBeingPurged.isEmpty()) {
                try {
                    LOGGER.debug("purge(): waiting...");
                    imagePurgeLock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        try {
            isGlobalPurgeInProgress.set(true);

            final Path path = rootPath();

            DeletingFileVisitor visitor = new DeletingFileVisitor();
            visitor.setRootPathToExclude(path);
            visitor.setLogger(LOGGER);

            LOGGER.info("purge(): purging...");
            Files.walkFileTree(path,
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    visitor);
            LOGGER.info("purge(): purged {} item(s) totaling {} bytes",
                    visitor.getDeletedFileCount(),
                    visitor.getDeletedFileSize());
        } finally {
            isGlobalPurgeInProgress.set(false);
            synchronized (imagePurgeLock) {
                imagePurgeLock.notifyAll();
            }
        }
    }

    /**
     * <p>Deletes all files associated with the given identifier.</p>
     *
     * <p>Will do nothing and return immediately if a global purge is in
     * progress in another thread.</p>
     */
    @Override
    public void purge(Identifier identifier) throws IOException {
        if (isGlobalPurgeInProgress.get()) {
            LOGGER.info("purge(Identifier) called with a global purge in " +
                    "progress. Aborting.");
            return;
        }
        synchronized (infoPurgeLock) {
            while (infosBeingPurged.contains(identifier)) {
                try {
                    LOGGER.debug("purge(Identifier): waiting on {}...",
                            identifier);
                    infoPurgeLock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        try {
            infosBeingPurged.add(identifier);
            LOGGER.info("purge(Identifier): purging {}...", identifier);

            // Delete the source image.
            final Path sourceFile = sourceImageFile(identifier);
            try {
                LOGGER.info("purge(Identifier): deleting {}", sourceFile);
                Files.deleteIfExists(sourceFile);
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
            // Delete the info.
            final Path infoFile = infoFile(identifier);
            try {
                LOGGER.info("purge(Identifier): deleting {}", infoFile);
                Files.deleteIfExists(infoFile);
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
            // Delete derivative images.
            for (Path imageFile : getDerivativeImageFiles(identifier)) {
                try {
                    LOGGER.info("purge(Identifier): deleting {}", imageFile);
                    Files.deleteIfExists(imageFile);
                } catch (IOException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        } finally {
            infosBeingPurged.remove(identifier);
        }
    }

    /**
     * <p>Deletes all derivative image files associated with the given
     * operation list.</p>
     *
     * <p>If purging is in progress in another thread, this method will wait
     * for it to finish before proceeding.</p>
     *
     * <p>Will do nothing and return immediately if a global purge is in
     * progress in another thread.</p>
     */
    @Override
    public void purge(OperationList opList) {
        if (isGlobalPurgeInProgress.get()) {
            LOGGER.info("purge(OperationList) called with a global purge in " +
                    "progress. Aborting.");
            return;
        }
        synchronized (imagePurgeLock) {
            while (imagesBeingPurged.contains(opList)) {
                try {
                    LOGGER.debug("purge(OperationList): waiting on {}...",
                            opList);
                    imagePurgeLock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        try {
            imagesBeingPurged.add(opList);
            LOGGER.info("purge(OperationList): purging {}...", opList);

            Path file = derivativeImageFile(opList);
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                LOGGER.warn("purge(OperationList(): unable to delete {}",
                        file);
            }
        } finally {
            imagesBeingPurged.remove(opList);
            synchronized (imagePurgeLock) {
                imagePurgeLock.notifyAll();
            }
        }
    }

    private void purgeAsync(final Path path) {
        ThreadPool.getInstance().submit(() -> {
            LOGGER.debug("purgeAsync(): deleting stale file: {}", path);
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LOGGER.warn("purgeAsync(): unable to delete {}", path);
            }
        });
    }

    /**
     * <p>Crawls the image directory, deleting all expired files within it
     * (temporary or not), and then does the same in the info directory.</p>
     *
     * <p>Will do nothing and return immediately if a global purge is in
     * progress in another thread.</p>
     */
    @Override
    public void purgeInvalid() throws IOException {
        if (isGlobalPurgeInProgress.get()) {
            LOGGER.info("purgeInvalid() called with a purge in progress. " +
                    "Aborting.");
            return;
        }
        synchronized (imagePurgeLock) {
            while (!imagesBeingPurged.isEmpty()) {
                try {
                    LOGGER.debug("purgeInvalid(): waiting...");
                    imagePurgeLock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            isGlobalPurgeInProgress.set(true);

            final ExpiredFileVisitor visitor = new ExpiredFileVisitor();

            LOGGER.info("purgeInvalid(): purging...");
            Files.walkFileTree(rootPath(),
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    visitor);
            LOGGER.info("purgeInvalid(): purged {} item(s) totaling {} bytes",
                    visitor.getDeletedFileCount(),
                    visitor.getDeletedFileSize());
        } finally {
            isGlobalPurgeInProgress.set(false);
            synchronized (imagePurgeLock) {
                imagePurgeLock.notifyAll();
            }
        }
    }

    @Override
    public void put(Identifier identifier, Info info) throws IOException {
        final ReadWriteLock lock = acquireInfoLock(identifier);

        lock.writeLock().lock();

        final Path destFile = infoFile(identifier);
        final Path tempFile = infoTempFile(identifier);

        try {
            LOGGER.info("put(): writing {} to {}", identifier, tempFile);

            try {
                // Create the containing directory.
                Files.createDirectories(tempFile.getParent());
            } catch (FileAlreadyExistsException e) {
                // When this method runs concurrently with an equal Identifier
                // argument, all of the other invocations will throw this,
                // which is fine.
                LOGGER.debug("put(): failed to create directory: {}",
                        e.getMessage());
            }

            try (OutputStream os = Files.newOutputStream(tempFile)) {
                info.writeAsJSON(os);
            }

            LOGGER.debug("put(): moving {} to {}", tempFile, destFile);
            Files.move(tempFile, destFile);
        } catch (FileAlreadyExistsException e) {
            // When this method runs concurrently with an equal Identifier
            // argument, all of the other invocations of Files.move() will
            // throw this, which is fine.
            LOGGER.debug("put(): file already exists: {}", e.getMessage());
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e2) {
                // Swallow this because the outer exception is more important.
                LOGGER.error("put(): failed to delete file: {}",
                        e2.getMessage());
            }
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets the age threshold for cleaning files. Cleanable files last
     * modified less than this many milliseconds ago will not be subject to
     * cleanup.
     *
     * @param age Age in milliseconds.
     */
    void setMinCleanableAge(long age) {
        minCleanableAge = age;
    }

}
