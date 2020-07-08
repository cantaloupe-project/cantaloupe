package edu.illinois.library.cantaloupe.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SystemUtils {

    private static final Pattern JAVA_VERSION_PATTERN =
            Pattern.compile("^(\\d+)");

    /**
     * @return Java major version such as <code>8</code>, <code>9</code>, etc.
     */
    public static int getJavaMajorVersion() {
        final String versionStr = System.getProperty("java.version");
        return parseJavaMajorVersion(versionStr);
    }

    /**
     * @param version Value of the {@code java.version} system property.
     * @return Major version.
     * @throws IllegalArgumentException if the argument is illegal, which would
     *         probably indicate a bug.
     */
    static int parseJavaMajorVersion(String version) {
        // Up to Java 8, this will be a string like: "1.8.0_60"
        // Beginning in Java 9, it will be a string like "9", "9.0.1", etc.
        // See: https://www.oracle.com/java/technologies/javase/versioning-naming.html
        if (version.startsWith("1.")) { // <= 8
            String[] parts = version.split(Pattern.quote("."));
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
        }
        Matcher matcher = JAVA_VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Unrecognized version: " + version);
    }

    /**
     * ALPN is built into Java 9 and later.
     */
    public static boolean isALPNAvailable() {
        return getJavaMajorVersion() >= 9;
    }

    private SystemUtils() {}

}
