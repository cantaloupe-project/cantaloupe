package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.request.Identifier;
import edu.illinois.library.cantaloupe.request.Parameters;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * Cache using a database table, storing images as BLOBs and image dimensions
 * as integers.
 */
class JdbcCache implements Cache {

    private static final Logger logger = LoggerFactory.
            getLogger(JdbcCache.class);

    private static final String IMAGE_TABLE_IMAGE_COLUMN = "image";
    private static final String IMAGE_TABLE_LAST_MODIFIED_COLUMN = "last_modified";
    private static final String IMAGE_TABLE_PARAMS_COLUMN = "params";
    private static final String INFO_TABLE_HEIGHT_COLUMN = "height";
    private static final String INFO_TABLE_IDENTIFIER_COLUMN = "identifier";
    private static final String INFO_TABLE_LAST_MODIFIED_COLUMN = "last_modified";
    private static final String INFO_TABLE_WIDTH_COLUMN = "width";

    private static final String CONNECTION_STRING_KEY = "JdbcCache.connection_string";
    private static final String PASSWORD_KEY = "JdbcCache.password";
    private static final String IMAGE_TABLE_KEY = "JdbcCache.image_table";
    private static final String INFO_TABLE_KEY = "JdbcCache.info_table";
    private static final String TTL_KEY = "JdbcCache.ttl_seconds";
    private static final String USER_KEY = "JdbcCache.user";

    private static Connection connection;

    static {
        try {
            Connection connection = getConnection();
            logger.info("Using {} {}", connection.getMetaData().getDriverName(),
                    connection.getMetaData().getDriverVersion());
            Configuration config = Application.getConfiguration();
            logger.info("Connection string: {}",
                    config.getString("JdbcCache.connection_string"));
        } catch (SQLException e) {
            logger.error("Failed to establish a database connection", e);
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null) {
            Configuration config = Application.getConfiguration();
            final String connectionString = config.
                    getString(CONNECTION_STRING_KEY, "");
            final String user = config.getString(USER_KEY, "");
            final String password = config.getString(PASSWORD_KEY, "");
            connection = DriverManager.getConnection(connectionString, user,
                    password);
        }
        return connection;
    }

