package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zaxxer.hikari.HikariDataSource;
import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Optional;

/**
 * <p>Cache using a database table, storing images as BLOBs and image infos
 * as JSON strings.</p>
 *
 * <p>This cache requires that a database schema be created manually--it will
 * not do it automatically. The current schema is:</p>
 *
 * <pre>CREATE TABLE IF NOT EXISTS {JdbcCache.derivative_image_table} (
 *     operations VARCHAR(4096) NOT NULL,
 *     image BLOB,
 *     last_accessed DATETIME
 * );
 *
 * CREATE TABLE IF NOT EXISTS {JdbcCache.info_table} (
 *     identifier VARCHAR(4096) NOT NULL,
 *     info VARCHAR(8192) NOT NULL,
 *     last_accessed DATETIME
 * );</pre>
 */
class JdbcCache implements DerivativeCache {

    /**
     * Wraps a {@link Blob} OutputStream, for writing an image to a BLOB.
     * The constructor creates a transaction that is committed on close if the
     * stream is {@link CompletableOutputStream#isComplete()
     * completely written}.
     */
    private class ImageBlobOutputStream extends CompletableOutputStream {

        private final OutputStream blobOutputStream;
        private final OperationList ops;
        private final Connection connection;
        private final Blob blob;

        /**
         * Constructor for writing derivative images.
         *
         * @param conn
         * @param ops Derivative image operation list
         */
        ImageBlobOutputStream(Connection conn,
                              OperationList ops) throws SQLException {
            this.connection = conn;
            this.ops = ops;

            connection.setAutoCommit(false);

            final Configuration config = Configuration.getInstance();
            final String sql = String.format(
                    "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
                    config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE),
                    DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                    DERIVATIVE_IMAGE_TABLE_IMAGE_COLUMN,
                    DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN);
            LOGGER.trace(sql);

            blob = connection.createBlob();
            blobOutputStream = blob.setBinaryStream(1);
        }

