package edu.illinois.library.cantaloupe.util;

import java.util.regex.Pattern;

public final class SystemUtils {

    /**
     * @return Java major version such as <code>8</code>, <code>9</code>, etc.
     */
    public static int getJavaMajorVersion() {
        final String versionStr = System.getProperty("java.version");
        return parseJavaMajorVersion(versionStr);
    }

    /**
     * @param version Value of the <code>java.version</code> system property.
     * @return Major version.
     */
    static int parseJavaMajorVersion(String version) {
        // Up to Java 8, this will be a string like: "1.8.0_60"
        // Beginning in Java 9, it will be a string like "9", "9.0.1", etc.
        if (version.startsWith("1.")) { // < 9
            String[] parts = version.split(Pattern.quote("."));
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
        }
        if (version.contains(".")) { // 9.x, 9.x.x, 10.x, 10.x.x, etc.
            return Integer.parseInt(version.substring(0, version.indexOf(".")));
        }
        return Integer.parseInt(version); // 9, 10, etc.
    }

    /**
     * ALPN is built into Java 9 and later.
     */
    public static boolean isALPNAvailable() {
        return getJavaMajorVersion() >= 9;
    }

    private SystemUtils() {}

}
