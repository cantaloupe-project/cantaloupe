package edu.illinois.library.cantaloupe.cache;

import java.io.OutputStream;

/**
 * Custom {@link OutputStream} that adds a {@code completable} flag that may be
 * set to {@code true} after the stream has been fully written.
 *
 * @since 4.1.9
 */
public abstract class CompletableOutputStream extends OutputStream {

    private boolean isCompletelyWritten;

    public boolean isCompletelyWritten() {
        return isCompletelyWritten;
    }

    public void setCompletelyWritten(boolean isCompletelyWritten) {
        this.isCompletelyWritten = isCompletelyWritten;
    }

}
