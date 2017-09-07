package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.OverlayService;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionService;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.net.URL;
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
 * <p>Endpoints translate request parameters into instances of this class, in
 * order to pass them off into {@link
 * edu.illinois.library.cantaloupe.processor.Processor processors} and
 * {@link edu.illinois.library.cantaloupe.cache.Cache caches}.</p>
 *
 * <p>Processors should iterate the operations in the list and apply them
 * (generally in order) as best they can. Instances will have an associated
 * {@link #getOutputFormat() output format} but
 * {@link #applyNonEndpointMutations} will also add a more comprehensive
 * {@link Encode} operation, based on it, to the list, which processors should
 * look at instead.</p>
 */
public final class OperationList implements Comparable<OperationList>,
        Iterable<Operation> {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(OperationList.class);

    private boolean frozen = false;
    private Identifier identifier;
    private List<Operation> operations = new ArrayList<>();
    private Map<String,Object> options = new HashMap<>();
    private Format outputFormat;

    /**
     * Constructs a minimal valid instance.
     */
    public OperationList(Identifier identifier, Format outputFormat) {
        setIdentifier(identifier);
        setOutputFormat(outputFormat);
    }

    public OperationList(Identifier identifier, Format outputFormat,
                         Operation... operations) {
        this(identifier, outputFormat);
        for (Operation op : operations) {
            add(op);
        }
    }

    /**
     * Adds an operation to the end of the list.
     *
     * @param op Operation to add. Null values are silently discarded.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void add(Operation op) {
        if (frozen) {
            throw new IllegalStateException();
        }
        if (op != null) {
            operations.add(op);
        }
    }

    /**
     * Adds an operation immediately after the last instance of the given
     * class in the list. If there are no such instances in the list, the
     * operation will be added to the end of the list.
     *
     * @param op Operation to add.
     * @param afterClass The operation will be added after the last
     *                   instance of this class in the list.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void addAfter(Operation op,
                         Class<? extends Operation> afterClass) {
        if (frozen) {
            throw new IllegalStateException();
        }
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
     * @param op Operation to add.
     * @param beforeClass The operation will be added before the first
     *                    instance of this class in the list.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void addBefore(Operation op,
                          Class<? extends Operation> beforeClass) {
        if (frozen) {
            throw new IllegalStateException();
        }
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
     * <p>This method should be called <strong>after</strong> all endpoint
     * operations have been added, as it may modify them. It will have the
     * side-effect of freezing the instance.</p>
     *
     * @param sourceImageSize        Full size of the source image.
     * @param sourceImageOrientation Orientation of the source image.
     * @param clientIp               Client IP address.
     * @param requestUrl             Request URL.
     * @param requestHeaders         Request headers.
     * @param cookies                Client cookies.
     * @throws IllegalArgumentException If the instance's output format has not
     *                                  been set.
     */
    public void applyNonEndpointMutations(final Dimension sourceImageSize,
                                          final Orientation sourceImageOrientation,
                                          final String clientIp,
                                          final URL requestUrl,
                                          final Map<String,String> requestHeaders,
                                          final Map<String,String> cookies) {
        if (getOutputFormat() == null
                || Format.UNKNOWN.equals(getOutputFormat())) {
            throw new IllegalArgumentException(
                    "Output format is null or unknown. This is probably a bug.");
        }

        final Configuration config = Configuration.getInstance();

        // Apply the orientation to the Crop operation, if both are present.
        Crop crop = (Crop) getFirst(Crop.class);
        if (crop != null && sourceImageOrientation != null) {
            crop.applyOrientation(sourceImageOrientation, sourceImageSize);
        }

        // Normalization
        final boolean normalize =
                config.getBoolean(Key.PROCESSOR_NORMALIZE, false);
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
                List<Redaction> redactions = service.redactionsFor(
                        getIdentifier(), requestHeaders, clientIp, cookies);
                for (Redaction redaction : redactions) {
                    add(redaction);
                }
            } else {
                LOGGER.debug("applyNonEndpointMutations(): redactions are " +
                        "disabled; skipping.");
            }
        } catch (DelegateScriptDisabledException e) {
            LOGGER.debug("applyNonEndpointMutations(): delegate script is " +
                    "disabled; skipping redactions.");
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
            add(new Sharpen(sharpen));
        }

        // Overlay
        try {
            final OverlayService service = new OverlayService();
            if (service.isEnabled() && service.shouldApplyToImage(getResultingSize(sourceImageSize))) {
                final Overlay overlay = service.newOverlay(
                        this, sourceImageSize, requestUrl, requestHeaders,
                        clientIp, cookies);
                add(overlay);
            } else {
                LOGGER.debug("applyNonEndpointMutations(): overlays are " +
                        "disabled; skipping.");
            }
        } catch (DelegateScriptDisabledException e) {
            LOGGER.debug("applyNonEndpointMutations(): delegate script is " +
                    "disabled; skipping overlay.");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        // Metadata copies
        // TODO: consider making these a property of Encode
        if (config.getBoolean(Key.PROCESSOR_PRESERVE_METADATA, false)) {
            add(new MetadataCopy());
        }

        // Create an Encode operation corresponding to the output format.
        Encode encode = new Encode(getOutputFormat());
        add(encode);
        switch (encode.getFormat()) {
            case JPG:
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
            final String bgColor =
                    config.getString(Key.PROCESSOR_BACKGROUND_COLOR);
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

    /**
     * @throws IllegalStateException If the instance is frozen.
     */
    public void clear() {
        if (frozen) {
            throw new IllegalStateException();
        }
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
     *         -1 if no instance of the given class is present in the list.
     */
    private int firstIndexOf(Class<? extends Operation> clazz) {
        int index = 0;
        boolean found = false;
        for (int i = 0, count = operations.size(); i < count; i++) {
            if (clazz.isAssignableFrom(operations.get(i).getClass())) {
                found = true;
                break;
            }
            index++;
        }
        return (found) ? index : -1;
    }

    /**
     * "Freezes" the instance so that operations cannot be added or removed.
     */
    public void freeze() {
        this.frozen = true;
    }

    /**
     * @param opClass Class to get the first instance of.
     * @return The first instance of <code>opClass</code> in the list, or
     *         <code>null</code> if there is no operation of that class in the
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
        if (frozen) {
            return Collections.unmodifiableMap(options);
        }
        return options;
    }

    /**
     * <p>Used for quickly checking the output format.</p>
     *
     * <p>N.B. After {@link #applyNonEndpointMutations} has been called, there
     * is more comprehensive encoding information available by passing an
     * {@link Encode} class to {@link #getFirst(Class)}.</p>
     *
     * @return The output format.
     */
    public Format getOutputFormat() {
        return outputFormat;
    }

    /**
     * @param fullSize Full size of the source image to which the instance is
     *                 being applied.
     * @return Resulting dimensions when all operations are applied in sequence
     *         to an image of the given full size.
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
        if (!this.getOutputFormat().equals(format)) {
            return true;
        }
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
                        getOutputFormat().equals(format)) { // (3)
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
        if (frozen) {
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
        if (frozen) {
            throw new IllegalStateException();
        }
        this.identifier = identifier;
    }

    /**
     * @param outputFormat Format to set.
     * @throws IllegalStateException If the instance is frozen.
     * @throws IllegalArgumentException If the given format is not supported.
     */
    public void setOutputFormat(Format outputFormat) {
        if (frozen) {
            throw new IllegalStateException();
        } else if (Format.UNKNOWN.equals(outputFormat)) {
            throw new IllegalArgumentException("Illegal output format: " +
                    outputFormat);
        }
        this.outputFormat = outputFormat;
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

        return StringUtil.filesystemSafe(getIdentifier().toString()) + "_" +
                opsString + "." + getOutputFormat().getPreferredExtension();
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
        map.put("identifier", getIdentifier().toString());
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
        parts.add(getIdentifier().toString());
        for (Operation op : this) {
            if (op.hasEffect()) {
                final String opName = op.getClass().getSimpleName().toLowerCase();
                parts.add(opName + ":" + op.toString());
            }
        }
        for (String key : getOptions().keySet()) {
            parts.add(key + ":" + getOptions().get(key));
        }
        return String.join("_", parts) + "." +
                getOutputFormat().getPreferredExtension();
    }

    /**
     * Validates the instance, throwing a {@link ValidationException} if
     * invalid.
     *
     * @param fullSize Full size of the source image on which the instance is
     *                 being applied.
     */
    public void validate(Dimension fullSize) throws ValidationException {
        for (Operation op : this) {
            op.validate(fullSize);
        }
    }

}
