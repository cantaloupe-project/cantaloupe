package edu.illinois.library.cantaloupe.util;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(StringUtil.class);

    public static final String FILENAME_REGEX = "[^A-Za-z0-9._-]";

    /**
     * Returns a filename-safe string guaranteed to uniquely represent the
     * given string.
     *
     * @return Filename-safe representation of the given string.
     */
    public static String filesystemSafe(String str) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(str.getBytes(Charset.forName("UTF8")));
            return Hex.encodeHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("filenameSafe(): {}", e.getMessage());
        }
        return str; // This should never hit.
    }

    /**
     * @param f
     * @return String representation of the given float with trailing zeroes
     *         removed.
     */
    public static String removeTrailingZeroes(Float f) {
        String s = f.toString();
        return s.indexOf(".") < 0 ? s : s.replaceAll("0*$", "").
                replaceAll("\\.$", "");
    }

    /**
     * Recursively filters out <code>removeables</code> from the given dirty
     * string.
     *
     * @param dirty
     * @param removeables
     * @return Sanitized string.
     */
    public static String sanitize(String dirty, final String... removeables) {
        for (String toRemove : removeables) {
            if (dirty.contains(toRemove)) {
                dirty = dirty.replace(toRemove, "");
                dirty = sanitize(dirty, removeables);
            }
        }
        return dirty;
    }

    /**
     * Recursively filters out <code>removeables</code> from the given dirty
     * string.
     *
     * @param dirty
     * @param removeables
     * @return Sanitized string.
     */
    public static String sanitize(String dirty, final Pattern... removeables) {
        for (Pattern toRemove : removeables) {
            Matcher matcher = toRemove.matcher(dirty);
            if (matcher.find()) {
                dirty = dirty.replaceAll(toRemove.pattern(), "");
                dirty = sanitize(dirty, removeables);
            }
        }
        return dirty;
    }

    private StringUtil() {}

}
