package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(StringUtils.class);

    public static final String FILENAME_REGEX = "[^A-Za-z0-9._-]";

    /**
     * Some web servers have issues dealing with encoded slashes ({@literal
     * %2F}) in URIs. This method enables the use of an alternate string to
     * represent a slash via {@link Key#SLASH_SUBSTITUTE}.
     *
     * @param uriPathComponent Path component (a part of the path before,
     *                         after, or between slashes).
     * @return                 Path component with slashes decoded.
     */
    public static String decodeSlashes(final String uriPathComponent) {
        final String substitute = Configuration.getInstance().
                getString(Key.SLASH_SUBSTITUTE, "");
        if (!substitute.isEmpty()) {
            return org.apache.commons.lang3.StringUtils.replace(
                    uriPathComponent, substitute, "/");
        }
        return uriPathComponent;
    }

    public static String escapeHTML(String html) {
        StringBuilder out = new StringBuilder(Math.max(16, html.length()));
        for (int i = 0, length = html.length(); i < length; i++) {
            char c = html.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

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
     * @param d
     * @return String representation of the given number with trailing zeroes
     *         removed.
     */
    public static String removeTrailingZeroes(double d) {
        String s = Float.toString((float) d);
        return !s.contains(".") ? s : s.replaceAll("0*$", "").
                replaceAll("\\.$", "");
    }

    /**
     * Recursively filters out {@literal removeables} from the given dirty
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
     * Recursively filters out {@literal removeables} from the given dirty
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

    /**
     * Strips a string from the end of another string.
     *
     * @param str     String to search.
     * @param toStrip String to strip off the end of the search string.
     */
    public static String stripEnd(String str, String toStrip) {
        final int expectedIndex = str.length() - toStrip.length();
        final int lastIndex = str.lastIndexOf(toStrip);
        if (expectedIndex >= 0 && lastIndex == expectedIndex) {
            return str.substring(0, expectedIndex);
        }
        return str;
    }

    /**
     * Strips a string from the beginning of another string.
     *
     * @param str     String to search.
     * @param toStrip String to strip off the beginning of the search string.
     */
    public static String stripStart(String str, String toStrip) {
        if (str.indexOf(toStrip) == 0) {
            return str.substring(toStrip.length());
        }
        return str;
    }

    /**
     * Strips any enclosing tags or other content around the {@literal rdf:RDF}
     * element within an RDF/XML XMP string.
     */
    public static String trimXMP(String xmp) {
        final int start = xmp.indexOf("<rdf:RDF");
        final int end = xmp.indexOf("</rdf:RDF");
        if (start > -1 && end > -1) {
            xmp = xmp.substring(start, end + 10);
        }
        return xmp;
    }

    private StringUtils() {}

}