        @Override
        public void close() throws IOException {
            LOGGER.debug("Closing stream for {}", ops);
            PreparedStatement statement = null;
            try {
                if (isComplete()) {
                    blobOutputStream.close();
                    final Configuration config = Configuration.getInstance();
                    final String sql = String.format(
                            "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
                                config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE),
                                DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                                DERIVATIVE_IMAGE_TABLE_IMAGE_COLUMN,
                                DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN);
                    LOGGER.debug(sql);
                    statement = connection.prepareStatement(sql);
                    statement.setString(1, ops.toString());
                    statement.setBlob(2, blob);
                    statement.setTimestamp(3, now());
                    statement.executeUpdate();
                    connection.commit();
                } else {
                    connection.rollback();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public void flush() throws IOException {
            blobOutputStream.flush();
        }

        @Override
        public void write(int b) throws IOException {
            blobOutputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            blobOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            blobOutputStream.write(b, off, len);
        }

    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(JdbcCache.class);

    static final String DERIVATIVE_IMAGE_TABLE_IMAGE_COLUMN = "image";
    static final String DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN =
            "last_accessed";
    static final String DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN = "operations";

    static final String INFO_TABLE_IDENTIFIER_COLUMN = "identifier";
    static final String INFO_TABLE_INFO_COLUMN = "info";
    static final String INFO_TABLE_LAST_ACCESSED_COLUMN = "last_accessed";

    private static HikariDataSource dataSource;

    /**
     * @return Connection from the connection pool. Clients must call
     *         {@link Connection#close} when they are done with it.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (dataSource == null) {
            final Configuration config = Configuration.getInstance();
            final String connectionString = config.
                    getString(Key.JDBCCACHE_JDBC_URL, "");
            final int connectionTimeout = 1000 *
                    config.getInt(Key.JDBCCACHE_CONNECTION_TIMEOUT, 10);
            final int maxPoolSize =
                    Runtime.getRuntime().availableProcessors() * 2 + 1;
            final String user = config.getString(Key.JDBCCACHE_USER, "");
            final String password = config.getString(Key.JDBCCACHE_PASSWORD, "");

            dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(connectionString);
            dataSource.setUsername(user);
            dataSource.setPassword(password);
            dataSource.setPoolName("JdbcCachePool");
            dataSource.setMaximumPoolSize(maxPoolSize);
            dataSource.setConnectionTimeout(connectionTimeout);

            // Create a connection in order to log some things and check
            // whether the database is sane.
            try (Connection connection = dataSource.getConnection()) {
                final DatabaseMetaData metadata = connection.getMetaData();
                LOGGER.info("Using {} {}", metadata.getDriverName(),
                        metadata.getDriverVersion());
                LOGGER.info("Connection URL: {}",
                        config.getString(Key.JDBCCACHE_JDBC_URL));

                final String[] tableNames = { getDerivativeImageTableName(),
                        getInfoTableName() };
                for (String tableName : tableNames) {
                    if (!tableExists(connection, tableName)) {
                        LOGGER.error("Missing table: {}", tableName);
                    }
                }
            }
        }
        return dataSource.getConnection();
    }

    /**
     * @return Name of the derivative image table.
     * @throws IllegalArgumentException If the image table name is not set.
     */
    static String getDerivativeImageTableName() {
        final String name = Configuration.getInstance().
                getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE);
        if (name == null) {
            throw new IllegalArgumentException(
                    Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE + " is not set");
        }
        return name;
    }

    /**
     * @return Name of the image info table.
     * @throws IllegalArgumentException If the info table name is not set.
     */
    static String getInfoTableName() {
        final String name = Configuration.getInstance().
                getString(Key.JDBCCACHE_INFO_TABLE);
        if (name == null) {
            throw new IllegalArgumentException(
                    Key.JDBCCACHE_INFO_TABLE + " is not set");
        }
        return name;
    }

    /**
     * @param connection Will not be closed.
     */
    private static boolean tableExists(Connection connection, String tableName)
            throws SQLException {
        DatabaseMetaData dbm = connection.getMetaData();
        try (ResultSet rs = dbm.getTables(null, null, tableName.toUpperCase(), null)) {
            return rs.next();
        }
    }

    /**
     * Updates the last-accessed time of the derivative image corresponding to
     * the given operation list.
     */
    private void accessDerivativeImage(OperationList opList,
                                       Connection connection)
            throws SQLException {
        final String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                getDerivativeImageTableName(),
                DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN,
                DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, now());
            statement.setString(2, opList.toString());

            LOGGER.trace(sql);
            statement.executeUpdate();
        }
    }

    /**
     * Updates the last-accessed time of the derivative image corresponding to
     * the given operation list asynchronously.
     */
    private void accessDerivativeImageAsync(OperationList opList) {
        TaskQueue.getInstance().submit(() -> {
            try (Connection conn = getConnection()) {
                accessDerivativeImage(opList, conn);
            } catch (SQLException e) {
                LOGGER.error("accessDerivativeImageAsync(): {}", e.getMessage());
            }
        });
    }

    /**
     * Updates the last-accessed time of the info corresponding to the given
     * identifier.
     */
    private void accessInfo(Identifier identifier, Connection connection)
            throws SQLException {
        final String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                getInfoTableName(),
                INFO_TABLE_LAST_ACCESSED_COLUMN,
                INFO_TABLE_IDENTIFIER_COLUMN);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, now());
            statement.setString(2, identifier.toString());

            LOGGER.trace(sql);
            statement.executeUpdate();
        }
    }

    /**
     * Updates the last-accessed time of the info corresponding to the given
     * operation list asynchronously.
     */
    private void accessInfoAsync(Identifier identifier) {
        TaskQueue.getInstance().submit(() -> {
            try (Connection conn = getConnection()) {
                accessInfo(identifier, conn);
            } catch (SQLException e) {
                LOGGER.error("accessInfoAsync(): {}", e.getMessage());
            }
        });
    }

    Timestamp earliestValidDate() {
        final long ttl = Configuration.getInstance().
                getLong(Key.DERIVATIVE_CACHE_TTL, 0);
        if (ttl > 0) {
            return new Timestamp(System.currentTimeMillis() - ttl * 1000);
        } else {
            return new Timestamp(0);
        }
    }

    @Override
    public Optional<Info> getInfo(Identifier identifier) throws IOException {
        final String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? AND %s >= ?",
                INFO_TABLE_INFO_COLUMN,
                getInfoTableName(),
                INFO_TABLE_IDENTIFIER_COLUMN,
                INFO_TABLE_LAST_ACCESSED_COLUMN);

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, identifier.toString());
            statement.setTimestamp(2, earliestValidDate());

            LOGGER.trace(sql);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    accessInfoAsync(identifier);

                    LOGGER.debug("Hit for info: {}", identifier);
                    String json = resultSet.getString(1);
                    return Optional.of(Info.fromJSON(json));
                } else {
                    LOGGER.debug("Miss for info: {}", identifier);
                    purgeInfoAsync(identifier);
                }
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        InputStream inputStream = null;

        final String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? AND %s >= ?",
                DERIVATIVE_IMAGE_TABLE_IMAGE_COLUMN,
                getDerivativeImageTableName(),
                DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN,
                DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN);

        try (Connection conn = getConnection();
            PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, opList.toString());
            statement.setTimestamp(2, earliestValidDate());

            LOGGER.trace(sql);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    LOGGER.debug("Hit for image: {}", opList);
                    inputStream = resultSet.getBinaryStream(1);
                    accessDerivativeImageAsync(opList);
                } else {
                    LOGGER.debug("Miss for image: {}", opList);
                    purgeDerivativeImageAsync(opList);
                }
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
        return inputStream;
    }

    @Override
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList ops) throws IOException {
        // TODO: return a no-op stream when a write of an equal op list is in progress in another thread
        LOGGER.debug("Miss; caching {}", ops);
        try {
            return new ImageBlobOutputStream(getConnection(), ops);
        } catch (SQLException e) {
            LOGGER.error("Throwing Except: {}", e);
            throw new IOException(e.getMessage(), e);
        }
    }

    private Timestamp now() {
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
        return new Timestamp(now.getTime());
    }

    @Override
    public void purge() throws IOException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedDerivativeImages =
                    purgeDerivativeImages(connection);
            final int numDeletedInfos = purgeInfos(connection);
            connection.commit();
            LOGGER.debug("Purged {} derivative images and {} infos",
                    numDeletedDerivativeImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(OperationList ops) throws IOException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedImages = purgeDerivativeImage(ops, connection);
            connection.commit();
            LOGGER.debug("Purged {} derivative images", numDeletedImages);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void purgeInfos() throws IOException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            int numDeleted;
            final String sql = "DELETE FROM " + getInfoTableName();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                LOGGER.trace(sql);
                numDeleted = statement.executeUpdate();
            }
            connection.commit();
            LOGGER.debug("purgeInfos(): purged {} info(s)", numDeleted);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void purgeInvalid() throws IOException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedDerivativeImages =
                    purgeExpiredDerivativeImages(connection);
            final int numDeletedInfos = purgeExpiredInfos(connection);
            connection.commit();
            LOGGER.debug("purgeInvalid(): purged {} derivative images and {} info(s)",
                    numDeletedDerivativeImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * @param conn Will not be closed.
     * @return Number of images purged.
     */
    private int purgeExpiredDerivativeImages(Connection conn)
            throws SQLException {
        final String sql = String.format("DELETE FROM %s WHERE %s < ?",
                getDerivativeImageTableName(),
                DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setTimestamp(1, earliestValidDate());
            LOGGER.trace(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * @param conn Will not be closed.
     * @return Number of infos purged.
     */
    private int purgeExpiredInfos(Connection conn)
            throws SQLException {
        final String sql = String.format("DELETE FROM %s WHERE %s < ?",
                getInfoTableName(), INFO_TABLE_LAST_ACCESSED_COLUMN);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setTimestamp(1, earliestValidDate());
            LOGGER.trace(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * @param ops Operation list corresponding to the derivative image to purge.
     * @param conn Will not be closed.
     * @return Number of purged images
     */
    private int purgeDerivativeImage(OperationList ops, Connection conn)
            throws SQLException {
        final String sql = String.format("DELETE FROM %s WHERE %s = ?",
                getDerivativeImageTableName(),
                DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, ops.toString());
            LOGGER.trace(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * @param ops Operation list corresponding to the derivative image to purge.
     */
    private void purgeDerivativeImageAsync(OperationList ops) {
        TaskQueue.getInstance().submit(() -> {
            try (Connection conn = getConnection()) {
                purgeDerivativeImage(ops, conn);
            } catch (SQLException e) {
                LOGGER.error("purgeDerivativeImageAsync(): {}", e.getMessage());
            }
        });
    }

    /**
     * Purges all derivative images.
     *
     * @param conn Will not be closed.
     * @return Number of purged images
     */
    private int purgeDerivativeImages(Connection conn) throws SQLException {
        final String sql = "DELETE FROM " + getDerivativeImageTableName();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            LOGGER.trace(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * Purges all derivative images corresponding to the source image with the
     * given identifier.
     *
     * @param identifier
     * @param conn Will not be closed.
     * @return The number of purged images
     */
    private int purgeDerivativeImages(Identifier identifier, Connection conn)
            throws SQLException {
        final String sql = "DELETE FROM " + getDerivativeImageTableName() +
                " WHERE " + DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN +
                " LIKE ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, identifier.toString() + "%");
            LOGGER.trace(sql);
            return statement.executeUpdate();
        }
    }

    @Override
    public void purge(Identifier identifier) throws IOException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedImages = purgeDerivativeImages(identifier,
                    connection);
            final int numDeletedInfos = purgeInfo(identifier, connection);
            connection.commit();
            LOGGER.debug("Deleted {} cached image(s) and {} cached info(s)",
                    numDeletedImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Purges the info corresponding to the source image with the given
     * identifier.
     *
     * @param identifier
     * @param conn Will not be closed.
     * @return The number of purged infos.
     */
    private int purgeInfo(Identifier identifier, Connection conn)
            throws SQLException {
        final String sql = String.format("DELETE FROM %s WHERE %s = ?",
                getInfoTableName(), INFO_TABLE_IDENTIFIER_COLUMN);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, identifier.toString());
            LOGGER.trace(sql);
            return statement.executeUpdate();
        }
    }

    private void purgeInfoAsync(Identifier identifier) {
        TaskQueue.getInstance().submit(() -> {
            try (Connection conn = getConnection()) {
                purgeInfo(identifier, conn);
            } catch (SQLException e) {
                LOGGER.error("purgeImageInfosAsync(): {}", e.getMessage());
            }
        });
    }

    /**
     * @param conn Will not be closed.
     * @return The number of purged infos.
     */
    private int purgeInfos(Connection conn) throws SQLException {
        final String sql = "DELETE FROM " + getInfoTableName();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            LOGGER.trace(sql);
            return statement.executeUpdate();
        }
    }

    @Override
    public void put(Identifier identifier, Info info) throws IOException {
        if (!info.isPersistable()) {
            LOGGER.trace("put(): info for {} is incomplete; ignoring",
                    identifier);
            return;
        }
        LOGGER.debug("put(): {}", identifier);
        try {
            put(identifier, info.toJSON());
        } catch (JsonProcessingException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void put(Identifier identifier, String info) throws IOException {
        LOGGER.debug("put(): {}", identifier);

        final String sql = String.format(
                "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
                getInfoTableName(),
                INFO_TABLE_IDENTIFIER_COLUMN,
                INFO_TABLE_INFO_COLUMN,
                INFO_TABLE_LAST_ACCESSED_COLUMN);

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);

            // Delete any existing info corresponding to the given identifier.
            purgeInfo(identifier, conn);

            // Add a new info corresponding to the given identifier.
            statement.setString(1, identifier.toString());
            statement.setString(2, info);
            statement.setTimestamp(3, now());

            LOGGER.trace(sql);
            statement.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

}
