package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.source.Source;

import java.util.Arrays;

/**
 * <p>Collection of information associated with a successful image request.
 * Instances are "ledger entries" of pairs of sources and processors that have
 * been used to fulfill a request successfully.</p>
 */
final class SourceProcessorPair {

    private Source source;
    private String processorName;
    private OperationList opList;

    SourceProcessorPair(Source source,
                        String processorName,
                        OperationList opList) {
        this.source        = source;
        this.processorName = processorName;
        this.opList        = opList;
    }

    /**
     * @return {@code true} only if the {@link #getSource() source} and {@link
     *         #getProcessorName() processor names} are equal. (Note: the
     *         {@link #getOperationList() operation list} is not considered.)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof SourceProcessorPair) {
            SourceProcessorPair other = (SourceProcessorPair) obj;
            return source.getClass().equals(other.source.getClass()) &&
                    processorName.equals(other.processorName);
        }
        return super.equals(obj);
    }

    OperationList getOperationList() {
        return opList;
    }

    String getProcessorName() {
        return processorName;
    }

    Source getSource() {
        return source;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new int[] {
                source.getClass().hashCode(),
                processorName.hashCode()
        });
    }

    @Override
    public String toString() {
        return String.format("%s -> %s -> %s",
                source.getIdentifier(),
                source.getClass().getName(),
                processorName);
    }

}
