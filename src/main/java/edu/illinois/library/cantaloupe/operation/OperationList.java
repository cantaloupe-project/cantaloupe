package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.OverlayService;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionService;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Normalized list of {@link Operation image transform operations}
 * corresponding to an image identified by its {@link Identifier}.</p>
 *
 * <p>This class has dual purposes:</p>
 *
 * <ol>
 *     <li>To assemble and store a list of image transform operations;</li>
 *     <li>To uniquely identify a post-processed ("derivative") image that has
 *     undergone processing using the instance. For example, the return values
 *     of {@link #toString()} and {@link #toFilename()} may be used in cache
 *     keys.</li>
 * </ol>
 *
 * <p>Endpoints translate request parameters into instances of this class, in
 * order to pass them off into {@link
 * edu.illinois.library.cantaloupe.processor.Processor processors} and
 * {@link edu.illinois.library.cantaloupe.cache.Cache caches}.</p>
 *
 * <p>Processors should iterate the operations in the list and apply them
 * (generally in order) as best they can.</p>
 */
public final class OperationList implements Comparable<OperationList>,
        Iterable<Operation> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OperationList.class);

    private boolean isFrozen;
    private Identifier identifier;
    private final List<Operation> operations = new ArrayList<>();
    private final Map<String,Object> options = new HashMap<>();

    /**
     * Constructs a minimal valid instance.
     */
    public OperationList() {}

    public OperationList(Identifier identifier) {
        this();

        setIdentifier(identifier);
    }

    public OperationList(Operation... operations) {
        this();

        for (Operation op : operations) {
            add(op);
        }
    }

    public OperationList(Identifier identifier, Operation... operations) {
        this(operations);

        setIdentifier(identifier);
    }

    /**
     * Adds an operation to the end of the list.
     *
     * @param op Operation to add. Null values are silently discarded.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void add(Operation op) {
        checkFrozen();
        if (op != null) {
            operations.add(op);
        }
    }

    /**
     * Adds an operation immediately after the last instance of the given
     * class in the list. If there are no such instances in the list, the
     * operation will be added to the end of the list.
     *
     * @param op         Operation to add.
     * @param afterClass The operation will be added after the last
     *                   instance of this class in the list.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void addAfter(Operation op,
                         Class<? extends Operation> afterClass) {
        checkFrozen();
        final int index = lastIndexOf(afterClass);
        if (index >= 0) {
            operations.add(index + 1, op);
        } else {
            add(op);
        }
    }

    /**
     * Adds an operation immediately before the first instance of the given
     * class in the list. If there are no such instances in the list, the
     * operation will be added to the end of the list.
     *
     * @param op          Operation to add.
     * @param beforeClass The operation will be added before the first
     *                    instance of this class in the list.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void addBefore(Operation op,
                          Class<? extends Operation> beforeClass) {
        checkFrozen();
        int index = firstIndexOf(beforeClass);
        if (index >= 0) {
            operations.add(index, op);
        } else {
            add(op);
        }
    }

    /**
     * <p>Most image-processing operations (crop, scale, etc.) are specified in
     * a client request to an endpoint. This method adds any other operations or
     * options that endpoints have nothing to do with, and also tweaks existing
     * operations according to either/both the application configuration and
     * delegate method return values.</p>
     *
     * <p>This method must be called <strong>after</strong> all endpoint
     * operations have been added, as it may modify them. It will have the
     * side-effect of {@link #freeze() freezing the instance}.</p>
     *
     * <p>The instance's identifier must be {@link #setIdentifier(Identifier)
     * set}.</p>
     *
     * @param info          Source image info.
     * @param delegateProxy Delegate proxy for the current request.
     * @throws IllegalArgumentException if the instance's output format has not
     *                                  been set.
     */
    public void applyNonEndpointMutations(final Info info,
                                          final DelegateProxy delegateProxy) {
        checkFrozen();

        final Identifier identifier = getIdentifier();
        if (identifier == null) {
            throw new IllegalStateException("Identifier not set.");
        }

        final Encode encode = (Encode) getFirst(Encode.class);

        if (encode == null) {
            throw new IllegalStateException("No " +
                    Encode.class.getSimpleName() + " operation present.");
        }

        final Configuration config = Configuration.getInstance();
        final Dimension sourceImageSize = info.getSize();

        // If the source image has a different orientation, adjust any Crop
        // and Rotate operations accordingly.
        final Orientation sourceImageOrientation = info.getOrientation();
        if (sourceImageOrientation != null) {
            Crop crop = (Crop) getFirst(Crop.class);
            if (crop != null) {
                crop.applyOrientation(sourceImageOrientation, sourceImageSize);
            }

            Rotate rotate = (Rotate) getFirst(Rotate.class);
            if (rotate != null) {
                rotate.addDegrees(sourceImageOrientation.getDegrees());
            }
        }

        // Normalization
        final boolean normalize = config.getBoolean(Key.PROCESSOR_NORMALIZE, false);
        if (normalize) {
            // 1. If a Crop is present, normalization has to happen before it,
            //    in order to sample the entire image.
            // 2. Otherwise, if a Scale is present, normalization should happen
            //    before an upscale, or after a downscale.
            // 3. Otherwise, it should be added before a Rotate, as this could
            //    introduce edge color that would throw it off.
            Normalize normalizeOp = new Normalize();
            if (getFirst(Crop.class) != null) {
                addBefore(normalizeOp, Crop.class);
            } else {
                Scale scale = (Scale) getFirst(Scale.class);
                if (scale != null) {
                    if (scale.isUp(sourceImageSize)) {
                        addBefore(normalizeOp, Scale.class);
                    } else {
                        addAfter(normalizeOp, Scale.class);
                    }
                } else {
                    addBefore(normalizeOp, Rotate.class);
                }
            }
        }

        // Redactions
        try {
            final RedactionService service = new RedactionService();
            if (service.isEnabled()) {
                List<Redaction> redactions = service.redactionsFor(delegateProxy);
                for (Redaction redaction : redactions) {
                    addBefore(redaction, Encode.class);
                }
            } else {
                LOGGER.debug("applyNonEndpointMutations(): redactions are " +
                        "disabled; skipping.");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        // Scale filter
        final Scale scale = (Scale) getFirst(Scale.class);
        if (scale != null) {
            final Float scalePct = scale.getResultingScale(sourceImageSize);
            if (scalePct != null) {
                final Key filterKey = (scalePct > 1) ?
                        Key.PROCESSOR_UPSCALE_FILTER :
                        Key.PROCESSOR_DOWNSCALE_FILTER;
                try {
                    final String filterStr = config.getString(filterKey);
                    final Scale.Filter filter =
                            Scale.Filter.valueOf(filterStr.toUpperCase());
                    scale.setFilter(filter);
                } catch (Exception e) {
                    LOGGER.warn("applyNonEndpointMutations(): invalid value for {}",
                            filterKey);
                }
            }
        }

        // Sharpening
        float sharpen = config.getFloat(Key.PROCESSOR_SHARPEN, 0f);
        if (sharpen > 0.001f) {
            addBefore(new Sharpen(sharpen), Encode.class);
        }

        // Overlay
        try {
            final OverlayService service = new OverlayService();
            if (service.isEnabled() &&
                    service.shouldApplyToImage(getResultingSize(sourceImageSize))) {
                final Overlay overlay = service.newOverlay(delegateProxy);
                addBefore(overlay, Encode.class);
            } else {
                LOGGER.debug("applyNonEndpointMutations(): overlays are " +
                        "disabled; skipping.");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        // Metadata copies
        // TODO: consider making these a property of Encode
        if (config.getBoolean(Key.PROCESSOR_PRESERVE_METADATA, false)) {
            addBefore(new MetadataCopy(), Encode.class);
        }

        // Encode customization
        switch (encode.getFormat()) {
            case JPG:
                // Compression
                encode.setCompression(Compression.JPEG);
                // Interlacing
                final boolean progressive =
                        config.getBoolean(Key.PROCESSOR_JPG_PROGRESSIVE, false);
                encode.setInterlacing(progressive);
                // Quality
                final int quality =
                        config.getInt(Key.PROCESSOR_JPG_QUALITY, 80);
                encode.setQuality(quality);
                break;
            case TIF:
                // Compression
                final String compressionStr =
                        config.getString(Key.PROCESSOR_TIF_COMPRESSION, "LZW");
                final Compression compression =
                        Compression.valueOf(compressionStr.toUpperCase());
                encode.setCompression(compression);
                break;
        }

        // Set the Encode operation's background color.
        if (!encode.getFormat().supportsTransparency()) {
            final String bgColor = config.getString(Key.PROCESSOR_BACKGROUND_COLOR);
            if (bgColor != null) {
                encode.setBackgroundColor(Color.fromString(bgColor));
            }
        }

        // Set the Encode operation's max sample size.
        boolean limit = config.getBoolean(Key.PROCESSOR_LIMIT_TO_8_BITS, true);
        encode.setMaxSampleSize(limit ? 8 : null);

        // Now that mutations have been applied, we don't want any more
        // changes to the instance down the pike.
        freeze();
    }

    private void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    /**
     * @throws IllegalStateException If the instance is frozen.
     */
    public void clear() {
        checkFrozen();
        operations.clear();
    }

    @Override
    public int compareTo(OperationList ops) {
        return this.toString().compareTo(ops.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof OperationList) {
            return obj.toString().equals(this.toString());
        }
        return super.equals(obj);
    }

    /**
     * @param clazz Operation class.
     * @return Index of the first instance of the given class in the list, or
     *         {@literal -1} if no instance of the given class is present in
     *         the list.
     */
    private int firstIndexOf(Class<? extends Operation> clazz) {
        int index = 0;
        boolean found = false;
        for (Operation operation : operations) {
            if (clazz.isAssignableFrom(operation.getClass())) {
                found = true;
                break;
            }
            index++;
        }
        return (found) ? index : -1;
    }

    /**
     * "Freezes" the instance and all of its operations.
     */
    public void freeze() {
        isFrozen = true;
        for (Operation op : this) {
            op.freeze();
        }
    }

    /**
     * @param opClass Class to get the first instance of.
     * @return The first instance of {@literal opClass} in the list, or
     *         {@literal null} if there is no operation of that class in the
     *         list.
     */
    public Operation getFirst(Class<? extends Operation> opClass) {
        for (Operation op : operations) {
            if (opClass.isAssignableFrom(op.getClass())) {
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
     *         crop/scale/etc., such as URI query variables, etc. If the
     *         instance is frozen, the map will be unmodifiable.
     */
    public Map<String,Object> getOptions() {
        if (isFrozen) {
            return Collections.unmodifiableMap(options);
        }
        return options;
    }

    /**
     * <p>Used for quickly checking the {@link Encode} operation's format.</p>
     *
     * <p>N.B.: {@link #applyNonEndpointMutations} may mutate the {@link
     * Encode} operation.</p>
     *
     * @return The output format.
     */
    public Format getOutputFormat() {
        Encode encode = (Encode) getFirst(Encode.class);
        return (encode != null) ? encode.getFormat() : null;
    }

    /**
     * @param fullSize Full size of the source image to which the instance is
     *                 being applied.
     * @return         Resulting dimensions when all operations are applied in
     *                 sequence to an image of the given full size.
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
     * unmodified source image, based on the given source format.
     *
     * @param format
     * @return Whether the operations are effectively calling for the
     *         unmodified source image.
     */
    public boolean hasEffect(Format format) {
        for (Operation op : this) {
            if (op.hasEffect()) {
                // 1. Ignore MetadataCopies. If the instance would otherwise be
                //    a no-op, metadata will get passed through anyway, and if
                //    it isn't, then this method will return false anyway.
                // 2. Ignore overlays when the output format is PDF.
                // 3. Ignore Encodes when the given output format is the same
                //    as the instance's output format. (This helps enable
                //    streaming source images without re-encoding them.)
                if (op instanceof MetadataCopy) { // (1)
                    continue;
                } else if (op instanceof Overlay &&
                        getOutputFormat().equals(Format.PDF)) { // (2)
                    continue;
                } else if (op instanceof Encode &&
                        format.equals(((Encode) op).getFormat())) { // (3)
                    continue;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @return Iterator over the instance's operations. If the instance is
     *         frozen, {@link Iterator#remove()} will throw an
     *         {@link UnsupportedOperationException}.
     */
    @Override
    public Iterator<Operation> iterator() {
        if (isFrozen) {
            return Collections.unmodifiableList(operations).iterator();
        }
        return operations.iterator();
    }

    /**
     * @param clazz Operation class.
     * @return Index of the last instance of the given class in the list, or -1
     *         if no instance of the given class is present in the list.
     */
    private int lastIndexOf(Class<? extends Operation> clazz) {
        for (int i = operations.size() - 1; i >= 0; i--) {
            if (clazz.isAssignableFrom(operations.get(i).getClass())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param identifier
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setIdentifier(Identifier identifier) {
        checkFrozen();
        this.identifier = identifier;
    }

    public Stream<Operation> stream() {
        return operations.stream();
    }

    /**
     * <p>Returns a filename-safe string guaranteed to uniquely represent the
     * instance. The filename is in the format:</p>
     *
     * <pre>{hashed identifier}_{hashed operation list + options list}.{output format extension}</pre>
     *
     * @return Filename-safe string guaranteed to uniquely represent the
     *         instance.
     */
    public String toFilename() {
        // Compile operations
        final List<String> opStrings = stream().
                filter(Operation::hasEffect).
                map(Operation::toString).
                collect(Collectors.toList());
        // Add options
        for (String key : this.getOptions().keySet()) {
            opStrings.add(key + ":" + this.getOptions().get(key));
        }

        String opsString = String.join("_", opStrings);

        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(opsString.getBytes(Charset.forName("UTF8")));
            opsString = Hex.encodeHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("toFilename(): {}", e.getMessage());
        }

        String idStr = "";
        Identifier identifier = getIdentifier();
        if (identifier != null) {
            idStr = identifier.toString();
        }

        String extension = "";
        Encode encode = (Encode) getFirst(Encode.class);
        if (encode != null) {
            extension = "." + encode.getFormat().getPreferredExtension();
        }

        return StringUtil.filesystemSafe(idStr) + "_" + opsString + extension;
    }

    /**
     * <p>Returns a map representing the instance with the following format
     * (expressed in JSON, with {@link Map}s expressed as objects and
     * {@link List}s expressed as arrays):</p>
     *
     * <pre>{
     *     "identifier": "result of {@link Identifier#toString()}",
     *     "operations": [
     *         result of {@link Operation#toMap(Dimension)}
     *     ],
     *     "options": {
     *         "key": value
     *     }
     * }</pre>
     *
     * @param fullSize Full size of the source image on which the instance is
     *                 being applied.
     * @return         Unmodifiable Map representation of the instance.
     */
    public Map<String,Object> toMap(Dimension fullSize) {
        final Map<String,Object> map = new HashMap<>();
        if (getIdentifier() != null) {
            map.put("identifier", getIdentifier().toString());
        }
        map.put("operations", this.stream().
                filter(Operation::hasEffect).
                map(op -> op.toMap(fullSize)).
                collect(Collectors.toList()));
        map.put("options", getOptions());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     *         represent the instance, but not guaranteed to have any particular
     *         format.
     */
    @Override
    public String toString() {
        final List<String> parts = new ArrayList<>();
        if (getIdentifier() != null) {
            parts.add(getIdentifier().toString());
        }
        for (Operation op : this) {
            if (op.hasEffect()) {
                final String opName = op.getClass().getSimpleName().toLowerCase();
                parts.add(opName + ":" + op.toString());
            }
        }
        for (String key : getOptions().keySet()) {
            parts.add(key + ":" + getOptions().get(key));
        }
        return String.join("_", parts);
    }

    /**
     * <ol>
     *     <li>Checks that an {@link #setIdentifier(Identifier) identifier is
     *     set};</li>
     *     <li>Checks that an {@link Encode} is present;</li>
     *     <li>Calls {@link Operation#validate(Dimension)} on each {@link
     *     Operation};</li>
     *     <li>Validates the {@literal page} {@link #getOptions() option}, if
     *     present.</li>
     * </ol>
     *
     * @param fullSize Full size of the source image on which the instance is
     *                 being applied.
     * @throws IllegalArgumentException if the instance is invalid.
     */
    public void validate(Dimension fullSize) {
        // Ensure that an identifier is set.
        if (getIdentifier() == null) {
            throw new IllegalArgumentException("Identifier not set.");
        }
        // Ensure that an Encode operation is present.
        if (getFirst(Encode.class) == null) {
            throw new IllegalArgumentException(
                    "Missing " + Encode.class.getSimpleName() + " operation");
        }
        // Validate each operation.
        for (Operation op : this) {
            op.validate(fullSize);
        }

        // "page" is a special query argument used by some processors, namely
        // ones that read PDFs, that tells them what page to read. We might
        // as well validate it here to save them the trouble.
        final String pageStr = (String) getOptions().get("page");
        if (pageStr != null) {
            try {
                final int page = Integer.parseInt(pageStr);
                if (page < 1) {
                    throw new IllegalArgumentException(
                            "Page number is out-of-bounds.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid page number.");
            }
        }
    }

}
