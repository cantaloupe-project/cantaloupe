package edu.illinois.library.cantaloupe.resolver;

import com.zaxxer.hikari.HikariDataSource;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class JdbcResolver extends AbstractResolver implements StreamResolver {

    private static class JdbcStreamSource implements StreamSource {

        private final int column;
        private final ResultSet resultSet;

        public JdbcStreamSource(ResultSet resultSet, int column) {
            this.resultSet = resultSet;
            this.column = column;
        }

        @Override
        public ImageInputStream newImageInputStream() throws IOException {
            return ImageIO.createImageInputStream(newInputStream());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                return resultSet.getBinaryStream(column);
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(JdbcResolver.class);

    public static final String CONNECTION_TIMEOUT_CONFIG_KEY =
            "JdbcResolver.connection_timeout";
    public static final String JDBC_URL_CONFIG_KEY = "JdbcResolver.url";
    public static final String MAX_POOL_SIZE_CONFIG_KEY =
            "JdbcResolver.max_pool_size";
    public static final String PASSWORD_CONFIG_KEY = "JdbcResolver.password";
    public static final String USER_CONFIG_KEY = "JdbcResolver.user";

    private static HikariDataSource dataSource;

    static {
        try (Connection connection = getConnection()) {
            logger.info("Using {} {}", connection.getMetaData().getDriverName(),
                    connection.getMetaData().getDriverVersion());
            Configuration config = Application.getConfiguration();
            logger.info("Connection string: {}",
                    config.getString(JDBC_URL_CONFIG_KEY));
        } catch (SQLException e) {
            logger.error("Failed to establish a database connection", e);
        }
    }

    /**
     * @return Connection from the connection pool. Clients must
     * <code>close()</code> it when they are done with it.
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
            dataSource.setPoolName("JdbcResolverPool");
            dataSource.setMaximumPoolSize(maxPoolSize);
            dataSource.setConnectionTimeout(connectionTimeout);
        }
        return dataSource.getConnection();
    }

    @Override
    public StreamSource getStreamSource() throws IOException {
        try (Connection connection = getConnection()) {
            final String sql = getLookupSql();
            if (!sql.contains("?")) {
                throw new IOException("get_lookup_sql() implementation does " +
                        "not support prepared statements");
            }
            logger.debug(sql);

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, getDatabaseIdentifier());
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return new JdbcStreamSource(result, 1);
            }
        } catch (ScriptException | SQLException |
                DelegateScriptDisabledException e) {
            throw new IOException(e.getMessage(), e);
        }
        throw new FileNotFoundException();
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            try {
                // JdbcResolver.function.media_type may contain a JavaScript
                // function or null.
                String functionResult = getMediaType();
                String mediaType = null;
                if (functionResult != null) {
                    // the function result may be a media type, or an SQL
                    // statement to look it up.
                    if (functionResult.toUpperCase().contains("SELECT") &&
                            functionResult.toUpperCase().contains("FROM")) {
                        logger.debug(functionResult);
                        try (Connection connection = getConnection()) {
                            PreparedStatement statement = connection.
                                    prepareStatement(functionResult);
                            statement.setString(1, getDatabaseIdentifier());
                            ResultSet resultSet = statement.executeQuery();
                            if (resultSet.next()) {
                                mediaType = resultSet.getString(1);
                            }
                        }
                    } else {
                        mediaType = functionResult;
                    }
                } else {
                    mediaType = Format.getFormat(identifier).
                            getPreferredMediaType().toString();
                }
                sourceFormat = Format.getFormat(mediaType);
            } catch (ScriptException | SQLException |
                    DelegateScriptDisabledException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return sourceFormat;
    }

    /**
     * @return Result of the <code>getDatabaseIdentifier()</code> method.
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     * @throws IOException
     */
    public String getDatabaseIdentifier() throws IOException,
            ScriptException, DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String[] args = { identifier.toString() };
        final String method = "get_database_identifier";
        final Object result = engine.invoke(method, args);
        return (String) result;
    }

    /**
     * @return Result of the <code>get_lookup_sql</code> method.
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     * @throws IOException
     */
    public String getLookupSql() throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke("get_lookup_sql");
        return (String) result;
    }

    /**
     * @return Result of the <code>get_media_type</code> method.
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     * @throws IOException
     */
    public String getMediaType() throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke("get_media_type");
        return (String) result;
    }

}
