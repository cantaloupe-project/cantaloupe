package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.source.Source;

/**
 * <p>Collection of information associated with a successful image request.
 * Instances are "ledger entries" of pairs of sources and processors that have
 * been used to fulfill a request successfully.</p>
 */
final class SourceUsage {

    private final Source source;

    SourceUsage(Source source) {
        this.source = source;
    }

    /**
     * @return {@code true} only if the {@link #getSource() source} is equal.
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
        return source.getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s -> %s",
                source.getIdentifier(),
                source.getClass().getName());
    }

}
