package edu.illinois.library.cantaloupe.util;

public abstract class SystemUtils {

    public static double getJavaVersion() {
        final String versionStr = System.getProperty("java.version");
        int pos = versionStr.indexOf('.');
        pos = versionStr.indexOf('.', pos + 1);
        return Double.parseDouble(versionStr.substring (0, pos));
    }

}
