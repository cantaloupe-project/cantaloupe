package edu.illinois.library.cantaloupe.http;

/**
 * HTTP byte range.
 */
public final class Range {

    public long start, end, length;

    public Range() {}

    public Range(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public Range(long start, long end, long length) {
        this(start, end);
        this.length = length;
    }

    @Override
    public String toString() {
        return String.format("bytes=%d-%d/%d", start, end, length);
    }

}
