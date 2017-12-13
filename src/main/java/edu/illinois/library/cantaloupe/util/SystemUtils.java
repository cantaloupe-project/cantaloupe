package edu.illinois.library.cantaloupe.util;

import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;

import java.util.regex.Pattern;

public final class SystemUtils {

    /**
     * @return Java major version such as <code>8</code>, <code>9</code>, etc.
     */
        // Up to Java 8, this will be a string like: 1.8.0_60
        // Beginning in Java 9, it will be an integer like 9.
    public static int getJavaMajorVersion() {
        final String versionStr = System.getProperty("java.version");
        if (versionStr.contains(".")) {
            String[] parts = versionStr.split(Pattern.quote("."));
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
        }
        return Integer.parseInt(versionStr);
    }

    /**
     * ALPN is built into Java 9. In earlier versions, it has to be provided by
     * a JAR on the boot classpath:
     *
     * <code>-Xbootclasspath/p:/path/to/alpn-boot-8.1.5.v20150921.jar</code>
     */
    public static boolean isALPNAvailable() {
        if (getJavaMajorVersion() < 9) {
            try {
                NegotiatingServerConnectionFactory.
                        checkProtocolNegotiationAvailable();
            } catch (IllegalStateException e) {
                return false;
            }
        }
        return true;
    }

    private SystemUtils() {}

}
