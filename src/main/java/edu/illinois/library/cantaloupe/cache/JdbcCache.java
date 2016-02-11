package edu.illinois.library.cantaloupe.cache;

import com.zaxxer.hikari.HikariDataSource;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;

/**
 * <p>Cache using a database table, storing images as BLOBs and image dimensions
 * as integers.</p>
 *
 * <p>This cache requires that a database schema be created manually -- it will
 * not do it automatically. See the user manual for more information.</p>
 */
class JdbcCache implements Cache {

    /**
     * Wraps a {@link Blob} OutputStream, for writing an image to a BLOB.
     * The constructor creates a transaction, which is committed on close.
     */
    private class ImageBlobOutputStream extends OutputStream {

        private OutputStream blobOutputStream;
        private OperationList ops;
        private Connection connection;
        private PreparedStatement statement;

        public ImageBlobOutputStream(Connection conn, OperationList ops)
                throws SQLException {
            this.connection = conn;
            this.ops = ops;

            connection.setAutoCommit(false);

            final Configuration config = Application.getConfiguration();
            final String sql = String.format(
                    "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
                    config.getString(IMAGE_TABLE_CONFIG_KEY),
                    IMAGE_TABLE_OPERATIONS_COLUMN, IMAGE_TABLE_IMAGE_COLUMN,
                    IMAGE_TABLE_LAST_MODIFIED_COLUMN);
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
                    connection.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
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

    public static final String IMAGE_TABLE_IMAGE_COLUMN = "image";
    public static final String IMAGE_TABLE_LAST_MODIFIED_COLUMN = "last_modified";
    public static final String IMAGE_TABLE_OPERATIONS_COLUMN = "operations";
    public static final String INFO_TABLE_HEIGHT_COLUMN = "height";
    public static final String INFO_TABLE_IDENTIFIER_COLUMN = "identifier";
    public static final String INFO_TABLE_LAST_MODIFIED_COLUMN = "last_modified";
    public static final String INFO_TABLE_WIDTH_COLUMN = "width";

    public static final String CONNECTION_TIMEOUT_CONFIG_KEY =
            "JdbcCache.connection_timeout";
    public static final String JDBC_URL_CONFIG_KEY = "JdbcCache.url";
    public static final String PASSWORD_CONFIG_KEY = "JdbcCache.password";
    public static final String IMAGE_TABLE_CONFIG_KEY = "JdbcCache.image_table";
    public static final String INFO_TABLE_CONFIG_KEY = "JdbcCache.info_table";
    public static final String MAX_POOL_SIZE_CONFIG_KEY =
            "JdbcCache.max_pool_size";
    public static final String TTL_CONFIG_KEY = "JdbcCache.ttl_seconds";
    public static final String USER_CONFIG_KEY = "JdbcCache.user";

    private static HikariDataSource dataSource;

    static {
        try (Connection connection = getConnection()) {
            final DatabaseMetaData metadata = connection.getMetaData();
            logger.info("Using {} {}", metadata.getDriverName(),
                    metadata.getDriverVersion());
            final Configuration config = Application.getConfiguration();
            logger.info("Connection URL: {}",
                    config.getString(JDBC_URL_CONFIG_KEY));

            final String imageTableName = getImageTableName();
            final String infoTableName = getInfoTableName();
            if (!tableExists(connection, imageTableName)) {
                logger.error("Missing table: {}", imageTableName);
            }
            if (!tableExists(connection, infoTableName)) {
                logger.error("Missing table: {}", infoTableName);
            }
        } catch (CacheException | SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * @return Connection from the connection pool. Clients must call
     * {@link Connection#close} when they are done with it.
     * @throws SQLException
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (dataSource == null) {
            final Configuration config = Application.getConfiguration();
            final String connectionString = config.
                    getString(JDBC_URL_CONFIG_KEY, "");
            final int connectionTimeout = 1000 *
                    config.getInt(CONNECTION_TIMEOUT_CONFIG_KEY, 10);
            final int maxPoolSize = config.getInt(MAX_POOL_SIZE_CONFIG_KEY, 10);
            final String user = config.getString(USER_CONFIG_KEY, "");
            final String password = config.getString(PASSWORD_CONFIG_KEY, "");

            dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(connectionString);
            dataSource.setUsername(user);
            dataSource.setPassword(password);
            dataSource.setPoolName("JdbcCachePool");
            dataSource.setMaximumPoolSize(maxPoolSize);
            dataSource.setConnectionTimeout(connectionTimeout);
        }
        return dataSource.getConnection();
    }

    /**
     * @return
     * @throws CacheException If the image table name is not set.
     */
    public static String getImageTableName() throws CacheException {
        final String name = Application.getConfiguration().
                getString(IMAGE_TABLE_CONFIG_KEY);
        if (name == null) {
            throw new CacheException(IMAGE_TABLE_CONFIG_KEY + " is not set");
        }
        return name;
    }

    /**
     * @return
     * @throws IOException If the info table name is not set.
     */
    public static String getInfoTableName() throws CacheException {
        final String name = Application.getConfiguration().
                getString(INFO_TABLE_CONFIG_KEY);
        if (name == null) {
            throw new CacheException(INFO_TABLE_CONFIG_KEY + " is not set");
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
        ResultSet rs = dbm.getTables(null, null, tableName.toUpperCase(), null);
        return rs.next();
    }

    @Override
    public Dimension getDimension(Identifier identifier) throws CacheException {
        final Timestamp oldestDate = oldestValidDate();
        final String tableName = getInfoTableName();
        try (Connection connection = getConnection()) {
            final String sql = String.format(
                    "SELECT %s, %s, %s FROM %s WHERE %s = ?",
                    INFO_TABLE_WIDTH_COLUMN, INFO_TABLE_HEIGHT_COLUMN,
                    INFO_TABLE_LAST_MODIFIED_COLUMN, tableName,
                    INFO_TABLE_IDENTIFIER_COLUMN);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, identifier.toString());
            logger.debug(sql);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                if (resultSet.getTimestamp(3).after(oldestDate)) {
                    logger.info("Hit for dimension: {}", identifier);
                    return new Dimension(
                            resultSet.getInt(INFO_TABLE_WIDTH_COLUMN),
                            resultSet.getInt(INFO_TABLE_HEIGHT_COLUMN));
                } else {
                    logger.info("Miss for dimension: {}", identifier);
                    purgeInfo(identifier, connection);
                }
            }
        } catch (CacheException | SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public InputStream getImageInputStream(OperationList ops) {
        InputStream inputStream = null;
        try {
            final String tableName = getImageTableName();
            final Timestamp oldestDate = oldestValidDate();
            try (Connection conn = getConnection()) {
                String sql = String.format(
                        "SELECT %s, %s FROM %s WHERE %s = ?",
                        IMAGE_TABLE_IMAGE_COLUMN,
                        IMAGE_TABLE_LAST_MODIFIED_COLUMN, tableName,
                        IMAGE_TABLE_OPERATIONS_COLUMN);
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setString(1, ops.toString());
                logger.debug(sql);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    if (resultSet.getTimestamp(2).after(oldestDate)) {
                        logger.info("Hit for image: {}", ops);
                        inputStream = resultSet.getBinaryStream(1);
                    } else {
                        logger.info("Miss for image: {}", ops);
                        purgeImage(ops, conn);
                    }
                }
            } catch (CacheException | SQLException e) {
                logger.error(e.getMessage(), e);
            }
        } catch (CacheException e) {
            logger.error(e.getMessage(), e);
        }
        return inputStream;
    }

    @Override
    public OutputStream getImageOutputStream(OperationList ops)
            throws CacheException {
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
        return new java.sql.Timestamp(now.getTime());
    }

    public Timestamp oldestValidDate() {
        final Configuration config = Application.getConfiguration();
        final long ttl = config.getLong(TTL_CONFIG_KEY, 0);
        if (ttl > 0) {
            final Instant oldestInstant = Instant.now().
                    minus(Duration.ofSeconds(ttl));
            return Timestamp.from(oldestInstant);
        } else {
            return new Timestamp(Long.MIN_VALUE);
        }
    }

    @Override
    public void purge() throws CacheException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedImages = purgeImages(connection);
            final int numDeletedInfos = purgeInfos(connection);
            connection.commit();
            logger.info("Deleted {} cached image(s) and {} cached dimension(s)",
                    numDeletedImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(Identifier identifier) throws CacheException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedImages = purgeImages(identifier, connection);
            final int numDeletedDimensions = purgeInfo(identifier, connection);
            connection.commit();
            logger.info("Deleted {} cached image(s) and {} cached dimension(s)",
                    numDeletedImages, numDeletedDimensions);
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(OperationList ops) throws CacheException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedImages = purgeImage(ops, connection);
            final int numDeletedDimensions = purgeInfo(ops.getIdentifier(),
                    connection);
            connection.commit();
            logger.info("Deleted {} cached image(s) and {} cached dimension(s)",
                    numDeletedImages, numDeletedDimensions);
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purgeExpired() throws CacheException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            final int numDeletedImages = purgeExpiredImages(connection);
            final int numDeletedInfos = purgeExpiredInfos(connection);
            connection.commit();
            logger.info("Deleted {} cached image(s) and {} cached dimension(s)",
                    numDeletedImages, numDeletedInfos);
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
    private int purgeExpiredImages(Connection conn)
            throws SQLException, CacheException {
        final String sql = String.format("DELETE FROM %s WHERE %s < ?",
                getImageTableName(), IMAGE_TABLE_LAST_MODIFIED_COLUMN);
        final PreparedStatement statement = conn.prepareStatement(sql);
        statement.setTimestamp(1, oldestValidDate());
        logger.debug(sql);
        return statement.executeUpdate();
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
                getInfoTableName(), INFO_TABLE_LAST_MODIFIED_COLUMN);
        final PreparedStatement statement = conn.prepareStatement(sql);
        statement.setTimestamp(1, oldestValidDate());
        logger.debug(sql);
        return statement.executeUpdate();
    }

    /**
     * @param ops
     * @param conn Will not be closed.
     * @return The number of purged images
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeImage(OperationList ops, Connection conn)
            throws SQLException, CacheException {
        String sql = String.format("DELETE FROM %s WHERE %s = ?",
                getImageTableName(), IMAGE_TABLE_OPERATIONS_COLUMN);
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, ops.toString());
        logger.debug(sql);
        return statement.executeUpdate();
    }

    /**
     * @param conn Will not be closed.
     * @return The number of purged images
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeImages(Connection conn)
            throws SQLException, CacheException {
        String sql = "DELETE FROM " + getImageTableName();
        PreparedStatement statement = conn.prepareStatement(sql);
        logger.debug(sql);
        return statement.executeUpdate();
    }

    /**
     * @param identifier
     * @param conn Will not be closed.
     * @return The number of purged images
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeImages(Identifier identifier, Connection conn)
            throws SQLException, CacheException {
        String sql = "DELETE FROM " + getImageTableName() + " WHERE " +
                IMAGE_TABLE_OPERATIONS_COLUMN + " LIKE ?";
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, identifier.toString() + "%");
        logger.debug(sql);
        return statement.executeUpdate();
    }

    /**
     * @param identifier
     * @param conn Will not be closed.
     * @return The number of purged infos
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeInfo(Identifier identifier, Connection conn)
            throws SQLException, CacheException {
        String sql = String.format("DELETE FROM %s WHERE %s = ?",
                getInfoTableName(), INFO_TABLE_IDENTIFIER_COLUMN);
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, identifier.toString());
        logger.debug(sql);
        return statement.executeUpdate();
    }

    /**
     * @param conn Will not be closed.
     * @return The number of purged infos
     * @throws SQLException
     * @throws CacheException
     */
    private int purgeInfos(Connection conn) throws SQLException, CacheException {
        final String sql = "DELETE FROM " + getInfoTableName();
        final PreparedStatement statement = conn.prepareStatement(sql);
        logger.debug(sql);
        return statement.executeUpdate();
    }

    @Override
    public void putDimension(Identifier identifier, Dimension dimension)
            throws CacheException {
        logger.info("Caching dimension: {}", identifier);
        try (Connection conn = getConnection()) {
            String sql = String.format(
                    "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                    getInfoTableName(), INFO_TABLE_IDENTIFIER_COLUMN,
                    INFO_TABLE_WIDTH_COLUMN, INFO_TABLE_HEIGHT_COLUMN,
                    INFO_TABLE_LAST_MODIFIED_COLUMN);
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, identifier.toString());
            statement.setInt(2, dimension.width);
            statement.setInt(3, dimension.height);
            statement.setTimestamp(4, now());
            logger.debug(sql);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

}
