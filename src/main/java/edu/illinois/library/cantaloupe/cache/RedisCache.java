package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * <p>Cache using Redis via the <a href="https://lettuce.io">Lettuce</a>
 * client.</p>
 *
 * <p>Content is structured as follows:</p>
 *
 * {@code {
 *     #{@link #IMAGE_HASH_KEY}: {
 *         "operation list string representation": image byte array
 *     },
 *     #{@link #INFO_HASH_KEY}: {
 *         "identifier": "UTF-8 JSON string"
 *     }
 * }}
 *
 * @since 3.4
 */
class RedisCache implements DerivativeCache {

    /**
     * Custom Lettuce codec that enables us to store byte array values
     * corresponding to UTF-8 string keys.
     */
    static class CustomRedisCodec implements RedisCodec<String, byte[]> {

        private final RedisCodec<String,String> keyDelegate = StringCodec.UTF8;
        private final RedisCodec<byte[],byte[]> valueDelegate = new ByteArrayCodec();

        @Override
        public ByteBuffer encodeKey(String k) {
            return keyDelegate.encodeKey(k);
        }

        @Override
        public ByteBuffer encodeValue(byte[] o) {
            return valueDelegate.encodeValue(o);
        }

        @Override
        public String decodeKey(ByteBuffer byteBuffer) {
            return keyDelegate.decodeKey(byteBuffer);
        }

        @Override
        public byte[] decodeValue(ByteBuffer byteBuffer) {
            return valueDelegate.decodeValue(byteBuffer);
        }

    }

    /**
     * Reads data into a buffer and provides stream access to it.
     */
    private static class RedisInputStream extends InputStream {

        private ByteArrayInputStream bufferStream;
        private final StatefulRedisConnection<String, byte[]> connection;
        private final String hashKey;
        private final String valueKey;

        RedisInputStream(String hashKey,
                         String valueKey,
                         StatefulRedisConnection<String, byte[]> connection) {
            this.connection = connection;
            this.hashKey = hashKey;
            this.valueKey = valueKey;
        }

        private void bufferValue() {
            byte[] value = connection.sync().hget(hashKey, valueKey);
            bufferStream = new ByteArrayInputStream(value);
        }

        @Override
        public void close() throws IOException {
            try {
                if (bufferStream != null) {
                    bufferStream.close();
                }
            } finally {
                super.close();
            }
        }

        @Override
        public int read() {
            if (bufferStream == null) {
                bufferValue();
            }
            return bufferStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (bufferStream == null) {
                bufferValue();
            }
            return bufferStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (bufferStream == null) {
                bufferValue();
            }
            return bufferStream.read(b, off, len);
        }

    }

    /**
     * Buffers written data and then writes it asynchronously to Redis.
     */
    private static class RedisOutputStream extends CompletableOutputStream {

        private final ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        private final StatefulRedisConnection<String, byte[]> connection;
        private final String hashKey;
        private final String valueKey;

        RedisOutputStream(String hashKey,
                          String valueKey,
                          StatefulRedisConnection<String, byte[]> connection) {
            this.connection = connection;
            this.hashKey = hashKey;
            this.valueKey = valueKey;
        }

        @Override
        public void close() throws IOException {
            try {
                if (isCompletelyWritten()) {
                    connection.async().hset(hashKey, valueKey,
                            bufferStream.toByteArray());
                }
            } finally {
                super.close();
            }
        }

        @Override
        public void flush() throws IOException {
            bufferStream.flush();
        }

        @Override
        public void write(int b) throws IOException {
            bufferStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            bufferStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            bufferStream.write(b, off, len);
        }

    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(RedisCache.class);

    private static final String IMAGE_HASH_KEY =
            "edu.illinois.library.cantaloupe.image";
    private static final String INFO_HASH_KEY =
            "edu.illinois.library.cantaloupe.info";

    private static StatefulRedisConnection<String, byte[]> connection;

