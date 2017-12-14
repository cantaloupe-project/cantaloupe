package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lambdaworks.redis.MapScanCursor;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * <p>Cache using Redis via the <a href="http://redis.paluch.biz">Lettuce</a>
 * client.</p>
 *
 * <p>Content is structured as follows:</p>
 *
 * <pre>{
 *     #{@link #IMAGE_HASH_KEY}: {
 *         "operation list string representation": image byte array
 *     },
 *     #{@link #INFO_HASH_KEY}: {
 *         "identifier": "UTF-8 JSON string"
 *     }
 * }</pre>
 */
class RedisCache implements DerivativeCache {

    /**
     * Custom Lettuce codec that enables us to store byte array values
     * corresponding to UTF-8 string keys.
     */
    static class CustomRedisCodec implements RedisCodec<String, byte[]> {

        private RedisCodec<String,String> keyDelegate = new Utf8StringCodec();
        private RedisCodec<byte[],byte[]> valueDelegate = new ByteArrayCodec();

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
        private StatefulRedisConnection<String, byte[]> connection;
        private String hashKey;
        private String valueKey;

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
                bufferStream.close();
            } finally {
                super.close();
            }
        }

        @Override
        public int read() throws IOException {
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
        public int read(byte[] b, int off, int len) throws IOException {
            if (bufferStream == null) {
                bufferValue();
            }
            return bufferStream.read(b, off, len);
        }

    }

    /**
     * Buffers written data and then writes it asynchronously to Redis.
     */
    private static class RedisOutputStream extends OutputStream {

        private ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        private StatefulRedisConnection<String, byte[]> connection;
        private String hashKey;
        private String valueKey;

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
                connection.async().hset(hashKey, valueKey,
                        bufferStream.toByteArray());
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

    /**
     * Thread-safely initializes a shared connection.
     */
    private static class LazyConnectionHolder {

        static StatefulRedisConnection<String, byte[]> connection;

        static {
            Configuration config = Configuration.getInstance();
            RedisURI redisUri =
                    RedisURI.Builder.redis(config.getString(Key.REDISCACHE_HOST)).
                            withPort(config.getInt(Key.REDISCACHE_PORT, 6379)).
                            withSsl(config.getBoolean(Key.REDISCACHE_SSL, false)).
                            withPassword(config.getString(Key.REDISCACHE_PASSWORD, "")).
                            withDatabase(config.getInt(Key.REDISCACHE_DATABASE, 0)).
                            build();
            RedisClient client = RedisClient.create(redisUri);
            connection = client.connect(new CustomRedisCodec());
        }
    }

    private static final Logger logger = LoggerFactory.
            getLogger(RedisCache.class);

    static final String IMAGE_HASH_KEY =
            "edu.illinois.library.cantaloupe.image";
    static final String INFO_HASH_KEY =
            "edu.illinois.library.cantaloupe.info";

    private static StatefulRedisConnection<String, byte[]> getConnection() {
        return LazyConnectionHolder.connection;
    }

    private static String imageKey(OperationList opList) {
        return opList.toString();
    }

    private static String infoKey(Identifier identifier) {
        return identifier.toString();
    }

    @Override
    public Info getImageInfo(Identifier identifier) throws CacheException {
        byte[] json = getConnection().sync().hget(INFO_HASH_KEY,
                infoKey(identifier));
        if (json != null) {
            try {
                String jsonStr = new String(json, "UTF-8");
                return Info.fromJSON(jsonStr);
            } catch (IOException e) {
                throw new CacheException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws CacheException {
        final String imageKey = imageKey(opList);
        if (getConnection().sync().hexists(IMAGE_HASH_KEY, imageKey)) {
            return new RedisInputStream(IMAGE_HASH_KEY, imageKey,
                    getConnection());
        }
        return null;
    }

    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList opList)
            throws CacheException {
        return new RedisOutputStream(IMAGE_HASH_KEY, imageKey(opList),
                getConnection());
    }

    @Override
    public void purge() {
        // Purge infos
        logger.info("purge(): purging {}...", INFO_HASH_KEY);
        getConnection().sync().del(INFO_HASH_KEY);

        // Purge images
        logger.info("purge(): purging {}...", IMAGE_HASH_KEY);
        getConnection().sync().del(IMAGE_HASH_KEY);
    }

    @Override
    public void purge(Identifier identifier) {
        // Purge info
        String infoKey = infoKey(identifier);
        logger.info("purge(Identifier): purging {}...", infoKey);
        getConnection().sync().hdel(INFO_HASH_KEY, infoKey);

        // Purge images
        ScanArgs imagePattern = ScanArgs.Builder.matches(identifier + "*");
        logger.info("purge(Identifier): purging {}...", imagePattern);
        MapScanCursor cursor = getConnection().sync().
                hscan(IMAGE_HASH_KEY, imagePattern);
        for (Object key : cursor.getMap().keySet()) {
            getConnection().sync().hdel(IMAGE_HASH_KEY, (String) key);
        }
    }

    /**
     * No-op.
     */
    @Override
    public void purgeExpired() {
        logger.info("purgeExpired(): " +
                "nothing to do (expiration must be configured in Redis)");
    }

    @Override
    public void purge(OperationList opList) {
        String imageKey = imageKey(opList);
        logger.info("purge(OperationList): purging {}...", imageKey);
        getConnection().sync().hdel(IMAGE_HASH_KEY, imageKey);
    }

    @Override
    public void put(Identifier identifier, Info imageInfo)
            throws CacheException {
        logger.info("put(): caching info for {}", identifier);
        try {
            getConnection().async().hset(INFO_HASH_KEY, infoKey(identifier),
                    imageInfo.toJSON().getBytes("UTF-8"));
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            logger.error("put(): {}", e.getMessage());
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        getConnection().close();
    }

}
