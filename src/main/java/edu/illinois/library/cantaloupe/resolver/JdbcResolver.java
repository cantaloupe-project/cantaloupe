package edu.illinois.library.cantaloupe.resolver;

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
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class JdbcResolver implements StreamResolver {

    private static Logger logger = LoggerFactory.getLogger(JdbcResolver.class);
    private static Connection connection;

    static {
        try {
            Connection connection = getConnection();
            logger.info("Using {} {}", connection.getMetaData().getDriverName(),
                    connection.getMetaData().getDriverVersion());
            Configuration config = Application.getConfiguration();
            logger.info("Connection string: {}",
                    config.getString("JdbcResolver.connection_string"));
        } catch (SQLException e) {
            logger.error("Failed to establish a database connection", e);
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null) {
            Configuration config = Application.getConfiguration();
            final String connectionString = config.
                    getString("JdbcResolver.connection_string", "");
            final String user = config.getString("JdbcResolver.user", "");
            final String password = config.getString("JdbcResolver.password", "");
            connection = DriverManager.getConnection(connectionString, user,
                    password);
        }
        return connection;
    }

    @Override
    public InputStream getInputStream(Identifier identifier)
            throws IOException {
        try {
            Configuration config = Application.getConfiguration();
            String sql = config.getString("JdbcResolver.lookup_sql");
            if (!sql.contains("?")) {
                throw new IOException("JdbcResolver.lookup_sql does not " +
                        "support prepared statements");
            }
            logger.debug(sql);

            PreparedStatement statement = getConnection().prepareStatement(sql);
            statement.setString(1, executeGetDatabaseIdentifier(identifier));
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getBinaryStream(1);
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
                if (functionResult.toUpperCase().contains("SELECT ") &&
                        functionResult.toUpperCase().contains("FROM ")) {
                    logger.debug(functionResult);
                    PreparedStatement statement = getConnection().
                            prepareStatement(functionResult);
                    statement.setString(1, executeGetDatabaseIdentifier(identifier));
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        mediaType = new MediaType(resultSet.getString(1));
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

    public String executeGetDatabaseIdentifier(Identifier identifier)
            throws ScriptException {
        Configuration config = Application.getConfiguration();
        final String statement = String.format("%s\ngetDatabaseIdentifier(\"%s\")",
                config.getString("JdbcResolver.function.identifier"),
                identifier);
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        return (String) engine.eval(statement);
    }

    public String executeGetMediaType(Identifier identifier) throws ScriptException,
            SQLException {
        Configuration config = Application.getConfiguration();
        String function = config.getString("JdbcResolver.function.media_type");
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
