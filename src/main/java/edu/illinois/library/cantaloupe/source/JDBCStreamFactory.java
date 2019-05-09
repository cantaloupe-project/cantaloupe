package edu.illinois.library.cantaloupe.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * StreamFactory for binary a.k.a. BLOB column values.
 */
class JDBCStreamFactory implements StreamFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JDBCStreamFactory.class);

    private String sql;
    private String databaseIdentifier;

    JDBCStreamFactory(String sql, String databaseIdentifier) {
        this.sql = sql;
        this.databaseIdentifier = databaseIdentifier;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        LOGGER.debug(sql);
        try (Connection connection = JdbcSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, databaseIdentifier);

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
