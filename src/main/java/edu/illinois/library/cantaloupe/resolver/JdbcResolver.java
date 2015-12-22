package edu.illinois.library.cantaloupe.resolver;

import com.zaxxer.hikari.HikariDataSource;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import org.apache.commons.configuration.Configuration;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class JdbcResolver extends AbstractResolver implements ChannelResolver {

    private static Logger logger = LoggerFactory.getLogger(JdbcResolver.class);

    private static final String CONNECTION_STRING_CONFIG_KEY =
            "JdbcResolver.connection_string";
    public static final String CONNECTION_TIMEOUT_CONFIG_KEY =
            "JdbcResolver.connection_timeout";
    private static final String IDENTIFIER_FUNCTION_CONFIG_KEY =
            "JdbcResolver.function.identifier";
    private static final String LOOKUP_SQL_CONFIG_KEY =
            "JdbcResolver.lookup_sql";
    public static final String MAX_POOL_SIZE_CONFIG_KEY =
            "JdbcResolver.max_pool_size";
    private static final String MEDIA_TYPE_FUNCTION_CONFIG_KEY =
            "JdbcResolver.function.media_type";
    private static final String PASSWORD_CONFIG_KEY = "JdbcResolver.password";
    private static final String USER_CONFIG_KEY = "JdbcResolver.user";

    private static HikariDataSource dataSource;

    static {
        try (Connection connection = getConnection()) {
            logger.info("Using {} {}", connection.getMetaData().getDriverName(),
                    connection.getMetaData().getDriverVersion());
            Configuration config = Application.getConfiguration();
            logger.info("Connection string: {}",
                    config.getString(CONNECTION_STRING_CONFIG_KEY));
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
                    getString(CONNECTION_STRING_CONFIG_KEY, "");
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
    public ReadableByteChannel getChannel(Identifier identifier)
            throws IOException {
        try (Connection connection = getConnection()) {
            Configuration config = Application.getConfiguration();
            String sql = config.getString(LOOKUP_SQL_CONFIG_KEY);
            if (!sql.contains("?")) {
                throw new IOException(LOOKUP_SQL_CONFIG_KEY +
                        " does not support prepared statements");
            }
            logger.debug(sql);

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, executeGetDatabaseIdentifier(identifier));
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return Channels.newChannel(result.getBinaryStream(1));
            }
        } catch (ScriptException | SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
        throw new FileNotFoundException();
    }

    @Override
    public SourceFormat getSourceFormat(Identifier identifier)
            throws IOException {
        try {
            // JdbcResolver.function.media_type may contain a JavaScript
            // function or null.
            String functionResult = executeGetMediaType(identifier);
            MediaType mediaType = null;
            if (functionResult != null) {
                // the function result may be a media type, or an SQL
                // statement to look it up.
                if (functionResult.toUpperCase().contains("SELECT") &&
                        functionResult.toUpperCase().contains("FROM")) {
                    logger.debug(functionResult);
                    try (Connection connection = getConnection()) {
                        PreparedStatement statement = connection.
                                prepareStatement(functionResult);
                        statement.setString(1, executeGetDatabaseIdentifier(identifier));
                        ResultSet resultSet = statement.executeQuery();
                        if (resultSet.next()) {
                            mediaType = new MediaType(resultSet.getString(1));
                        }
                    }
                } else {
                    mediaType = new MediaType(functionResult);
                }
            } else {
                mediaType = SourceFormat.getSourceFormat(identifier).
                        getPreferredMediaType();
            }
            return SourceFormat.getSourceFormat(mediaType);
        } catch (ScriptException | SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * @param identifier
     * @return Result of the <code>getDatabaseIdentifier()</code> function.
     * @throws ScriptException
     * @throws SQLException
     */
    public String executeGetDatabaseIdentifier(Identifier identifier)
            throws ScriptException {
        Configuration config = Application.getConfiguration();
        final String statement = String.format("%s\ngetDatabaseIdentifier(\"%s\")",
                config.getString(IDENTIFIER_FUNCTION_CONFIG_KEY), identifier);
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        return (String) engine.eval(statement);
    }

    /**
     * @param identifier
     * @return Result of the <code>getMediaType()</code> function.
     * @throws ScriptException
     * @throws SQLException
     */
    public String executeGetMediaType(Identifier identifier)
            throws ScriptException, SQLException {
        Configuration config = Application.getConfiguration();
        String function = config.getString(MEDIA_TYPE_FUNCTION_CONFIG_KEY);
        if (function != null && function.length() > 0) {
            final String statement = String.format("%s\ngetMediaType(\"%s\")",
                    function, identifier);
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("js");
            return (String) engine.eval(statement);
        }
        return null;
    }

}
