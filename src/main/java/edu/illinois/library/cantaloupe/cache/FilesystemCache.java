package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.util.DeletingFileVisitor;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 *     <li>Cache files are created with a {@literal .tmp} extension and moved
 *     into place when closed for writing.</li>
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
    private static class ConcurrentFileOutputStream<T>
            extends CompletableOutputStream {

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

                    // If the written file is complete, move it into place.
                    // Otherwise, delete it.
                    if (isComplete()) {
                        CFOS_LOGGER.debug("close(): moving {} to {}",
                                tempFile, destinationFile);
                        Files.move(tempFile, destinationFile,
                                StandardCopyOption.REPLACE_EXISTING);
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

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FilesystemCache.class);

    // Algorithm used for hashing identifiers to create filenames & pathnames.
    // Will be passed to MessageDigest.getInstance(). MD5 is chosen for its
    // efficiency. A collision here and there is not a big deal.
    private static final String HASH_ALGORITHM = "MD5";
    private static final String DERIVATIVE_IMAGE_FOLDER = "image";
    private static final String INFO_FOLDER = "info";
    private static final String SOURCE_IMAGE_FOLDER = "source";

    static final String INFO_EXTENSION = ".json";
    private static final String TEMP_EXTENSION = ".tmp";

    /**
     * Set of {@link Identifier}s or {@link OperationList}s) for which image
     * files are currently being written from any thread.
     */
    private static final Set<Object> imagesBeingWritten =
            ConcurrentHashMap.newKeySet();

    /**
     * Set of {@link OperationList}s for which image files are currently being
     * purged by {@link #purge(OperationList)} from any thread.
     */
    private final Set<OperationList> imagesBeingPurged =
            ConcurrentHashMap.newKeySet();

    /**
     * Set of identifiers for which info files are currently being purged by
     * {@link #purge(Identifier)} from any thread.
     */
    private final Set<Identifier> infosBeingPurged =
            ConcurrentHashMap.newKeySet();

    /**
     * Toggled by {@link #purge()} and {@link #purgeInvalid()}.
     */
    private final AtomicBoolean isGlobalPurgeInProgress =
            new AtomicBoolean(false);

    private long minCleanableAge = 1000 * 60 * 10;

    /**
     * Several different lock objects for context-dependent synchronization.
     * Reduces contention for the instance.
     */
    private final Object derivativeImageWriteLock = new Object();
    private final Object imagePurgeLock = new Object();
    private final Object infoPurgeLock = new Object();
    private final Object sourceImageWriteLock = new Object();

    /**
     * Rather than using a global lock, per-identifier locks allow for
     * simultaneous writes to different infos. Map entries are added on demand
     * and never removed.
     */
    private final Map<Identifier,ReadWriteLock> infoLocks =
            new ConcurrentHashMap<>();

    /**
     * Returns the last-accessed time of the given file. On some OS/filesystem
     * combinations, this may be unreliable or not available, in which case the
     * last-modified time is returned instead.
     *
     * @param file File to check.
     * @return Last-accessed time of the given file, if available, or the
     *         last-modified time otherwise.
     * @throws NoSuchFileException if the given file does not exist.
     * @throws IOException if there is some other error.
     */
    static FileTime getLastAccessedTime(Path file) throws IOException {
        try {
            // Last-accessed time is not reliable on macOS+APFS 10.13.2.
            if (SystemUtils.IS_OS_MAC) {
                LOGGER.trace("macOS detected; using file last-modified " +
                        "instead of last-accessed times.");
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
        return String.join(File.separator, components);
    }

    /**
     * Determines whether the given file is expired by comparing the result of
     * {@link #getLastAccessedTime(Path)} to either
     * {@link Key#DERIVATIVE_CACHE_TTL} or {@link Key#SOURCE_CACHE_TTL},
     * depending on the given path. If the selected key is set to {@literal 0},
     * {@literal false} will be returned.
     *
     * @param file Path to check.
     * @return Whether the given file is expired.
     */
    static boolean isExpired(Path file) throws IOException {
        final Configuration config = Configuration.getInstance();

        final long ttlSec = file.startsWith(rootSourceImagePath()) ?
                config.getLong(Key.SOURCE_CACHE_TTL, 0) :
                config.getLong(Key.DERIVATIVE_CACHE_TTL, 0);
        final long ttlMsec = 1000 * ttlSec;
        final long fileAge = System.currentTimeMillis()
                - getLastAccessedTime(file).toMillis();
        final boolean expired = (ttlMsec > 0 && fileAge > ttlMsec);

        LOGGER.trace("isExpired(): {}: TTL: {}; last accessed: {}; expired? {}",
                file, ttlSec, fileAge, expired);
        return expired;
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
     * @return Path of an info file corresponding to the image with the given
     *         identifier.
     */
    static Path infoFile(final Identifier identifier) {
        return rootInfoPath()
                .resolve(hashedPathFragment(identifier.toString()))
                .resolve(StringUtils.md5(identifier.toString())
                        + INFO_EXTENSION);
    }

    /**
     * @return Temporary info file corresponding to the image with the given
     *         identifier.
     */
    static Path infoTempFile(final Identifier identifier) {
        return rootInfoPath()
                .resolve(hashedPathFragment(identifier.toString()))
                .resolve(StringUtils.md5(identifier.toString())
                        + INFO_EXTENSION + tempFileSuffix());
    }

    /**
     * @param identifier Identifier identifying the file.
     * @return Path corresponding to the given identifier.
     */
    static Path sourceImageFile(Identifier identifier) {
        return rootSourceImagePath()
                .resolve(hashedPathFragment(identifier.toString()))
                .resolve(StringUtils.md5(identifier.toString()));
    }

    /**
     * @param identifier Identifier identifying the file.
     * @return Temp file corresponding to a source image with the given
     *         identifier. Clients should delete it when they are done with it.
     */
    static Path sourceImageTempFile(Identifier identifier) {
        return rootSourceImagePath()
                .resolve(hashedPathFragment(identifier.toString()))
                .resolve(StringUtils.md5(identifier.toString())
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
     * Deletes temporary and zero-byte files.
     *
     * @see DetritalFileVisitor
     */
    @Override
    public void cleanUp() throws IOException {
        final Path path = rootPath();

        LOGGER.debug("cleanUp(): cleaning directory: {}", path);
        DetritalFileVisitor visitor =
                new DetritalFileVisitor(minCleanableAge, TEMP_EXTENSION);

        Files.walkFileTree(path,
                EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE,
                visitor);
        LOGGER.debug("cleanUp(): cleaned {} item(s) totaling {} bytes",
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
                StringUtils.md5(identifier.toString());
        try {
            return Files.list(cachePath)
                    .filter(p -> p.getFileName().toString().startsWith(expectedNamePrefix))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (NoSuchFileException e) {
            LOGGER.debug("getDerivativeImageFiles(): no such file: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    @Override
    public Optional<Info> getInfo(Identifier identifier) throws IOException {
        final ReadWriteLock lock = acquireInfoLock(identifier);
        lock.readLock().lock();
        try {
            final Path cacheFile = infoFile(identifier);
            if (!isExpired(cacheFile)) {
                LOGGER.debug("getInfo(): hit: {}", cacheFile);
                return Optional.of(Info.fromJSON(cacheFile));
            } else {
                purgeAsync(cacheFile);
            }
        } catch (NoSuchFileException | FileNotFoundException e) {
            LOGGER.debug("getInfo(): not found: {}", e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Path> getSourceImageFile(Identifier identifier)
            throws IOException {
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

        final Path cacheFile = sourceImageFile(identifier);

        try {
            if (!isExpired(cacheFile)) {
                LOGGER.debug("getSourceImageFile(): hit: {} ({})",
                        identifier, cacheFile);
                return Optional.of(cacheFile);
            } else {
                purgeAsync(cacheFile);
            }
        } catch (NoSuchFileException e) {
            LOGGER.debug("getSourceImageFile(): {} ", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList ops)
            throws IOException {
        InputStream inputStream = null;
        final Path cacheFile = derivativeImageFile(ops);

        try {
            if (!isExpired(cacheFile)) {
                try {
                    LOGGER.debug("newDerivativeImageInputStream(): hit: {} ({})",
                            ops, cacheFile);
                    inputStream = Files.newInputStream(cacheFile);
                } catch (NoSuchFileException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } else {
                purgeAsync(cacheFile);
            }
        } catch (NoSuchFileException e) {
            LOGGER.debug("newDerivativeImageInputStream(): {} ",
                    e.getMessage());
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
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList ops) throws IOException {
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
        CompletableOutputStream os = newOutputStream(
                identifier, sourceImageTempFile(identifier),
                sourceImageFile(identifier), sourceImageWriteLock);
        // ConcurrentFileOutputStream is a CompletableOutputStream in order to
        // work with newDerivativeImageOutputStream(). But this method does not
        // need that extra functionality, so setting it as completely written
        // here makes it behave like an ordinary OutputStream.
        os.complete();
        return os;
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
    private CompletableOutputStream newOutputStream(Object imageIdentifier,
                                                    Path tempFile,
                                                    Path destFile,
                                                    Object notifyObj) throws IOException {
        // If the image is being written in another thread, it may (or may not)
        // be present in the imagesBeingWritten set. If so, return a null
        // output stream to avoid interfering.
        if (imagesBeingWritten.contains(imageIdentifier)) {
            LOGGER.debug("newOutputStream(): miss, but cache file for {} is " +
                    "being written in another thread, so returning a no-op stream",
                    imageIdentifier);
            return new CompletableNullOutputStream();
        }

        LOGGER.debug("newOutputStream(): miss; caching {}", imageIdentifier);

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
            LOGGER.debug("newOutputStream(): {} already exists; returning a no-op stream",
                    tempFile.getParent());
            return new CompletableNullOutputStream();
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
            LOGGER.debug("purge() called with a purge already in progress. " +
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

            LOGGER.debug("purge(): starting...");
            Files.walkFileTree(path,
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    visitor);
            LOGGER.debug("purge(): purged {} item(s) totaling {} bytes",
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
            LOGGER.debug("purge(Identifier) called with a global purge in " +
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
            LOGGER.debug("purge(Identifier): purging {}...", identifier);

            // Delete the source image.
            final Path sourceFile = sourceImageFile(identifier);
            try {
                LOGGER.debug("purge(Identifier): deleting {}", sourceFile);
                Files.deleteIfExists(sourceFile);
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
            // Delete the info.
            final Path infoFile = infoFile(identifier);
            try {
                LOGGER.debug("purge(Identifier): deleting {}", infoFile);
                Files.deleteIfExists(infoFile);
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
            // Delete derivative images.
            for (Path imageFile : getDerivativeImageFiles(identifier)) {
                try {
                    LOGGER.debug("purge(Identifier): deleting {}", imageFile);
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
            LOGGER.debug("purge(OperationList) called with a global purge in " +
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
            LOGGER.debug("purge(OperationList): purging {}...", opList);

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
        TaskQueue.getInstance().submit(() -> {
            LOGGER.debug("purgeAsync(): deleting stale file: {}", path);
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LOGGER.warn("purgeAsync(): unable to delete {}", path);
            }
        });
    }

    @Override
    public void purgeInfos() throws IOException {
        if (isGlobalPurgeInProgress.get()) {
            LOGGER.debug("purgeInfos() called with a purge in progress. Aborting.");
            return;
        }
        synchronized (infoPurgeLock) {
            while (!infosBeingPurged.isEmpty()) {
                try {
                    LOGGER.debug("purgeInfos(): waiting...");
                    infoPurgeLock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        try {
            isGlobalPurgeInProgress.set(true);

            final InfoFileVisitor visitor = new InfoFileVisitor();

            LOGGER.debug("purgeInfos(): starting...");
            Files.walkFileTree(rootPath(),
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    visitor);
            LOGGER.debug("purgeInfos(): purged {} info(s) totaling {} bytes",
                    visitor.getDeletedFileCount(),
                    visitor.getDeletedFileSize());
        } finally {
            isGlobalPurgeInProgress.set(false);
            synchronized (infoPurgeLock) {
                infoPurgeLock.notifyAll();
            }
        }
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
            LOGGER.debug("purgeInvalid() called with a purge in progress. Aborting.");
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

            LOGGER.debug("purgeInvalid(): starting...");
            Files.walkFileTree(rootPath(),
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    visitor);
            LOGGER.debug("purgeInvalid(): purged {} item(s) totaling {} bytes",
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
        if (!info.isPersistable()) {
            LOGGER.debug("put(): info for {} is incomplete; ignoring",
                    identifier);
            return;
        }
        final Path destFile      = infoFile(identifier);
        final Path tempFile      = infoTempFile(identifier);
        final ReadWriteLock lock = acquireInfoLock(identifier);
        lock.writeLock().lock();
        try {
            LOGGER.debug("put(): writing {} to {}", identifier, tempFile);
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
            Files.move(tempFile, destFile, StandardCopyOption.REPLACE_EXISTING);
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

    @Override
    public void put(Identifier identifier, String info) throws IOException {
        final Path destFile      = infoFile(identifier);
        final Path tempFile      = infoTempFile(identifier);
        final ReadWriteLock lock = acquireInfoLock(identifier);
        lock.writeLock().lock();
        try {
            LOGGER.debug("put(): writing {} to {}", identifier, tempFile);
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

            Files.writeString(tempFile, info);

            LOGGER.debug("put(): moving {} to {}", tempFile, destFile);
            Files.move(tempFile, destFile, StandardCopyOption.REPLACE_EXISTING);
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
