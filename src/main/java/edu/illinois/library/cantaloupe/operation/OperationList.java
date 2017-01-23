package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Normalized list of {@link Operation image transform operations}
 * corresponding to an image identified by its {@link Identifier}, along with
 * a {@link Format} in which the processed image is to be written.</p>
 *
 * <p>Endpoints translate request parameters into instances of this class, in
 * order to pass them off into {@link
 * edu.illinois.library.cantaloupe.processor.Processor processors} and
 * {@link edu.illinois.library.cantaloupe.cache.Cache caches}.</p>
 */
public class OperationList implements Comparable<OperationList>,
        Iterable<Operation> {

    private static final Logger logger = LoggerFactory.
            getLogger(OperationList.class);

    private Identifier identifier;
    private List<Operation> operations = new ArrayList<>();
    private Map<String,Object> options = new HashMap<>();
    private Format outputFormat;

    /**
     * No-op constructor.
     */
    public OperationList() {}

    public OperationList(Identifier identifier) {
        setIdentifier(identifier);
    }

    public OperationList(Identifier identifier, Format outputFormat) {
        setIdentifier(identifier);
        setOutputFormat(outputFormat);
    }

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
        return this.toString().compareTo(ops.toString());
    }

    /**
     * @param clazz
     * @return Whether the instance contains an operation of the given class.
     */
    public boolean contains(Class clazz) {
        for (Operation op : this) {
            if (op.getClass().equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OperationList) {
            return obj.toString().equals(this.toString());
        }
        return super.equals(obj);
    }

    /**
     * @param opClass Class to get the first instance of.
     * @return The first instance of <code>opClass</code> in the list, or
     *         <code>null</code> if there is no operation of that class in the
     *         list.
     */
    public Operation getFirst(Class<? extends Operation> opClass) {
        for (Operation op : operations) {
            if (op.getClass().equals(opClass)) {
                return op;
            }
        }
        return null;
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

    public Format getOutputFormat() {
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
     * identifier. {@link #isNoOp(Format)} should be used instead, if
     * possible.
     *
     * @return Whether the operations are effectively calling for the
     * unmodified source image.
     */
    public boolean isNoOp() {
        return isNoOp(Format.inferFormat(this.getIdentifier()));
    }

    /**
     * Determines whether the operations are effectively calling for the
     * unmodified source image, based on the given source format.
     *
     * @param format
     * @return Whether the operations are effectively calling for the
     * unmodified source image.
     */
    public boolean isNoOp(Format format) {
        if (!this.getOutputFormat().equals(format)) {
            return false;
        }
        for (Operation op : this) {
            if (op.hasEffect()) {
                // 1. Ignore overlays when the output formats is PDF.
                // 2. Ignore MetadataCopies. If the instance would otherwise
                //    be a no-op, metadata will get passed through anyway, and
                //    if it isn't, then this method will return false anyway.
                if (!(op instanceof Overlay &&                   // (1)
                        getOutputFormat().equals(Format.PDF)) && // (1)
                        !(op instanceof MetadataCopy)) {         // (2)
                    return false;
                }
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

    public void setOutputFormat(Format outputFormat) {
        this.outputFormat = outputFormat;
    }

    public Stream<Operation> stream() {
        return operations.stream();
    }

    /**
     * <p>Returns a filename-safe string guaranteed to uniquely represent the
     * instance. The filename is in the format:</p>
     *
     * <pre>{hashed identifier}_{hashed operation list}.{output format extension}</pre>
     *
     * @return Filename-safe string guaranteed to uniquely represent the
     *         instance.
     */
    public String toFilename() {
        final String identifierFilename = getIdentifier().toFilename();

        final List<String> opStrings = stream().
                filter(Operation::hasEffect).
                map(Operation::toString).
                collect(Collectors.toList());
        String opsString = StringUtils.join(opStrings, "_");

        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(opsString.getBytes(Charset.forName("UTF8")));
            opsString = Hex.encodeHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.error("toFilename(): {}", e.getMessage());
        }

        return identifierFilename + "_" + opsString + "." +
                getOutputFormat().getPreferredExtension();
    }

    /**
     * <p>Returns a map representing the instance with the following format
     * (expressed in JSON, with {@link Map}s expressed as objects and
     * {@link List}s expressed as arrays):</p>
     *
     * <pre>{
     *     "identifier": "result of {@link Identifier#toString()}",
     *     "operations": [
     *         "result of {@link Operation#toMap(Dimension)}"
     *     ],
     *     "options": {
     *         "key": "value"
     *     },
     *     "output_format": "result of {@link Format#toMap}"
     * }</pre>
     *
     * @param fullSize Full size of the source image on which the instance is
     *                 being applied.
     * @return Map representation of the instance.
     */
    public Map<String,Object> toMap(Dimension fullSize) {
        final Map<String,Object> map = new HashMap<>();
        // identifier
        map.put("identifier", getIdentifier().toString());
        // operations
        final List<Map<String,Object>> opsList = new ArrayList<>();
        for (Operation op : this) {
            if (op.hasEffect()) {
                opsList.add(op.toMap(fullSize));
            }
        }
        map.put("operations", opsList);
        // options
        map.put("options", getOptions());
        // output format
        map.put("output_format", getOutputFormat().toMap());

        return map;
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
            if (op.hasEffect()) {
                final String opName = op.getClass().getSimpleName().toLowerCase();
                parts.add(opName + ":" + op.toString());
            }
        }
        for (String key : this.getOptions().keySet()) {
            parts.add(key + ":" + this.getOptions().get(key));
        }
        return StringUtils.join(parts, "_") + "." +
                getOutputFormat().getPreferredExtension();
    }

}
