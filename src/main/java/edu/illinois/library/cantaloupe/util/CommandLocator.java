package edu.illinois.library.cantaloupe.util;

import java.io.File;

public final class CommandLocator {

    /**
     * @param binaryName Binary name, excluding path information.
     * @param searchPath Path in which the binary may be expected to reside.
     *                   May be {@literal null} or empty.
     * @return           Most specific path to {@code binaryName}.
     */
    public static String locate(String binaryName, String searchPath) {
        if (searchPath != null && !searchPath.isEmpty()) {
            // We are expecting searchPath to be a directory path, but it may
            // also be a full path to binaryName.
            if (searchPath.endsWith(binaryName) &&
                    searchPath.length() > binaryName.length()) {
                return searchPath;
            } else {
                searchPath = StringUtils.stripEnd(searchPath, File.separator);
                searchPath = StringUtils.stripEnd(searchPath, binaryName);
            }
            return searchPath + File.separator + binaryName;
        }
        // No search path provided, so fall back to the PATH.
        return binaryName;
    }

    private CommandLocator() {}

}