    @Override
    public void flush() throws IOException {
        try {
            int numDeletedImages = flushImages();
            int numDeletedInfos = flushInfos();
            logger.info("Deleted {} cached image(s) and {} cached dimension(s)",
                    numDeletedImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * @return The number of flushed images
     * @throws SQLException
     * @throws IOException
     */
    private int flushImages() throws SQLException, IOException {
        Configuration config = Application.getConfiguration();
        Connection conn = getConnection();

        final String imageTableName = config.getString(IMAGE_TABLE_KEY);
        if (imageTableName != null && imageTableName.length() > 0) {
            String sql = "DELETE FROM " + imageTableName;
            PreparedStatement statement = conn.prepareStatement(sql);
            return statement.executeUpdate();
        } else {
            throw new IOException(IMAGE_TABLE_KEY + " is not set");
        }
    }

    /**
     * @return The number of flushed infos
     * @throws SQLException
     * @throws IOException
     */
    private int flushInfos() throws SQLException, IOException {
        Configuration config = Application.getConfiguration();
        Connection conn = getConnection();

        final String infoTableName = config.getString(INFO_TABLE_KEY);
        if (infoTableName != null && infoTableName.length() > 0) {
            String sql = "DELETE FROM " + infoTableName;
            PreparedStatement statement = conn.prepareStatement(sql);
            return statement.executeUpdate();
        } else {
            throw new IOException(INFO_TABLE_KEY + " is not set");
        }
    }

    @Override
    public void flush(Parameters params) throws IOException {
        try {
            int numDeletedImages = flushImage(params);
            int numDeletedDimensions = flushInfo(params.getIdentifier());
            logger.info("Deleted {} cached image(s) and {} cached dimension(s)",
                    numDeletedImages, numDeletedDimensions);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * @param params
     * @return The number of flushed images
     * @throws SQLException
     * @throws IOException
     */
    private int flushImage(Parameters params) throws SQLException, IOException {
        Configuration config = Application.getConfiguration();
        Connection conn = getConnection();

        final String imageTableName = config.getString(IMAGE_TABLE_KEY);
        if (imageTableName != null && imageTableName.length() > 0) {
            String sql = String.format("DELETE FROM %s WHERE %s = ?",
                    imageTableName, IMAGE_TABLE_PARAMS_COLUMN);
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, params.toString());
            return statement.executeUpdate();
        } else {
            throw new IOException(IMAGE_TABLE_KEY + " is not set");
        }
    }

    /**
     * @param identifier
     * @return The number of flushed infos
     * @throws SQLException
     * @throws IOException
     */
    private int flushInfo(Identifier identifier) throws SQLException, IOException {
        Configuration config = Application.getConfiguration();
        Connection conn = getConnection();

        final String infoTableName = config.getString(INFO_TABLE_KEY);
        if (infoTableName != null && infoTableName.length() > 0) {
            String sql = String.format("DELETE FROM %s WHERE %s = ?",
                    infoTableName, INFO_TABLE_IDENTIFIER_COLUMN);
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, identifier.toString());
            return statement.executeUpdate();
        } else {
            throw new IOException(INFO_TABLE_KEY + " is not set");
        }
    }

    @Override
    public void flushExpired() throws IOException {
        try {
            int numDeletedImages = flushExpiredImages();
            int numDeletedInfos = flushExpiredInfos();
            logger.info("Deleted {} cached image(s) and {} cached dimension(s)",
                    numDeletedImages, numDeletedInfos);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private int flushExpiredImages() throws SQLException, IOException {
        Configuration config = Application.getConfiguration();
        Connection conn = getConnection();

        final String imageTableName = config.getString(IMAGE_TABLE_KEY);
        if (imageTableName != null && imageTableName.length() > 0) {
            String sql = String.format("DELETE FROM %s WHERE %s < ?",
                    imageTableName, IMAGE_TABLE_LAST_MODIFIED_COLUMN);
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setDate(1, oldestValidDate());
            return statement.executeUpdate();
        } else {
            throw new IOException(IMAGE_TABLE_KEY + " is not set");
        }
    }

    private int flushExpiredInfos() throws SQLException, IOException {
         Configuration config = Application.getConfiguration();
        Connection conn = getConnection();

        final String infoTableName = config.getString(INFO_TABLE_KEY);
        if (infoTableName != null && infoTableName.length() > 0) {
            String sql = String.format("DELETE FROM %s WHERE %s < ?",
                    infoTableName, INFO_TABLE_LAST_MODIFIED_COLUMN);
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setDate(1, oldestValidDate());
            return statement.executeUpdate();
        } else {
            throw new IOException(INFO_TABLE_KEY + " is not set");
        }
    }

    @Override
    public Dimension getDimension(Identifier identifier) throws IOException {
        final Date oldestDate = oldestValidDate();
        Configuration config = Application.getConfiguration();
        String tableName = config.getString(INFO_TABLE_KEY, "");
        if (tableName != null && tableName.length() > 0) {
            try {
                String sql = String.format(
                        "SELECT width, height FROM %s " +
                        "WHERE identifier = ? AND %s > ?",
                        tableName, INFO_TABLE_LAST_MODIFIED_COLUMN);
                PreparedStatement statement = getConnection().prepareStatement(sql);
                statement.setString(1, identifier.getValue());
                statement.setDate(2, oldestDate);
                ResultSet resultSet = statement.getResultSet();
                if (resultSet.next()) {
                    return new Dimension(resultSet.getInt("width"),
                            resultSet.getInt("height"));
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            throw new IOException(INFO_TABLE_KEY + " is not set");
        }
        return null;
    }

    @Override
    public InputStream getImageInputStream(Parameters params) {
        InputStream inputStream = null;
        final Date oldestDate = oldestValidDate();
        Configuration config = Application.getConfiguration();
        String tableName = config.getString(IMAGE_TABLE_KEY, "");
        if (tableName != null && tableName.length() > 0) {
            try {
                Connection conn = getConnection();
                String sql = String.format(
                        "SELECT %s FROM %s WHERE %s = ? AND %s > ?",
                        IMAGE_TABLE_IMAGE_COLUMN, tableName,
                        IMAGE_TABLE_PARAMS_COLUMN,
                        IMAGE_TABLE_LAST_MODIFIED_COLUMN);
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setString(1, params.toString());
                statement.setDate(2, oldestDate);
                ResultSet resultSet = statement.getResultSet();
                if (resultSet.next()) {
                    inputStream = resultSet.getBinaryStream(1);
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            logger.error("{} is not set", IMAGE_TABLE_KEY);
        }
        return inputStream;
    }

    @Override
    public OutputStream getImageOutputStream(Parameters params)
            throws IOException {
        Configuration config = Application.getConfiguration();
        String tableName = config.getString(IMAGE_TABLE_KEY, "");
        if (tableName != null && tableName.length() > 0) {
            try {
                Connection conn = getConnection();
                Blob blob = conn.createBlob();
                return blob.setBinaryStream(0);
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            throw new IOException(IMAGE_TABLE_KEY + " is not set");
        }
    }

    private void createImageTable() throws IOException {
        Configuration config = Application.getConfiguration();
        String tableName = config.getString(IMAGE_TABLE_KEY, "");
        if (tableName != null && tableName.length() > 0) {
            try {
                Connection conn = getConnection();
                String sql = String.format(
                        "IF (NOT EXISTS (" +
                                "SELECT * FROM INFORMATION_SCHEMA.TABLES " +
                                "WHERE TABLE_NAME = '%s')) " +
                        "BEGIN " +
                        "CREATE TABLE %s (" +
                                "id INTEGER NOT NULL PRIMARY KEY, " +
                                "%s VARCHAR(4096) NOT NULL, " +
                                "%s BLOB, " +
                                "%s DATETIME) " +
                        "END",
                        tableName, tableName,
                        IMAGE_TABLE_PARAMS_COLUMN,
                        IMAGE_TABLE_IMAGE_COLUMN,
                        IMAGE_TABLE_LAST_MODIFIED_COLUMN);
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.execute();
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            throw new IOException(IMAGE_TABLE_KEY + " is not set");
        }
    }

    private void createInfoTable() throws IOException {
        Configuration config = Application.getConfiguration();
        String tableName = config.getString(INFO_TABLE_KEY, "");
        if (tableName != null && tableName.length() > 0) {
            try {
                Connection conn = getConnection();
                String sql = String.format(
                        "IF (NOT EXISTS (" +
                                "SELECT * FROM INFORMATION_SCHEMA.TABLES " +
                                "WHERE TABLE_NAME = '%s')) " +
                        "BEGIN " +
                        "CREATE TABLE %s (" +
                                "id INTEGER NOT NULL PRIMARY KEY, " +
                                "%s VARCHAR(4096) NOT NULL, " +
                                "%s INTEGER, " +
                                "%s INTEGER, " +
                                "%s DATETIME) " +
                        "END",
                        tableName, tableName, INFO_TABLE_IDENTIFIER_COLUMN,
                        INFO_TABLE_WIDTH_COLUMN, INFO_TABLE_HEIGHT_COLUMN,
                        INFO_TABLE_LAST_MODIFIED_COLUMN);
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.execute();
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            throw new IOException(INFO_TABLE_KEY + " is not set");
        }
    }

    private Date oldestValidDate() {
        Configuration config = Application.getConfiguration();
        final Instant oldestInstant = Instant.now().
                minus(Duration.ofSeconds(config.getLong(TTL_KEY, 0)));
        return new Date(Date.from(oldestInstant).getTime());
    }

    @Override
    public void putDimension(Identifier identifier, Dimension dimension)
            throws IOException {
        Configuration config = Application.getConfiguration();
        String tableName = config.getString(INFO_TABLE_KEY, "");
        if (tableName != null && tableName.length() > 0) {
            try {
                Connection conn = getConnection();
                String sql = String.format(
                        "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
                        tableName, INFO_TABLE_IDENTIFIER_COLUMN, INFO_TABLE_WIDTH_COLUMN,
                        INFO_TABLE_HEIGHT_COLUMN);
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setString(1, identifier.getValue());
                statement.setInt(2, dimension.width);
                statement.setInt(3, dimension.height);
                statement.execute();
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            throw new IOException(INFO_TABLE_KEY + " is not set");
        }
    }

}
