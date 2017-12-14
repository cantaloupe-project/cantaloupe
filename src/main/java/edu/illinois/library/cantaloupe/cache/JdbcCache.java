package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zaxxer.hikari.HikariDataSource;
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
     * The constructor creates a transaction, which is committed on close.
     */
    private class ImageBlobOutputStream extends OutputStream {

        private OutputStream blobOutputStream;
        private OperationList ops;
        private Connection connection;
        private PreparedStatement statement;

        /**
         * Constructor for writing derivative images.
         *
         * @param conn
         * @param ops Derivative image operation list
         */
        ImageBlobOutputStream(Connection conn, OperationList ops)
                throws SQLException {
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
            logger.debug(sql);

            final Blob blob = connection.createBlob();
            blobOutputStream = blob.setBinaryStream(1);
            statement = connection.prepareStatement(sql);
            statement.setString(1, ops.toString());
            statement.setBlob(2, blob);
            statement.setTimestamp(3, now());
        }

        @Override
        public void close() throws IOException {
            logger.debug("Closing stream for {}", ops);
            try {
                statement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            } finally {
                try {
                    statement.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
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

    private static final Logger logger = LoggerFactory.
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
     * @throws SQLException
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
                logger.info("Using {} {}", metadata.getDriverName(),
                        metadata.getDriverVersion());
                logger.info("Connection URL: {}",
                        config.getString(Key.JDBCCACHE_JDBC_URL));

                final String[] tableNames = { getDerivativeImageTableName(),
                        getInfoTableName() };
                for (String tableName : tableNames) {
                    if (!tableExists(connection, tableName)) {
                        logger.error("Missing table: {}", tableName);
                    }
                }
            } catch (CacheException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return dataSource.getConnection();
    }

    /**
     * @return Name of the derivative image table.
     * @throws CacheException If the image table name is not set.
     */
    static String getDerivativeImageTableName() throws CacheException {
        final String name = Configuration.getInstance().
                getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE);
        if (name == null) {
            throw new CacheException(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE +
                    " is not set");
        }
        return name;
    }

    /**
     * @return Name of the image info table.
     * @throws CacheException If the info table name is not set.
     */
    static String getInfoTableName() throws CacheException {
        final String name = Configuration.getInstance().
                getString(Key.JDBCCACHE_INFO_TABLE);
        if (name == null) {
            throw new CacheException(Key.JDBCCACHE_INFO_TABLE + " is not set");
        }
        return name;
    }

    /**
     * @param connection Will not be closed.
     * @throws SQLException
     */
    private static boolean tableExists(Connection connection, String tableName)
            throws SQLException {
        DatabaseMetaData dbm = connection.getMetaData();
        try (ResultSet rs = dbm.getTables(null, null, tableName.toUpperCase(), null)) {
            return rs.next();
        }
    }

    /**
     * Updates the last-accessed time for the derivative image corresponding to
     * the given operation list.
     *
     * @param opList
     * @param connection
     *
     * @throws CacheException
     * @throws SQLException
     */
    private void accessDerivativeImage(OperationList opList,
                                       Connection connection)
            throws CacheException, SQLException {
        final String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                getDerivativeImageTableName(),
                DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN,
                DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, now());
            statement.setString(2, opList.toString());

            logger.debug(sql);
            statement.executeUpdate();
        }
    }

    /**
     * Updates the last-accessed time for the info corresponding to the given
     * identifier.
     *
     * @param identifier
     * @param connection
     *
     * @throws CacheException
     * @throws SQLException
     */
    private void accessImageInfo(Identifier identifier, Connection connection)
            throws CacheException, SQLException {
        final String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                getInfoTableName(),
                INFO_TABLE_LAST_ACCESSED_COLUMN,
                INFO_TABLE_IDENTIFIER_COLUMN);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, now());
            statement.setString(2, identifier.toString());

            logger.debug(sql);
            statement.executeUpdate();
        }
    }

    @Override
    public Info getImageInfo(Identifier identifier) throws CacheException {
        final String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? AND %s >= ?",
                INFO_TABLE_INFO_COLUMN,
                getInfoTableName(),
                INFO_TABLE_IDENTIFIER_COLUMN,
                INFO_TABLE_LAST_ACCESSED_COLUMN);

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, identifier.toString());
            statement.setTimestamp(2, oldestValidDate());

            logger.debug(sql);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    accessImageInfo(identifier, connection);
                    logger.info("Hit for image info: {}", identifier);
                    String json = resultSet.getString(1);
                    return Info.fromJSON(json);
                } else {
                    logger.info("Miss for image info: {}", identifier);
                    purgeImageInfo(identifier, connection);
                }
            }
        } catch (CacheException | IOException | SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws CacheException {
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
            statement.setTimestamp(2, oldestValidDate());

            logger.debug(sql);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    logger.info("Hit for image: {}", opList);
                    inputStream = resultSet.getBinaryStream(1);
                    accessDerivativeImage(opList, conn);
                } else {
                    logger.info("Miss for image: {}", opList);
                    purgeDerivativeImage(opList, conn);
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return inputStream;
    }

    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList ops)
            throws CacheException {
        // TODO: return a no-op stream when a write corresponding to an
        // identical op list is in progress in another thread
        logger.info("Miss; caching {}", ops);
        try {
            return new ImageBlobOutputStream(getConnection(), ops);
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    private Timestamp now() {
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
        return new Timestamp(now.getTime());
    }

    Timestamp oldestValidDate() {
        final long ttl = Configuration.getInstance().
                getLong(Key.CACHE_SERVER_TTL, 0);
        if (ttl > 0) {
            return new Timestamp(System.currentTimeMillis() - ttl * 1000);
        } else {
            return new Timestamp(Long.MIN_VALUE);
        }
    }

    @Override
    public void purge() throws CacheException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedDerivativeImages =
                    purgeDerivativeImages(connection);
            final int numDeletedInfos = purgeImageInfos(connection);
            connection.commit();
            logger.info("Purged {} derivative images and {} infos",
                    numDeletedDerivativeImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(OperationList ops) throws CacheException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedImages = purgeDerivativeImage(ops, connection);
            connection.commit();
            logger.info("Purged {} derivative images", numDeletedImages);
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purgeExpired() throws CacheException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedDerivativeImages =
                    purgeExpiredDerivativeImages(connection);
            final int numDeletedInfos = purgeExpiredInfos(connection);
            connection.commit();
            logger.info("Purged {} derivative images and {} info(s)",
                    numDeletedDerivativeImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    /**
     * @param conn Will not be closed.
     * @return
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeExpiredDerivativeImages(Connection conn)
            throws SQLException, CacheException {
        final String sql = String.format("DELETE FROM %s WHERE %s < ?",
                getDerivativeImageTableName(),
                DERIVATIVE_IMAGE_TABLE_LAST_ACCESSED_COLUMN);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setTimestamp(1, oldestValidDate());
            logger.debug(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * @param conn Will not be closed.
     * @return
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeExpiredInfos(Connection conn)
            throws SQLException, CacheException {
        final String sql = String.format("DELETE FROM %s WHERE %s < ?",
                getInfoTableName(), INFO_TABLE_LAST_ACCESSED_COLUMN);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setTimestamp(1, oldestValidDate());
            logger.debug(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * @param ops Operation list corresponding to the derivative image to purge.
     * @param conn Will not be closed.
     * @return Number of purged images
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeDerivativeImage(OperationList ops, Connection conn)
            throws SQLException, CacheException {
        final String sql = String.format("DELETE FROM %s WHERE %s = ?",
                getDerivativeImageTableName(),
                DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, ops.toString());
            logger.debug(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * @param conn Will not be closed.
     * @return Number of purged images
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeDerivativeImages(Connection conn)
            throws SQLException, CacheException {
        final String sql = "DELETE FROM " + getDerivativeImageTableName();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            logger.debug(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * @param identifier
     * @param conn Will not be closed.
     * @return The number of purged images
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeDerivativeImages(Identifier identifier, Connection conn)
            throws SQLException, CacheException {
        final String sql = "DELETE FROM " + getDerivativeImageTableName() +
                " WHERE " + DERIVATIVE_IMAGE_TABLE_OPERATIONS_COLUMN +
                " LIKE ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, identifier.toString() + "%");
            logger.debug(sql);
            return statement.executeUpdate();
        }
    }

    @Override
    public void purge(Identifier identifier) throws CacheException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedImages = purgeDerivativeImages(identifier,
                    connection);
            final int numDeletedInfos = purgeImageInfo(identifier, connection);
            connection.commit();
            logger.info("Deleted {} cached image(s) and {} cached info(s)",
                    numDeletedImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    /**
     * @param identifier
     * @param conn Will not be closed.
     * @return The number of purged infos
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeImageInfo(Identifier identifier, Connection conn)
            throws SQLException, CacheException {
        final String sql = String.format("DELETE FROM %s WHERE %s = ?",
                getInfoTableName(), INFO_TABLE_IDENTIFIER_COLUMN);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, identifier.toString());
            logger.debug(sql);
            return statement.executeUpdate();
        }
    }

    /**
     * @param conn Will not be closed.
     * @return The number of purged infos
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeImageInfos(Connection conn)
            throws SQLException, CacheException {
        final String sql = "DELETE FROM " + getInfoTableName();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            logger.debug(sql);
            return statement.executeUpdate();
        }
    }

    @Override
    public void put(Identifier identifier, Info imageInfo)
            throws CacheException {
        logger.info("Caching image info: {}", identifier);

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
            purgeImageInfo(identifier, conn);

            // Add a new info corresponding to the given identifier.
            statement.setString(1, identifier.toString());
            statement.setString(2, imageInfo.toJSON());
            statement.setTimestamp(3, now());

            logger.debug(sql);
            statement.executeUpdate();
            conn.commit();
        } catch (SQLException | JsonProcessingException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

}