    private static synchronized StatefulRedisConnection<String, byte[]> getConnection() {
        if (connection == null) {
            Configuration config = Configuration.getInstance();
            RedisURI redisUri =
                    RedisURI.Builder.redis(config.getString(Key.REDISCACHE_HOST)).
                            withPort(config.getInt(Key.REDISCACHE_PORT, 6379)).
                            withSsl(config.getBoolean(Key.REDISCACHE_SSL, false)).
                            withPassword(config.getString(Key.REDISCACHE_PASSWORD, "").toCharArray()).
                            withDatabase(config.getInt(Key.REDISCACHE_DATABASE, 0)).
                            build();
            RedisClient client = RedisClient.create(redisUri);
            connection = client.connect(new CustomRedisCodec());
        }
        return connection;
    }

    private static String imageKey(OperationList opList) {
        return opList.toString();
    }

    private static String infoKey(Identifier identifier) {
        return identifier.toString();
    }

    @Override
    public Optional<Info> getInfo(Identifier identifier) throws IOException {
        byte[] json = getConnection().sync().hget(INFO_HASH_KEY,
                infoKey(identifier));
        if (json != null) {
            String jsonStr = new String(json, StandardCharsets.UTF_8);
            return Optional.of(Info.fromJSON(jsonStr));
        }
        return Optional.empty();
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList) {
        final String imageKey = imageKey(opList);
        if (getConnection().sync().hexists(IMAGE_HASH_KEY, imageKey)) {
            return new RedisInputStream(IMAGE_HASH_KEY, imageKey,
                    getConnection());
        }
        return null;
    }

    @Override
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList opList) {
        return new RedisOutputStream(IMAGE_HASH_KEY, imageKey(opList),
                getConnection());
    }

    @Override
    public void purge() {
        purgeInfos();
        purgeImages();
    }

    @Override
    public void purge(Identifier identifier) {
        // Purge info
        String infoKey = infoKey(identifier);
        LOGGER.debug("purge(Identifier): purging {}...", infoKey);
        getConnection().sync().hdel(INFO_HASH_KEY, infoKey);

        // Purge images
        ScanArgs imagePattern = ScanArgs.Builder.matches(identifier + "*");
        LOGGER.debug("purge(Identifier): purging {}...", imagePattern);

        MapScanCursor<String, byte[]> cursor = getConnection().sync().
                hscan(IMAGE_HASH_KEY, imagePattern);
        for (String key : cursor.getMap().keySet()) {
            getConnection().sync().hdel(IMAGE_HASH_KEY, key);
        }
    }

    private void purgeImages() {
        LOGGER.debug("purgeImages(): purging {}...", IMAGE_HASH_KEY);
        getConnection().sync().del(IMAGE_HASH_KEY);
    }

    @Override
    public void purgeInfos() {
        LOGGER.debug("purgeInfos(): purging {}...", INFO_HASH_KEY);
        getConnection().sync().del(INFO_HASH_KEY);
    }

    /**
     * No-op.
     */
    @Override
    public void purgeInvalid() {
        LOGGER.debug("purgeInvalid(): " +
                "nothing to do (expiration must be configured in Redis)");
    }

    @Override
    public void purge(OperationList opList) {
        String imageKey = imageKey(opList);
        LOGGER.debug("purge(OperationList): purging {}...", imageKey);
        getConnection().sync().hdel(IMAGE_HASH_KEY, imageKey);
    }

    @Override
    public void put(Identifier identifier, Info info) throws IOException {
        if (!info.isPersistable()) {
            LOGGER.debug("put(): info for {} is incomplete; ignoring",
                    identifier);
            return;
        }
        LOGGER.debug("put(): caching info for {}", identifier);
        try {
            getConnection().async().hset(
                    INFO_HASH_KEY,
                    infoKey(identifier),
                    info.toJSON().getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            LOGGER.error("put(): {}", e.getMessage());
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        getConnection().close();
    }

}
