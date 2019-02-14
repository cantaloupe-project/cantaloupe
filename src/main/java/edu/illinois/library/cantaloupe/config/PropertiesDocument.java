package edu.illinois.library.cantaloupe.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Properties document, preserving comments, whitespace, and key order.
 * The intent is to provide a functional replacement of Commons Configuration's
 * properties support in order to avoid awkwardness of packaging that
 * dependency into the application's weird dual JAR/WAR file.</p>
 *
 * <p>Multi-line strings are not supported.</p>
 *
 * <p>UTF-8-encoded documents are supported, even in JDK versions prior to 9,
 * when ISO 8859-1 was the only encoding supported by {@link
 * java.util.Properties}.</p>
 *
 * @author Alex Dolski UIUC
 * @since 4.1
 */
class PropertiesDocument {

    static abstract class Item {}

    static class EmptyLine extends Item {
        private static final String STRING_REPRESENTATION = "";

        @Override
        public String toString() {
            return STRING_REPRESENTATION;
        }
    }

    static class Comment extends Item {
        private int offset;
        private String character, comment;

        String comment() {
            return comment;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < offset; i++) {
                builder.append(SPACE);
            }
            builder.append(character);
            builder.append(comment);
            return builder.toString();
        }
    }

    static class KeyValuePair extends Item {
        /** a.k.a. left indent */
        private int offset;
        private String key, normalizedKey,
                pairSeparator = PAIR_SEPARATOR_1, value;

        KeyValuePair() {}

        KeyValuePair(String key, String value) {
            setKey(key);
            setValue(value);
        }

        /**
         * @return Key potentially with surrounding whitespace.
         */
        String key() {
            return key;
        }

        String value() {
            return value;
        }

        void setKey(String key) {
            this.key = key;
        }

        void setPairSeparator(String pairSeparator) {
            this.pairSeparator = pairSeparator;
        }

        void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < offset; i++) {
                builder.append(SPACE);
            }
            builder.append(key);
            builder.append(pairSeparator);
            builder.append(value);
            return builder.toString();
        }
    }

    private static final String COMMENT_1        = "#";
    private static final String COMMENT_2        = "!";
    private static final String PAIR_SEPARATOR_1 = "=";
    private static final String PAIR_SEPARATOR_2 = ":";
    private static final String SPACE            = " ";

    /**
     * Used for grabbing the pair separator with surrounding space intact.
     */
    private static final Pattern PAIR_SEPARATOR_PATTERN =
            Pattern.compile("(\\s*[=:]\\s*)");

    private final List<Item> items = new ArrayList<>(Key.values().length + 20);

    void clear() {
        items.clear();
    }

    /**
     * Clears all document keys matching the given key.
     */
    void clearKey(String key) {
        new ArrayList<>(items).stream()
                .filter(item -> item instanceof KeyValuePair &&
                        key.equals(((KeyValuePair) item).key()))
                .forEach(items::remove);
    }

    boolean containsKey(String key) {
        return items.stream()
                .anyMatch(it -> it instanceof KeyValuePair &&
                        key.equals(((KeyValuePair) it).key()));
    }

    /**
     * @return Value for the given key.
     */
    String get(String key) {
        return items.stream()
                .filter(it -> it instanceof KeyValuePair &&
                        key.equals(((KeyValuePair) it).key()))
                .findFirst()
                .map(it -> ((KeyValuePair) it).value().trim().replaceAll("\\\\+", "\\\\"))
                .orElse(null);
    }

    Iterator<String> getKeys() {
        return items.stream()
                .filter(it -> it instanceof KeyValuePair)
                .map(it -> ((KeyValuePair) it).key())
                .iterator();
    }

    List<Item> items() {
        return items;
    }

    /**
     * Sets the given key to the given value, maintaining its position in the
     * document. If the given key does not already exist in the document, it is
     * appended to the end.
     */
    void set(String key, String value) {
        final KeyValuePair newPair = new KeyValuePair(key, value);
        boolean found = false;
        for (int i = 0, count = items.size(); i < count; i++) {
            Item item = items.get(i);
            if (item instanceof KeyValuePair) {
                KeyValuePair pair = (KeyValuePair) item;
                if (key.equals(pair.key())) {
                    found = true;
                    newPair.pairSeparator = pair.pairSeparator;
                    items.set(i, newPair);
                    break;
                }
            }
        }
        if (!found) {
            items.add(newPair);
        }
    }

    void load(Path file) throws IOException {
        items.clear();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            parse(reader);
        }
    }

    private void parse(BufferedReader reader) throws IOException {
        String line;
        int lineNum = 0;
        while ((line = reader.readLine()) != null) {
            final String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                items.add(new EmptyLine());
            } else if (trimmedLine.matches("^[#!].*")) {
                Comment comment   = new Comment();
                comment.offset    = Math.min(
                        line.indexOf(COMMENT_1),
                        line.indexOf(COMMENT_2));
                comment.character = trimmedLine.substring(0, 1);
                comment.comment   = trimmedLine.substring(1);
                items.add(comment);
            } else {
                int i = trimmedLine.indexOf(PAIR_SEPARATOR_1);
                if (i < 0) {
                    i = trimmedLine.indexOf(PAIR_SEPARATOR_2);
                }
                if (i > 0) {
                    KeyValuePair pair = new KeyValuePair();
                    // Read the pair's left-indentation.
                    for (int s = 0; s < line.length() - 1; s++) {
                        if (SPACE.equals(line.substring(s, s + 1))) {
                            pair.offset++;
                        } else {
                            break;
                        }
                    }
                    pair.setKey(trimmedLine.substring(0, i).trim());
                    pair.setValue(trimmedLine.substring(i + 1).trim());

                    final Matcher matcher =
                            PAIR_SEPARATOR_PATTERN.matcher(trimmedLine);
                    if (matcher.find()) {
                        pair.setPairSeparator(matcher.group(0));
                    }

                    items.add(pair);
                } else {
                    throw new IOException(
                            "Line " + lineNum + " is malformed.");
                }
            }
            lineNum++;
        }
    }

    void save(Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Item item : items) {
                writer.write(item.toString());
                writer.newLine();
            }
        }
    }

    int size() {
        return items.size();
    }

}
