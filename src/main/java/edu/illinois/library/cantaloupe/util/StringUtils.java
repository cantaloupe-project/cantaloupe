package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    public static final String ASCII_FILENAME_UNSAFE_REGEX =
            "[^A-Za-z0-9\\-._ ]";
    // http://www.fileformat.info/info/unicode/category/index.htm
    public static final String UNICODE_FILENAME_UNSAFE_REGEX =
            "[^\\pL\\pM\\pN\\pS\\pZs\\-._ ]";

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
     * @param str String to hash.
     * @return    Lowercase MD5 checksum string.
     */
    public static String md5(String str) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(str.getBytes());
            byte[] bytes = digest.digest();
            return DatatypeConverter.printHexBinary(bytes).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return String representation of the given number with trailing zeroes
     *         removed.
     */
    public static String removeTrailingZeroes(double d) {
        String s = Float.toString((float) d);
        return !s.contains(".") ? s : s.replaceAll("0*$", "").
                replaceAll("\\.$", "");
    }

    /**
     * Recursively filters out {@code removeables} from the given dirty string.
     *
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
     * Recursively filters out {@code removeables} from the given dirty string,
     * replacing it with {@code replacement}.
     *
     * @return Sanitized string.
     */
    public static String sanitize(String dirty,
                                  final Pattern... removeables) {
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
     * Converts a string to a boolean. {@literal 1} and {@literal true} are
     * accepted as true; {@literal 0} and {@literal false} as false. All other
     * arguments throw a {@link NumberFormatException}, in contrast to {@link
     * Boolean#parseBoolean(String)}, which treats all non-true values as
     * false.
     *
     * @param str String to convert.
     * @return Boolean value of the given string.
     * @throws NumberFormatException if the string has an unrecognized format.
     */
    public static boolean toBoolean(@Nullable String str) {
        if (str == null) {
            throw new NumberFormatException();
        }

        switch (str) {
            case "1":
            case "true":
                return true;
            case "0":
            case "false":
                return false;
            default:
                throw new NumberFormatException();
        }
    }

    /**
     * The following byte size formats are supported:
     *
     * <ul>
     *     <li>Whole number</li>
     *     <li>Decimal number suffixed with {@literal K}, {@literal KB},
     *     {@literal M}, {@literal MB}, {@literal G}, {@literal GB},
     *     {@literal T}, {@literal TB}, {@literal P}, {@literal PB}
     *         <ul>
     *             <li>Lowercase suffixes are allowed.</li>
     *             <li>Spaces are allowed between the number and suffix.</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param str String byte size.
     * @return    If the given string had no units, a long representation.
     *            Otherwise, a power of {@literal 1024}.
     */
    public static long toByteSize(String str) {
        str = str.toUpperCase();
        final String numberStr = str.replaceAll("[^\\d.]", "");
        final double number = Double.parseDouble(numberStr);
        short exponent = 0;

        if (str.endsWith("K") || str.endsWith("KB")) {
            exponent = 1;
        } else if (str.endsWith("M") || str.endsWith("MB")) {
            exponent = 2;
        } else if (str.endsWith("G") || str.endsWith("GB")) {
            exponent = 3;
        } else if (str.endsWith("T") || str.endsWith("TB")) {
            exponent = 4;
        } else if (str.endsWith("P") || str.endsWith("PB")) { // you never know
            exponent = 5;
        }
        return Math.round(number * Math.pow(1024, exponent));
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
