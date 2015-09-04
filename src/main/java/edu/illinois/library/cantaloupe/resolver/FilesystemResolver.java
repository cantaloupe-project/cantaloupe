package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FilesystemResolver implements Resolver {

    public InputStream resolve(String identifier) {
        try {
            return new FileInputStream(getPath(identifier));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private String getPath(String identifier) {
        return StringUtils.stripEnd(getPathPrefix() + "/" + identifier + "/" +
                getPathSuffix(), "/");
    }

    private String getPathPrefix() {
        String prefix;
        try {
            prefix = Application.getConfiguration().
                    getString("FilesystemResolver.path_prefix");
        } catch (ConfigurationException e) {
            return "";
        }
        return prefix;
    }

    private String getPathSuffix() {
        String suffix;
        try {
            suffix = Application.getConfiguration().
                    getString("FilesystemResolver.path_suffix");
        } catch (ConfigurationException e) {
            return "";
        }
        return suffix;
    }

}
