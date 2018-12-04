package edu.illinois.library.cantaloupe.http;

/**
 * HTTP byte range.
 */
public final class Range {

    public long start, end, length;

    @Override
    public String toString() {
        return String.format("bytes=%d-%d/%d", start, end, length);
    }

}
