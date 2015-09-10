package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FilesystemResolver implements Resolver {

    private static Logger logger = LoggerFactory.
            getLogger(FilesystemResolver.class);

    public File getFile(String identifier) throws FileNotFoundException {
        File file = new File(getPath(identifier));
        if (!file.exists()) {
            String message = "Failed to resolve " + identifier + " to " +
                    file.getAbsolutePath();
            logger.warn(message);
            throw new FileNotFoundException(message);
        }
        logger.debug("Resolved {} to {}", identifier, file.getAbsolutePath());
        return file;
    }

    public InputStream getInputStream(String identifier)
            throws FileNotFoundException {
        return new FileInputStream(getPath(identifier));
    }

    public String getPath(String identifier) {
        Configuration config = Application.getConfiguration();
        String prefix = config.getString("FilesystemResolver.path_prefix");
        if (prefix == null) {
            prefix = "";
        }
        String suffix = config.getString("FilesystemResolver.path_suffix");
        if (suffix == null) {
            suffix = "";
        }
        return prefix + identifier + suffix;
    }

    public SourceFormat getSourceFormat(String identifier) {
        // try to get the source format based on a filename extension in the
        // identifier
        String extension = null;
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        int i = identifier.lastIndexOf('.');
        if (i > 0) {
            extension = identifier.substring(i + 1);
        }
        if (extension != null) {
            for (SourceFormat enumValue : SourceFormat.values()) {
                if (enumValue.getExtensions().contains(extension)) {
                    sourceFormat = enumValue;
                    break;
                }
            }
        }
        // TODO: if that failed (or maybe even instead of doing that), read the magic byte in the file header
        return sourceFormat;
    }

}
