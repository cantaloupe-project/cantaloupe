package edu.illinois.library.cantaloupe.image;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Normalized list of image transform operations along with an image identifier
 * and desired output format.
 */
public class OperationList implements Comparable<OperationList>, Iterable<Operation> {

    private Identifier identifier;
    private List<Operation> operations = new ArrayList<>();
    private Map<String,Object> options = new HashMap<>();
    private OutputFormat outputFormat;

    /**
     * @param op Operation to add. Null values will be discarded.
     */
    public void add(Operation op) {
        if (op != null) {
            operations.add(op);
        }
    }

    @Override
    public int compareTo(OperationList ops) {
        int last = this.toString().compareTo(ops.toString());
        return (last == 0) ? this.toString().compareTo(ops.toString()) : last;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * @return Map of auxiliary options separate from the basic
     * crop/scale/etc., such as URI query variables, etc.
     */
    public Map<String,Object> getOptions() {
        return options;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    /**
     * @return Whether the operations are effectively calling for the
     * unmodified source image.
     */
    public boolean isNoOp() {
        if (!this.getOutputFormat().isEqual(
                SourceFormat.getSourceFormat(this.getIdentifier()))) {
            return false;
        }
        for (Operation op : this) {
            if (!op.isNoOp()) {
                System.out.println(op);
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<Operation> iterator() {
        return operations.iterator();
    }

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     * represent the instance, but not guaranteed to be meaningful.
     */
    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        parts.add(getIdentifier().toString());
        for (Operation op : this) {
            parts.add(op.toString());
        }
        return StringUtils.join(parts, "_") + "." +
                getOutputFormat().getExtension();
    }

}
