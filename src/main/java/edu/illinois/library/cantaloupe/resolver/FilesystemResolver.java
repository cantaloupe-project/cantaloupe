package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FilesystemResolver implements Resolver {

    public File getFile(String identifier) throws FileNotFoundException {
        File file = new File(getPath(identifier));
        if (!file.exists()) {
            throw new FileNotFoundException(
                    "File not found:" + file.getAbsolutePath());
        }
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

}
