package edu.illinois.library.cantaloupe.image;

import org.apache.commons.lang3.StringUtils;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Normalized list of image transform operations along with an image identifier
 * and desired output format.
 */
public class OperationList implements Comparable<OperationList>,
        Iterable<Operation> {

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

    public void clear() {
        operations.clear();
    }

    @Override
    public int compareTo(OperationList ops) {
        int last = this.toString().compareTo(ops.toString());
        return (last == 0) ? this.toString().compareTo(ops.toString()) : last;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OperationList) {
            return obj.toString().equals(this.toString());
        }
        return super.equals(obj);
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
     * @param fullSize
     * @return Resulting dimensions when all operations are applied in sequence
     * to an image of the given full size.
     */
    public Dimension getResultingSize(Dimension fullSize) {
        Dimension size = new Dimension(fullSize.width, fullSize.height);
        for (Operation op : this) {
            size = op.getResultingSize(size);
        }
        return size;
    }

    /**
     * Determines whether the operations are effectively calling for the
     * unmodified source image, guessing the source format based on the
     * identifier. {@link #isNoOp(SourceFormat)} should be used instead, if
     * possible.
     *
     * @return Whether the operations are effectively calling for the
     * unmodified source image.
     */
    public boolean isNoOp() {
        return isNoOp(SourceFormat.getSourceFormat(this.getIdentifier()));
    }

    /**
     * Determines whether the operations are effectively calling for the
     * unmodified source image, based on the given source format.
     *
     * @param sourceFormat
     * @return Whether the operations are effectively calling for the
     * unmodified source image.
     */
    public boolean isNoOp(SourceFormat sourceFormat) {
        if (!this.getOutputFormat().isEqual(sourceFormat)) {
            return false;
        }
        for (Operation op : this) {
            if (!op.isNoOp()) {
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
     * represent the instance, but not guaranteed to have any particular
     * format.
     */
    @Override
    public String toString() {
        final List<String> parts = new ArrayList<>();
        parts.add(getIdentifier().toString());
        for (Operation op : this) {
            if (!op.isNoOp()) {
                parts.add(op.toString());
            }
        }
        return StringUtils.join(parts, "_") + "." +
                getOutputFormat().getExtension();
    }

}
