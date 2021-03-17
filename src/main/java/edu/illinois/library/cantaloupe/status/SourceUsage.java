package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.source.Source;

import java.util.Objects;

/**
 * Encapsulates a successful use of a {@link Source}.
 *
 * @since 5.0
 */
final class SourceUsage {

    private final Source source;

    SourceUsage(Source source) {
        this.source = source;
    }

    /**
     * @return True of the given instance's {@link #getSource() source} is of
     *         the same class.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof SourceUsage) {
            SourceUsage other = (SourceUsage) obj;
            return source.getClass().equals(other.source.getClass());
        }
        return super.equals(obj);
    }

    Source getSource() {
        return source;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(source.getClass());
    }

    @Override
    public String toString() {
        return String.format("%s -> %s",
                source.getIdentifier(),
                source.getClass().getName());
    }

}
