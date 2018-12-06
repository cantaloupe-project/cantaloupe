package edu.illinois.library.cantaloupe.http;

import java.util.Arrays;

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
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Range) {
            Range other = (Range) obj;
            return start == other.start && end == other.end &&
                    length == other.length;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new long[] { start, end, length });
    }

    @Override
    public String toString() {
        return String.format("bytes=%d-%d/%d", start, end, length);
    }

}
