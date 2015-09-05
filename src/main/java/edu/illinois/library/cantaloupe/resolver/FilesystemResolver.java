package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FilesystemResolver extends AbstractResolver implements Resolver {

    public InputStream resolve(String identifier) {
        try {
            return new FileInputStream(getPath(identifier));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private String getPath(String identifier) {
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
