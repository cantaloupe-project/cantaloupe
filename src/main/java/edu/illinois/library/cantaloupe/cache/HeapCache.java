package edu.illinois.library.cantaloupe.cache;

import com.google.protobuf.ByteString;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static edu.illinois.library.cantaloupe.config.Key.*;

/**
 * <p>Heap-based LRU cache.</p>
 *
 * <p>This implementation is size-limited rather than time-limited. When the
 * target size
 * ({@link edu.illinois.library.cantaloupe.config.Key#HEAPCACHE_TARGET_SIZE})
 * has been exceeded, the minimum number of least-recently-accessed items are
 * purged that will reduce it back down to this size. (The configured target
 * size may be safely changed while the application is running.)</p>
 *
 * <p>Because this cache is not time-limited,
 * {@link edu.illinois.library.cantaloupe.config.Key#DERIVATIVE_CACHE_TTL} does not
 * apply.</p>
 *
 * <p>The cache supports startup/shutdown persistence, using
 * {@link edu.illinois.library.cantaloupe.config.Key#HEAPCACHE_PERSIST}. When
 * enabled, its contents will be serialized to a file on application shutdown,
 * and read back in at startup. The file is coded using
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol
 * Buffers</a>.</p>
 *
 * @see <a href="https://github.com/google/protobuf">Protocol Buffers</a>
 * @see <a href="https://developers.google.com/protocol-buffers/docs/javatutorial">
 *     Protocol Buffer Basics: Java</a>
 * @since 3.4
 */
class HeapCache implements DerivativeCache {

    /**
     * <p>Cacheable item, either image data or an {@link Info} JSON string.</p>
     *
     * <p>Storing infos as strings makes access less efficient but map size
     * computation more efficient.</p>
     */
    static class Item {

        private final byte[] data;

        Item(byte[] data) {
            this.data = data;
        }

        byte[] getData() {
            return data;
        }

    }

    /**
     * Item key. There are different constructors depending on what the
     * instance is intended to point to.
     */
    static class Key implements Comparable<Key> {

        private final String imageId;
        private String opList;
        private long lastAccessedTime;

        /**
         * Info constructor.
         *
         * @param imageId Identifier of the image described by the info.
         */
        Key(String imageId) {
            this.imageId = imageId;
            touch();
        }

        /**
         * Derivative image constructor.
         *
         * @param imageId Identifier of the source image corresponding to the
         *                derivative image.
         * @param opList  String representation of the operation list
         *                describing the derivative image.
         */
        Key(String imageId, String opList) {
            this(imageId);
            this.opList = opList;
        }

        @Override
        public int compareTo(Key o) {
            return equals(o) ? 0 : 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Key) {
                final Key other = (Key) obj;
                return toString().equals(other.toString());
            }
            return super.equals(obj);
        }

        private String getIdentifier() {
            return imageId;
        }

        private long getLastAccessedTime() {
            return lastAccessedTime;
        }

        private String getOperationList() {
            return opList;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        synchronized void setLastAccessedTime(long time) {
            this.lastAccessedTime = time;
        }

        @Override
        public String toString() {
            return (getOperationList() != null) ?
                    "op:" + getOperationList() : "id:" + getIdentifier();
        }

        /**
         * Updates the last-accessed time.
         */
        synchronized void touch() {
            lastAccessedTime = System.currentTimeMillis();
        }

    }

    /**
     * Buffers written data and adds it to the cache upon closure.
     */
    private class HeapCacheOutputStream extends CompletableOutputStream {

        private final OperationList opList;
        private final ByteArrayOutputStream wrappedStream =
                new ByteArrayOutputStream();

        HeapCacheOutputStream(OperationList opList) {
            this.opList = opList;
        }

        @Override
        public void close() throws IOException {
            LOGGER.debug("Closing stream for {}", opList);
            if (isCompletelyWritten()) {
                Key key = itemKey(opList);
                Item item = new Item(wrappedStream.toByteArray());
                cache.put(key, item);
            }
            try {
                super.close();
            } finally {
                wrappedStream.close();
            }
        }

        @Override
        public void flush() throws IOException {
            wrappedStream.flush();
        }

        @Override
        public void write(int b) throws IOException {
            wrappedStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            wrappedStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wrappedStream.write(b, off, len);
        }

    }

    /**
     * Periodically purges excess contents from the cache.
     */
    private class Worker implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(Worker.class);

