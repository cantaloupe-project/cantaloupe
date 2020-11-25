package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

import javax.xml.bind.DatatypeConverter;
import java.awt.FontMetrics;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            digest.update(str.getBytes(StandardCharsets.UTF_8));
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
     * @param str String to reverse.
     * @return Reversed string.
     */
    public static String reverse(String str) {
        return new StringBuilder(str).reverse().toString();
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
    public static boolean toBoolean(String str) {
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

    /**
     * Returns an array of strings, one for each line in the string after it
     * has been wrapped to fit lines of <var>maxWidth</var>. Lines end with any
     * of CR, LF, or CRLF. A line ending at the end of the string will not
     * output a further, empty string.
     *
     * @param str      The string to split. Must not be {@code null}.
     * @param fm       Used for string width calculations.
     * @param maxWidth The max line width, in points.
     * @return         List of strings.
     */
    public static List<String> wrap(String str, FontMetrics fm, int maxWidth) {
        final List<String> lines = Arrays.asList(str.split("\\r?\\n"));
        if (lines.isEmpty()) {
            return lines;
        }
        // Remove the last list element, if it is blank.
        final int lastIndex = lines.size() - 1;
        if (lines.get(lastIndex).isBlank()) {
            lines.remove(lastIndex);
        }
        final List<String> strings = new ArrayList<>();
        lines.forEach(line -> wrapLineInto(line, strings, fm, maxWidth));
        return strings;
    }

    /**
     * Given a line of text and font metrics information, wrap the line and add
     * the new line(s) to <var>list</var>.
     *
     * @param line     A line of text.
     * @param list     An output list of strings.
     * @param fm       Font metrics.
     * @param maxWidth Maximum width of the line(s).
     * @author         <a href="mailto:jimm@io.com">Jim Menard</a>
     */
    private static void wrapLineInto(String line,
                                     List<String> list,
                                     FontMetrics fm,
                                     int maxWidth) {
        int len = line.length();
        int width;
        while (len > 0 && (width = fm.stringWidth(line)) > maxWidth) {
            // Guess where to split the line. Look for the next space before
            // or after the guess.
            int guess     = len * maxWidth / width;
            String before = line.substring(0, guess).trim();

            width = fm.stringWidth(before);
            int pos;
            if (width > maxWidth) { // Too long
                pos = findBreakBefore(line, guess);
            } else { // Too short or possibly just right
                pos = findBreakAfter(line, guess);
                if (pos != -1) { // Make sure this doesn't make us too long
                    before = line.substring(0, pos).trim();
                    if (fm.stringWidth(before) > maxWidth) {
                        pos = findBreakBefore(line, guess);
                    }
                }
            }
            if (pos == -1) {
                pos = guess; // Split in the middle of the word
            }
            list.add(line.substring(0, pos).trim());
            line = line.substring(pos).trim();
            len  = line.length();
        }
        if (len > 0) {
            list.add(line);
        }
    }

    /**
     * @param line A string.
     * @param start Where to start looking.
     * @return The index of the first whitespace character or {@code -} in
     *         <var>line</var> that is at or before <var>start</var>. Returns
     *         {@code -1} if no such character is found.
     */
    private static int findBreakBefore(String line, int start) {
        for (int i = start; i >= 0; --i) {
            char c = line.charAt(i);
            if (Character.isWhitespace(c) || c == '-') {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param line A string.
     * @param start Where to start looking.
     * @return The index of the first whitespace character or {@code -} in
     *         <var>line</var> that is at or after <var>start</var>. Returns
     *         {@code -1} if no such character is found.
     */
    private static int findBreakAfter(String line, int start) {
        int len = line.length();
        for (int i = start; i < len; ++i) {
            char c = line.charAt(i);
            if (Character.isWhitespace(c) || c == '-') {
                return i;
            }
        }
        return -1;
    }

    private StringUtils() {}

}
