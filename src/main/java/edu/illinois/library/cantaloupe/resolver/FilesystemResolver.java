package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

public class FilesystemResolver implements Resolver {

    public String resolve(String identifier) {
        String str = getPathPrefix() + "/" + identifier + "/" + getPathSuffix();
        return StringUtils.stripEnd(str, "/");
    }

    private String getPathPrefix() {
        String prefix;
        try {
            prefix = Application.getConfiguration().getString("fs_resolver.path_prefix");
        } catch (ConfigurationException e) {
            return "";
        }
        return prefix;
    }

    private String getPathSuffix() {
        String suffix;
        try {
            suffix = Application.getConfiguration().getString("fs_resolver.path_suffix");
        } catch (ConfigurationException e) {
            return "";
        }
        return suffix;
    }

}
