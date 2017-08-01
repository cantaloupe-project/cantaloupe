package edu.illinois.library.cantaloupe.util;

import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;

public abstract class SystemUtils {

    public static double getJavaVersion() {
        final String versionStr = System.getProperty("java.version");
        int pos = versionStr.indexOf('.');
        pos = versionStr.indexOf('.', pos + 1);
        return Double.parseDouble(versionStr.substring (0, pos));
    }

    /**
     * ALPN is built into Java 9. In earlier versions, it has to be provided by
     * a JAR on the boot classpath:
     *
     * <code>-Xbootclasspath/p:/path/to/alpn-boot-8.1.5.v20150921.jar</code>
     */
    public static boolean isALPNAvailable() {
        if (getJavaVersion() < 1.9) {
            try {
                NegotiatingServerConnectionFactory.
                        checkProtocolNegotiationAvailable();
            } catch (IllegalStateException e) {
                return false;
            }
        }
        return true;
    }

}