        private static final int INTERVAL_SECONDS = 10;

        @Override
        public void run() {
            while (true) {
                if (workerShouldWork.get()) {
                    try {
                        purgeExcess();
                        logger.trace("Cache size: {} items ({} bytes)",
                                size(), getByteSize());
                        Thread.sleep(INTERVAL_SECONDS * 1000);
                    } catch (ConfigurationException e) {
                        logger.error("run(): {}", e.getMessage());
                    } catch (InterruptedException e) {
                        return;
                    }
                } else {
                    logger.debug("run(): stopping");
                    return;
                }
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HeapCache.class);

    private final ConcurrentMap<Key, Item> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean isDirty          = new AtomicBoolean(false);
    private final AtomicBoolean workerShouldWork = new AtomicBoolean(true);

    private final Object persistenceLock = new Object();

    /**
     * <p>Dumps the cache contents to the file specified by
     * {@link edu.illinois.library.cantaloupe.config.Key#HEAPCACHE_PATHNAME},
     * first deleting the file that already exists at that path, if any.</p>
     *
     * <p>Concurrent calls will block.</p>
     *
     * <p>N.B.:
     * {@link edu.illinois.library.cantaloupe.config.Key#HEAPCACHE_PERSIST} is
     * <strong>not</strong> respected.</p>
     */
    void dumpToPersistentStore() throws IOException {
        synchronized (persistenceLock) {
            final Path path = getPath();
            if (path != null) {
                // Delete any existing file that is in the way.
                Files.deleteIfExists(path);
                // Create any necessary directories up to the parent.
                Files.createDirectories(path.getParent());
                // Write out the contents.
                LOGGER.debug("Dumping to {}...", path);

                final long size = size();
                final long byteSize = getByteSize();

                final HeapCacheProtos.Cache.Builder cacheBuilder =
                        HeapCacheProtos.Cache.newBuilder();

                // Iterate over the cache keys and add cache values one-by-one to
                // the protobuf cache, removing them from the cache along the way
                // to save memory.
                final Iterator<Map.Entry<Key,Item>> it = cache.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry<Key,Item> entry = it.next();
                    final Key key                   = entry.getKey();
                    final Item item                 = entry.getValue();
                    if (key.getOperationList() != null) { // it's an image
                        final HeapCacheProtos.Image image =
                                HeapCacheProtos.Image.newBuilder()
                                        .setLastAccessed(key.getLastAccessedTime())
                                        .setIdentifier(key.getIdentifier())
                                        .setOperationList(key.getOperationList())
                                        .setData(ByteString.copyFrom(item.getData()))
                                        .build();
                        cacheBuilder.addImage(image);
                    } else { // it's an info
                        final HeapCacheProtos.Info info =
                                HeapCacheProtos.Info.newBuilder()
                                        .setLastAccessed(key.getLastAccessedTime())
                                        .setIdentifier(key.getIdentifier())
                                        .setJson(new String(item.getData(), StandardCharsets.UTF_8))
                                        .build();
                        cacheBuilder.addInfo(info);
                    }
                    it.remove();
                }
                try (OutputStream fos = Files.newOutputStream(path)) {
                    cacheBuilder.build().writeTo(fos);
                }
                LOGGER.debug("Dumped {} items ({} bytes)", size, byteSize);
            } else {
                throw new IOException("dumpToPersistentStore(): " +
                        HEAPCACHE_PATHNAME + " is not set");
            }
        }
    }

    /**
     * <p>Returns the item corresponding to the given key, updating its last-
     * accessed time before returning it.</p>
     *
     * <p><strong>All cache map retrievals should use this method.</strong></p>
     *
     * @param key Key to access.
     * @return Item corresponding to the given key. May be <code>null</code>.
     */
    private Item get(Key key) {
        Item item = cache.get(key);
        if (item != null) {
            touch(item);
        }
        return item;
    }

    /**
     * @return Current size of the contents in bytes.
     */
    long getByteSize() {
        return cache.values().parallelStream()
                .mapToLong(t -> t.getData().length).sum();
    }

    @Override
    public Optional<Info> getInfo(Identifier identifier) throws IOException {
        Item item = get(itemKey(identifier));
        if (item != null) {
            LOGGER.debug("getInfo(): hit for {}", identifier);
            Info info = Info.fromJSON(new String(item.getData(), StandardCharsets.UTF_8));
            return Optional.of(info);
        }
        return Optional.empty();
    }

