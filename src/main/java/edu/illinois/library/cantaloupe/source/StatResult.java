package edu.illinois.library.cantaloupe.source;

import java.time.Instant;

/**
 * Holds some limited metadata about a source image.
 */
public final class StatResult {

    private Instant lastModified;

    public Instant getLastModified() {
        return lastModified;
    }

    void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

}
