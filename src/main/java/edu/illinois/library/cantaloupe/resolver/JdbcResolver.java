package edu.illinois.library.cantaloupe.resolver;

import com.zaxxer.hikari.HikariDataSource;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <p>Maps an identifier to a binary/BLOB field in a relational database.</p>
 *
 * <p>A custom schema is not required; any schema will work. However, several
 * delegate methods must be implemented in order to obtain the information
 * needed to run the SQL queries.</p>
 *
 * <p>JDBC drivers are the client's responsibility. A JDBC driver is required
 * and not included.</p>
 */
class JdbcResolver extends AbstractResolver implements StreamResolver {

    /**
     * StreamSource for binary a.k.a. BLOB column values.
     */
    private static class JdbcStreamSource implements StreamSource {

        private String sql;
        private String databaseIdentifier;

        JdbcStreamSource(String sql, String databaseIdentifier) {
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
                        throw new IOException("Resource not found");
                    }
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(JdbcResolver.class);

    private static final String GET_DATABASE_IDENTIFIER_DELEGATE_METHOD =
            "JdbcResolver::get_database_identifier";
    private static final String GET_LOOKUP_SQL_DELEGATE_METHOD =
            "JdbcResolver::get_lookup_sql";
    private static final String GET_MEDIA_TYPE_DELEGATE_METHOD =
            "JdbcResolver::get_media_type";

    private static HikariDataSource dataSource;

    /**
     * @return Connection from the connection pool. Clients must close it.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (dataSource == null) {
            final Configuration config = Configuration.getInstance();

            final String connectionString =
                    config.getString(Key.JDBCRESOLVER_JDBC_URL, "");
            final int connectionTimeout =
                    1000 * config.getInt(Key.JDBCCACHE_CONNECTION_TIMEOUT, 10);
            final int maxPoolSize =
                    Runtime.getRuntime().availableProcessors() * 2 + 1;
            final String user = config.getString(Key.JDBCRESOLVER_USER, "");
            final String password = config.getString(Key.JDBCRESOLVER_PASSWORD, "");

            dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(connectionString);
            dataSource.setUsername(user);
            dataSource.setPassword(password);
            dataSource.setPoolName("JdbcResolverPool");
            dataSource.setMaximumPoolSize(maxPoolSize);
            dataSource.setConnectionTimeout(connectionTimeout);

            try (Connection connection = dataSource.getConnection()) {
                LOGGER.info("Using {} {}", connection.getMetaData().getDriverName(),
                        connection.getMetaData().getDriverVersion());
                LOGGER.info("Connection string: {}",
                        config.getString(Key.JDBCRESOLVER_JDBC_URL));
            }
        }
        return dataSource.getConnection();
    }

    @Override
    public void checkAccess() throws IOException {
        try (Connection connection = getConnection()) {
            final String sql = getLookupSQL();
            if (!sql.contains("?")) {
                throw new IOException(GET_LOOKUP_SQL_DELEGATE_METHOD +
                        " implementation does not support prepared statements");
            }

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
        } catch (ScriptException | SQLException |
                DelegateScriptDisabledException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            try {
                // The JdbcResolver::get_media_type() delegate method may
                // return a media type, or nil.
                String methodResult = getMediaType();
                MediaType mediaType = null;
                if (methodResult != null) {
                    // the function result may be a media type, or an SQL
                    // statement to look it up.
                    if (methodResult.toUpperCase().startsWith("SELECT")) {
                        LOGGER.debug(methodResult);
                        try (Connection connection = getConnection();
                             PreparedStatement statement = connection.
                                     prepareStatement(methodResult)) {
                            statement.setString(1, getDatabaseIdentifier());
                            try (ResultSet resultSet = statement.executeQuery()) {
                                if (resultSet.next()) {
                                    mediaType = new MediaType(resultSet.getString(1));
                                }
                            }
                        }
                    } else {
                        mediaType = new MediaType(methodResult);
                    }
                } else {
                    mediaType = Format.inferFormat(identifier).
                            getPreferredMediaType();
                }
                if (mediaType != null) {
                    sourceFormat = mediaType.toFormat();
                } else {
                    sourceFormat = Format.UNKNOWN;
                }
            } catch (ScriptException | SQLException |
                    DelegateScriptDisabledException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return sourceFormat;
    }

    /**
     * @return Result of the {@link #GET_DATABASE_IDENTIFIER_DELEGATE_METHOD}
     *         method.
     */
    String getDatabaseIdentifier() throws IOException,
            ScriptException, DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(
                GET_DATABASE_IDENTIFIER_DELEGATE_METHOD,
                identifier.toString(), context.asMap());
        return (String) result;
    }

    /**
     * @return Result of the {@link #GET_LOOKUP_SQL_DELEGATE_METHOD} method.
     */
    String getLookupSQL() throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_LOOKUP_SQL_DELEGATE_METHOD);
        final String resultStr = (String) result;
        if (!resultStr.contains("?")) {
            throw new IOException(GET_LOOKUP_SQL_DELEGATE_METHOD +
                    " implementation does not support prepared statements");
        }
        return resultStr;
    }

    /**
     * @return Result of the {@link #GET_MEDIA_TYPE_DELEGATE_METHOD} method.
     */
    String getMediaType() throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_MEDIA_TYPE_DELEGATE_METHOD);
        return (String) result;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        try {
            return new JdbcStreamSource(getLookupSQL(), getDatabaseIdentifier());
        } catch (ScriptException | DelegateScriptDisabledException e) {
            throw new IOException(e.getMessage(), e);
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