    private List<Key> getKeysSortedByLastAccessedTime() {
        List<Key> sortedKeys = new ArrayList<>(cache.keySet());
        sortedKeys.sort((Key k1, Key k2) -> {
            if (k1.getLastAccessedTime() == k2.getLastAccessedTime()) {
                return 0;
            }
            return (k1.getLastAccessedTime() < k2.getLastAccessedTime()) ?
                    -1 : 1;
        });
        return sortedKeys;
    }

    /**
     * @return Path representing the value of
     *         {@link edu.illinois.library.cantaloupe.config.Key#HEAPCACHE_PATHNAME},
     *         of {@literal null} if it is not set.
     */
    private Path getPath() {
        final Configuration config = Configuration.getInstance();
        String pathname = config.getString(HEAPCACHE_PATHNAME);
        if (pathname != null) {
            return Paths.get(pathname);
        }
        return null;
    }

    /**
     * @return Capacity of the instance based on the application configuration.
     * @throws ConfigurationException If the capacity in the configuration is
     *                                invalid.
     */
    long getTargetByteSize() throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        String humanSize = config.getString(HEAPCACHE_TARGET_SIZE);
        if (humanSize != null && !humanSize.isEmpty()) {
            long size = StringUtils.toByteSize(humanSize);
            if (size <= 0) {
                throw new ConfigurationException(HEAPCACHE_TARGET_SIZE +
                        " must be greater than zero.");
            }
            return size;
        }
        throw new ConfigurationException(HEAPCACHE_TARGET_SIZE + " is null");
    }

    @Override
    public void initialize() {
        if (isPersistenceEnabled()) {
            loadFromPersistentStore();
        }

        // Start a worker thread to manage the size.
        try {
            ThreadPool.getInstance().submit(new Worker(),
                    ThreadPool.Priority.LOW);
        } catch (RejectedExecutionException e) {
            LOGGER.error("initialize(): {}", e.getMessage());
        }
    }

    boolean isDirty() {
        return isDirty.get();
    }

    /**
     * @return Value of
     *         {@link edu.illinois.library.cantaloupe.config.Key#HEAPCACHE_PERSIST}
     *         in the application configuration, or {@literal false} if it is
     *         not set.
     */
    boolean isPersistenceEnabled() {
        final Configuration config = Configuration.getInstance();
        return config.getBoolean(HEAPCACHE_PERSIST, false);
    }

    /**
     * @param identifier Image identifier.
     * @return Key for an info for a source image identified by the given
     *         identifier.
     */
    private Key itemKey(Identifier identifier) {
        return new Key(identifier.toString());
    }

    /**
     * @param opList Operation list.
     * @return Key for a derivative image for a source image identified by
     *         the given identifier.
     */
    private Key itemKey(OperationList opList) {
        return new Key(opList.getIdentifier().toString(), opList.toString());
    }

    void loadFromPersistentStore() {
        synchronized (persistenceLock) {
            final Path path = getPath();

            if (path != null && Files.exists(path)) {
                LOGGER.debug("loadFromPersistentStore(): reading {}...", path);

                try (InputStream is = Files.newInputStream(path)) {
                    final HeapCacheProtos.Cache protoCache =
                            HeapCacheProtos.Cache.parseFrom(is);

                    // Read in the images.
                    for (HeapCacheProtos.Image image : protoCache.getImageList()) {
                        final Key key = new Key(image.getIdentifier(),
                                image.getOperationList());
                        key.setLastAccessedTime(image.getLastAccessed());
                        final Item item = new Item(image.getData().toByteArray());
                        cache.put(key, item);
                    }

                    // Read in the infos.
                    for (HeapCacheProtos.Info info : protoCache.getInfoList()) {
                        final Key key = new Key(info.getIdentifier());
                        key.setLastAccessedTime(info.getLastAccessed());
                        final Item item = new Item(info.getJsonBytes().toByteArray());
                        cache.put(key, item);
                    }

                    LOGGER.debug("Loaded {} items ({} bytes)",
                            size(), getByteSize());
                } catch (NoSuchFileException e) {
                    LOGGER.error("loadFromPersistentStore(): file not found: {}",
                            e.getMessage());
                } catch (IOException e) {
                    LOGGER.error("loadFromPersistentStore(): {}",
                            e.getMessage());
                }
            } else {
                LOGGER.debug("loadFromPersistentStore(): does not exist: {}", path);
            }
        }
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList) {
        Item item = get(itemKey(opList));
        if (item != null) {
            return new ByteArrayInputStream(item.getData());
        }
        return null;
    }

    @Override
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList opList) {
        final Key key = itemKey(opList);
        final Item item = cache.get(key);
        if (item != null) {
            LOGGER.debug("newDerivativeImageOutputStream(): hit for {}", opList);
            touch(item);
            return new CompletableNullOutputStream();
        } else {
            LOGGER.debug("newDerivativeImageOutputStream(): miss; caching {}",
                    opList);
            isDirty.lazySet(true);
            return new HeapCacheOutputStream(opList);
        }
    }

    @Override
    public void onCacheWorker() {
        DerivativeCache.super.onCacheWorker();

        if (isPersistenceEnabled()) {
            try {
                dumpToPersistentStore();
            } catch (IOException e) {
                LOGGER.error("onCacheWorker(): {}", e.getMessage(), e);
            }
        }

    }

    @Override
    public void purge() {
        LOGGER.debug("purge(): purging {} items", cache.size());
        cache.clear();
    }

    @Override
    public void purge(Identifier identifier) {
        LOGGER.debug("purge(Identifier): purging {}...", identifier);
        final String imageId = itemKey(identifier).getIdentifier();
        cache.keySet().removeIf(k -> k.getIdentifier().equals(imageId));
    }

    @Override
    public void purge(OperationList opList) {
        LOGGER.debug("purge(OperationList): purging {}...", opList.toString());
        cache.remove(itemKey(opList));
    }

    /**
     * Purges as much content as needed to reduce the current size below the
     * target size, starting with the least-recently-used first.
     */
    void purgeExcess() throws ConfigurationException {
        synchronized (Worker.class) {
            final long size = getByteSize();
            final long targetSize = getTargetByteSize();
            long excess = size - targetSize;
            excess = (excess < 0) ? 0 : excess;
            LOGGER.debug("purgeExcess(): cache size: {}; target: {}; excess: {}",
                    size, targetSize, excess);
            if (excess > 0) {
                long purgedItems = 0;
                long purgedSize = 0;
                for (Key key : getKeysSortedByLastAccessedTime()) {
                    Item item = cache.get(key);
                    if (item != null) {
                        cache.remove(key);
                        purgedItems++;
                        purgedSize += item.getData().length;
                    }
                    if (purgedSize >= excess) {
                        break;
                    }
                }
                isDirty.lazySet(true);
                LOGGER.debug("purgeExcess(): purged {} items ({} bytes)",
                        purgedItems, purgedSize);
            }
        }
    }

    @Override
    public void purgeInfos() {
        cache.entrySet().removeIf(entry -> entry.getKey().opList == null);
    }

    /**
     * Does nothing, as items in this cache never expire.
     */
    @Override
    public void purgeInvalid() {
        LOGGER.debug("purgeInvalid() is not supported by this cache; aborting");
    }

    @Override
    public void put(Identifier identifier, Info imageInfo) throws IOException {
        if (!imageInfo.isPersistable()) {
            LOGGER.debug("put(): info for {} is incomplete; ignoring",
                    identifier);
            return;
        }
        LOGGER.debug("put(): caching info for {}", identifier);
        isDirty.lazySet(true);
        Key key = itemKey(identifier);

        // Rather than storing the info instance itself, we store its JSON
        // serialization, mainly in order to be able to easily get its size.
        Item item = new Item(imageInfo.toJSON().getBytes(StandardCharsets.UTF_8));
        cache.putIfAbsent(key, item);
    }

    /**
     * @return Number of cached items.
     */
    long size() {
        return cache.size();
    }

    @Override
    public void shutdown() {
        workerShouldWork.set(false);

        // Dump the cache contents to disk, if the cache is dirty, and if
        // PERSIST_CONFIG_KEY is set to true.
        if (isDirty() && isPersistenceEnabled()) {
            try {
                dumpToPersistentStore();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    /**
     * Updates the last-accessed time of the key corresponding to the given
     * item.
     *
     * @param item Item whose key should be touched.
     */
    private void touch(Item item) {
        cache.entrySet().parallelStream()
                .filter(entry -> Objects.equals(entry.getValue(), item))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet()).forEach(Key::touch);
    }

}
