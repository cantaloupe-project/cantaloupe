package edu.illinois.library.cantaloupe.source;

import com.zaxxer.hikari.HikariDataSource;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * <p>Maps an identifier to a binary/BLOB field in a relational database.</p>
 *
 * <p>A custom schema is not required; any schema will work. However, several
 * delegate methods must be implemented in order to obtain the information
 * needed to run the SQL queries.</p>
 *
 * <h1>JDBC Drivers</h1>
 *
 * <p>JDBC drivers are the user's responsibility. A JDBC driver is required
 * and not included.</p>
 *
 * <h1>Format Inference</h1>
 *
 * <ol>
 *     <li>If the {@link DelegateMethod#JDBCSOURCE_MEDIA_TYPE} method returns
 *     either a media type, or a query that can be invoked to obtain a media
 *     type, and that is successful, that format will be used.</li>
 *     <li>If the source image's identifier has a recognized filename
 *     extension, the format will be inferred from that.</li>
 *     <li>Otherwise, a small range of data will be read from the beginning of
 *     the resource, and an attempt will be made to infer a format from any
 *     "magic bytes" it may contain.</li>
 * </ol>
 */
class JdbcSource extends AbstractSource implements StreamSource {

    /**
     * StreamFactory for binary a.k.a. BLOB column values.
     */
    private static class JdbcStreamFactory implements StreamFactory {

        private String sql;
        private String databaseIdentifier;

        JdbcStreamFactory(String sql, String databaseIdentifier) {
            this.sql = sql;
            this.databaseIdentifier = databaseIdentifier;
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, databaseIdentifier);

                LOGGER.debug(sql);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return result.getBinaryStream(1);
                    } else {
                        throw new NoSuchFileException("Resource not found");
                    }
                }
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JdbcSource.class);

    /**
     * Byte length of the range used to detect the source image format.
     */
    private static final int FORMAT_DETECTION_RANGE_LENGTH = 32;

    /**
     * Abstraction of a connection pool.
     */
    private static HikariDataSource dataSource;

    /**
     * @return Connection from the pool. Clients must close it!
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (dataSource == null) {
            final Configuration config = Configuration.getInstance();

            final String connectionString =
                    config.getString(Key.JDBCSOURCE_JDBC_URL, "");
            final int connectionTimeout =
                    1000 * config.getInt(Key.JDBCCACHE_CONNECTION_TIMEOUT, 10);
            final int maxPoolSize =
                    Runtime.getRuntime().availableProcessors() * 2 + 1;
            final String user = config.getString(Key.JDBCSOURCE_USER, "");
            final String password = config.getString(Key.JDBCSOURCE_PASSWORD, "");

            dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(connectionString);
            dataSource.setUsername(user);
            dataSource.setPassword(password);
            dataSource.setPoolName(JdbcSource.class.getSimpleName() + "Pool");
            dataSource.setMaximumPoolSize(maxPoolSize);
            dataSource.setConnectionTimeout(connectionTimeout);

            try (Connection connection = dataSource.getConnection()) {
                LOGGER.info("Using {} {}", connection.getMetaData().getDriverName(),
                        connection.getMetaData().getDriverVersion());
                LOGGER.info("Connection string: {}",
                        config.getString(Key.JDBCSOURCE_JDBC_URL));
            }
        }
        return dataSource.getConnection();
    }

    @Override
    public void checkAccess() throws IOException {
        try (Connection connection = getConnection()) {
            final String sql = getLookupSQL();

            // Check that the image exists.
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, getDatabaseIdentifier());
                LOGGER.debug(sql);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new NoSuchFileException(sql);
                    }
                }
            }
        } catch (ScriptException | SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            try {
                String methodResult = getMediaType();
                if (methodResult != null) {
                    // the delegate method result may be a media type, or an
                    // SQL statement to look it up.
                    if (methodResult.toUpperCase().startsWith("SELECT")) {
                        LOGGER.debug(methodResult);
                        try (Connection connection = getConnection();
                             PreparedStatement statement = connection.
                                     prepareStatement(methodResult)) {
                            statement.setString(1, getDatabaseIdentifier());
                            try (ResultSet resultSet = statement.executeQuery()) {
                                if (resultSet.next()) {
                                    String value = resultSet.getString(1);
                                    if (value != null) {
                                        sourceFormat =
                                                new MediaType(value).toFormat();
                                    }
                                }
                            }
                        }
                    } else {
                        sourceFormat = new MediaType(methodResult).toFormat();
                    }
                }
                // If we don't have a media type yet, attempt to infer one
                // from the identifier.
                if (sourceFormat == null || Format.UNKNOWN.equals(sourceFormat)) {
                    sourceFormat = Format.inferFormat(identifier);
                }
                // If we still don't have a media type, attempt to detect it
                // from the blob contents.
                if (sourceFormat == null || Format.UNKNOWN.equals(sourceFormat)) {
                    detectFormat();
                }
            } catch (ScriptException | SQLException e) {
                throw new IOException(e);
            }
        }
        return sourceFormat;
    }

    /**
     * Reads the first few bytes of a blob and attempts to detect its type,
     * saving the result in {@link #sourceFormat}. If unsuccessful, {@link
     * Format#UNKNOWN} will be set.
     */
    private void detectFormat() throws IOException {
        try (Connection connection = getConnection()) {
            final String sql = getLookupSQL();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, getDatabaseIdentifier());
                LOGGER.debug(sql);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        try (InputStream blobStream =
                                     new BufferedInputStream(result.getBinaryStream(1),
                                             FORMAT_DETECTION_RANGE_LENGTH)) {
                            byte[] headerBytes =
                                    new byte[FORMAT_DETECTION_RANGE_LENGTH];
                            blobStream.read(headerBytes);

                            InputStream headerBytesStream =
                                    new ByteArrayInputStream(headerBytes);
                            List<MediaType> types =
                                    MediaType.detectMediaTypes(headerBytesStream);
                            sourceFormat = types.isEmpty() ?
                                    Format.UNKNOWN : types.get(0).toFormat();
                        }
                    } else {
                        sourceFormat = Format.UNKNOWN;
                    }
                }
            }
        } catch (ScriptException | SQLException e) {
            throw new IOException(e);
        }
    }

    /**
     * @return Result of the {@link
     *         DelegateMethod#JDBCSOURCE_DATABASE_IDENTIFIER} method.
     */
    String getDatabaseIdentifier() throws ScriptException {
        return getDelegateProxy().getJdbcSourceDatabaseIdentifier();
    }

    /**
     * @return Result of the {@link DelegateMethod#JDBCSOURCE_LOOKUP_SQL}
     *         method.
     */
    String getLookupSQL() throws IOException, ScriptException {
        final String sql = getDelegateProxy().getJdbcSourceLookupSQL();

        if (!sql.contains("?")) {
            throw new IOException(DelegateMethod.JDBCSOURCE_LOOKUP_SQL +
                    " implementation does not support prepared statements");
        }
        return sql;
    }

    /**
     * @return Result of the {@link DelegateMethod#JDBCSOURCE_MEDIA_TYPE}
     *         method.
     */
    String getMediaType() throws ScriptException {
        return getDelegateProxy().getJdbcSourceMediaType();
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        try {
            return new JdbcStreamFactory(getLookupSQL(), getDatabaseIdentifier());
        } catch (ScriptException e) {
            throw new IOException(e);
        }
    }

    @Override
    public synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

}
