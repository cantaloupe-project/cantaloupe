package edu.illinois.library.cantaloupe.source;

import com.zaxxer.hikari.HikariDataSource;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.delegate.DelegateMethod;
import org.apache.commons.io.IOUtils;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>Maps an identifier to a binary/BLOB field in a relational database.</p>
 *
 * <p>A custom schema is not required; most schemas will work. However, several
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
 * <p>See {@link FormatIterator}.</p>
 */
class JdbcSource extends AbstractSource implements Source {

    /**
     * <ol>
     *     <li>If the {@link DelegateMethod#JDBCSOURCE_MEDIA_TYPE} method
     *     returns either a media type, or a query that can be invoked to
     *     obtain one, and that is successful, that format will be used.</li>
     *     <li>If the source image's identifier has a recognized filename
     *     extension, the format will be inferred from that.</li>
     *     <li>Otherwise, a small range of data will be read from the beginning
     *     of the resource, and an attempt will be made to infer a format from
     *     any "magic bytes" it may contain.</li>
     * </ol>
     *
     * @param <T> {@link Format}.
     */
    class FormatIterator<T> implements Iterator<T> {

        /**
         * Infers a {@link Format} based on the media type column.
         */
        private class MediaTypeColumnChecker implements FormatChecker {
            @Override
            public Format check() throws IOException {
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
                                            return new MediaType(value).toFormat();
                                        }
                                    }
                                }
                            }
                        } else {
                            return new MediaType(methodResult).toFormat();
                        }
                    }
                } catch (SQLException | ScriptException e) {
                    throw new IOException(e);
                }
                return Format.UNKNOWN;
            }
        }

        /**
         * Infers a {@link Format} based on image magic bytes.
         */
        private class ByteChecker implements FormatChecker {
            @Override
            public Format check() throws IOException {
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
                                    IOUtils.read(blobStream, headerBytes);

                                    InputStream headerBytesStream =
                                            new ByteArrayInputStream(headerBytes);
                                    List<MediaType> types =
                                            MediaType.detectMediaTypes(headerBytesStream);
                                    return types.isEmpty() ?
                                            Format.UNKNOWN : types.get(0).toFormat();
                                }
                            }
                        }
                    }
                } catch (ScriptException | SQLException e) {
                    throw new IOException(e);
                }
                return Format.UNKNOWN;
            }
        }

        private FormatChecker formatChecker;

        @Override
        public boolean hasNext() {
            return (formatChecker == null ||
                    formatChecker instanceof IdentifierFormatChecker ||
                    formatChecker instanceof FormatIterator.MediaTypeColumnChecker);
        }

        @Override
        public T next() {
            if (formatChecker == null) {
                formatChecker = new IdentifierFormatChecker(identifier);
            } else if (formatChecker instanceof IdentifierFormatChecker) {
                formatChecker = new MediaTypeColumnChecker();
            } else if (formatChecker instanceof FormatIterator.MediaTypeColumnChecker) {
                formatChecker = new ByteChecker();
            } else {
                throw new NoSuchElementException();
            }
            try {
                //noinspection unchecked
                return (T) formatChecker.check();
            } catch (IOException e) {
                LOGGER.warn("Error checking format: {}", e.getMessage());
                //noinspection unchecked
                return (T) Format.UNKNOWN;
            }
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JdbcSource.class);

    /**
     * Byte length of the range used to detect the source image format.
     */
    private static final int FORMAT_DETECTION_RANGE_LENGTH = 32;

    private static HikariDataSource dataSource;

    private FormatIterator<Format> formatIterator = new FormatIterator<>();

    /**
     * @return Connection from the pool. Must be close()d!
     */
    static synchronized Connection getConnection() throws SQLException {
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
    public StatResult stat() throws IOException {
        Instant lastModified = getLastModified();
        StatResult result = new StatResult();
        result.setLastModified(lastModified);
        return result;
    }

    /**
     * @return Result of the {@link
     *         DelegateMethod#JDBCSOURCE_DATABASE_IDENTIFIER} method.
     */
    String getDatabaseIdentifier() throws ScriptException {
        return getDelegateProxy().getJdbcSourceDatabaseIdentifier();
    }

    @Override
    public FormatIterator<Format> getFormatIterator() {
        return formatIterator;
    }

    Instant getLastModified() throws IOException {
        try {
            String methodResult = getDelegateProxy().getJdbcSourceLastModified();
            if (methodResult != null) {
                // the delegate method result may be an ISO 8601 string, or an
                // SQL statement to look it up.
                if (methodResult.toUpperCase().startsWith("SELECT")) {
                    // It's called readability, IntelliJ!
                    //noinspection UnnecessaryLocalVariable
                    final String sql = methodResult;
                    LOGGER.debug(sql);
                    try (Connection connection = getConnection();
                         PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, getDatabaseIdentifier());
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (resultSet.next()) {
                                Timestamp value = resultSet.getTimestamp(1);
                                if (value != null) {
                                    return value.toInstant();
                                }
                            } else {
                                throw new NoSuchFileException(sql);
                            }
                        }
                    }
                } else {
                    return Instant.parse(methodResult);
                }
            }
        } catch (SQLException | ScriptException e) {
            throw new IOException(e);
        }
        return null;
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
            return new JDBCStreamFactory(getLookupSQL(), getDatabaseIdentifier());
        } catch (ScriptException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        super.setIdentifier(identifier);
        reset();
    }

    private void reset() {
        formatIterator = new FormatIterator<>();
    }

    @Override
    public synchronized void shutdown() {
        synchronized (JdbcSource.class) {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
        }
    }

}
