package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.OverlayFactory;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionService;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Normalized list of {@link Operation image transform operations}
 * associated with a source image that is identified by either a {@link
 * MetaIdentifier} or {@link Identifier}.</p>
 *
 * <p>This class has dual purposes:</p>
 *
 * <ol>
 *     <li>To describe a list of image transform operations;</li>
 *     <li>To uniquely identify a post-processed (&quot;derivative&quot;) image
 *     created using the instance. For example, the return values of {@link
 *     #toString()} or {@link #toFilename()} may be used in cache keys.</li>
 * </ol>
 *
 * <p>Endpoints translate request arguments into instances of this class, in
 * order to pass them off into {@link
 * edu.illinois.library.cantaloupe.processor.Processor processors} and {@link
 * edu.illinois.library.cantaloupe.cache.Cache caches}.</p>
 *
 * <p>Processors should iterate the operations in the list and apply them
 * (generally in order) as best they can. They must take the {@link
 * #getScaleConstraint() scale constraint} into account when cropping and
 * scaling.</p>
 */
public final class OperationList implements Iterable<Operation> {

    public static final class Builder {

        private Identifier identifier;
        private MetaIdentifier metaIdentifier;
        private Operation[] operations;
        private Map<String,String> options;
        private int pageIndex;

        public Builder withIdentifier(Identifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withMetaIdentifier(MetaIdentifier metaIdentifier) {
            this.metaIdentifier = metaIdentifier;
            return this;
        }

        public Builder withOperations(Operation... operations) {
            this.operations = operations;
            return this;
        }

        public Builder withOptions(Map<String,String> options) {
            this.options = options;
            return this;
        }

        public Builder withPageIndex(int pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        public OperationList build() {
            OperationList opList = new OperationList();
            opList.setIdentifier(identifier);
            opList.setMetaIdentifier(metaIdentifier);
            opList.setPageIndex(pageIndex);
            if (operations != null) {
                opList.operations.addAll(List.of(operations));
            }
            if (options != null) {
                opList.options.putAll(options);
            }
            return opList;
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OperationList.class);

    private boolean isFrozen;
    private Identifier identifier;
    private MetaIdentifier metaIdentifier;
    private final List<Operation> operations = new ArrayList<>();
    private final Map<String,Object> options = new HashMap<>();
    private int pageIndex;

    public static OperationList.Builder builder() {
        return new Builder();
    }

    /**
     * No-op constructor.
     */
    public OperationList() {}

    public OperationList(Identifier identifier) {
        this();
        setIdentifier(identifier);
    }

    public OperationList(MetaIdentifier identifier) {
        this();
        setMetaIdentifier(identifier);
    }

    /**
     * Adds an operation to the end of the list.
     *
     * @param op Operation to add. {@code null} values are silently discarded.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void add(Operation op) {
        if (op != null) {
            checkFrozen();
            operations.add(op);
        }
    }

    /**
     * Adds an operation immediately after the last instance of the given
     * class in the list. If there are no such instances in the list, the
     * operation will be added to the end of the list.
     *
     * @param op         Operation to add. {@code null} values are silently
     *                   discarded.
     * @param afterClass The operation will be added after the last
     *                   instance of this class in the list.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void addAfter(Operation op,
                         Class<? extends Operation> afterClass) {
        if (op != null) {
            checkFrozen();
            final int index = lastIndexOf(afterClass);
            if (index >= 0) {
                operations.add(index + 1, op);
            } else {
                add(op);
            }
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
        if (op != null) {
            checkFrozen();
            int index = firstIndexOf(beforeClass);
            if (index >= 0) {
                operations.add(index, op);
            } else {
                add(op);
            }
        }
    }

    /**
     * <p>Most image-processing operations (crop, scale, etc.) are supplied by
     * a client in a request to an endpoint. This method adds any other
     * operations or options that endpoints have nothing to do with, and also
     * tweaks existing operations according to either/both the application
     * configuration and delegate method return values.</p>
     *
     * <p>This method must be called <strong>after</strong> all endpoint
     * operations have been added, as it may modify them.</p>
     *
     * <p>The instance's identifier must be {@link #setIdentifier(Identifier)
     * set}.</p>
     *
     * @param info          Source image info.
     * @param delegateProxy Delegate proxy for the current request.
     */
    public void applyNonEndpointMutations(final Info info,
                                          final DelegateProxy delegateProxy) {
        checkFrozen();

        // If there is a scale constraint set, but no Scale operation, add one.
        if (getScaleConstraint().hasEffect()) {
            Scale scale = (Scale) getFirst(Scale.class);
            if (scale == null) {
                scale = new ScaleByPercent();
                int index = firstIndexOf(Crop.class);
                if (index == -1) {
                    operations.add(0, scale);
                } else {
                    addAfter(scale, Crop.class);
                }
            }
        }

        final Configuration config = Configuration.getInstance();
        final Dimension sourceImageSize = info.getSize();

        // If the source image has a different orientation, adjust any Crop
        // and Rotate operations accordingly.
        final Metadata srcMetadata = info.getMetadata();
        if (srcMetadata != null) {
            final Orientation orientation = srcMetadata.getOrientation();
            final Crop crop = (Crop) getFirst(Crop.class);
            if (crop != null) {
                crop.setOrientation(orientation);
            }
            final Rotate rotate = (Rotate) getFirst(Rotate.class);
            if (rotate != null) {
                rotate.addDegrees(orientation.getDegrees());
            }
        }

        // Redactions
        try {
            final RedactionService service = new RedactionService();
            List<Redaction> redactions = service.redactionsFor(delegateProxy);
            for (Redaction redaction : redactions) {
                addBefore(redaction, Encode.class);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        // Scale customization
        final Scale scale = (Scale) getFirst(Scale.class);
        if (scale != null) {
            // Filter
            double[] scales = scale.getResultingScales(
                    sourceImageSize, getScaleConstraint());
            double smallestScale = Arrays.stream(scales).min().orElse(1);

            final Key filterKey = (smallestScale > 1) ?
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

        // Sharpening
        double sharpen = config.getDouble(Key.PROCESSOR_SHARPEN, 0);
        if (sharpen > 0.001) {
            addBefore(new Sharpen(sharpen), Encode.class);
        }

        // Overlay
        try {
            final OverlayFactory overlayFactory = new OverlayFactory();
            if (overlayFactory.shouldApplyToImage(getResultingSize(sourceImageSize))) {
                final Optional<Overlay> overlay =
                        overlayFactory.newOverlay(delegateProxy);
                overlay.ifPresent(ov -> addBefore(ov, Encode.class));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        // Encode customization
        final Encode encode = (Encode) getFirst(Encode.class);
        if (encode != null && Format.get("jpg").equals(encode.getFormat())) {
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
        } else if (encode != null && Format.get("tif").equals(encode.getFormat())) {
            // Compression
            final String compressionStr =
                    config.getString(Key.PROCESSOR_TIF_COMPRESSION, "LZW");
            final Compression compression =
                    Compression.valueOf(compressionStr.toUpperCase());
            encode.setCompression(compression);
        }

        // Set the Encode operation's background color.
        if (encode != null && !encode.getFormat().supportsTransparency()) {
            final String bgColor = config.getString(Key.PROCESSOR_BACKGROUND_COLOR);
            if (bgColor != null) {
                encode.setBackgroundColor(Color.fromString(bgColor));
            }
        }

        // Add metadata.
        Metadata metadata = null;
        // Add XMP metadata returned from a delegate method, if any.
        if (delegateProxy != null) {
            try {
                final String xmp = delegateProxy.getMetadata();
                if (xmp != null) {
                    metadata = new Metadata();
                    metadata.setXMP(xmp);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        // Source metadata may contain important native information like
        // e.g. delay time in the case of animated GIF, so that too must be
        // copied over.
        if (srcMetadata != null) {
            if (metadata == null) {
                metadata = new Metadata();
            }
            metadata.setNativeMetadata(srcMetadata.getNativeMetadata());
        }
        if (encode != null && metadata != null) {
            encode.setMetadata(metadata);
        }
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

    /**
     * @return The identifier ascribed to the instance, if set; otherwise the
     *         {@link #getMetaIdentifier() meta-identifier}'s identifier, if
     *         set; otherwise {@code null}.
     */
    public Identifier getIdentifier() {
        if (identifier != null) {
            return identifier;
        } else if (metaIdentifier != null) {
            return metaIdentifier.getIdentifier();
        }
        return null;
    }

    public MetaIdentifier getMetaIdentifier() {
        return metaIdentifier;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    /**
     * @return Map of auxiliary options separate from the basic
     *         crop/scale/etc., such as URI query arguments, etc. If the
     *         instance is frozen, the map is unmodifiable.
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
     * @return Page index.
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * @param fullSize Full size of the source image to which the instance is
     *                 being applied.
     * @return         Resulting dimensions when all operations are applied in
     *                 sequence to an image of the given full size.
     */
    public Dimension getResultingSize(Dimension fullSize) {
        for (Operation op : this) {
            fullSize = op.getResultingSize(fullSize, getScaleConstraint());
        }
        return fullSize;
    }

    /**
     * Convenience method that returns the instance ascribed to the {@link
     * #getMetaIdentifier() meta-identifier}, if set, or a neutral instance
     * otherwise.
     */
    public ScaleConstraint getScaleConstraint() {
        ScaleConstraint scaleConstraint = null;
        if (getMetaIdentifier() != null) {
            scaleConstraint = getMetaIdentifier().getScaleConstraint();
        }
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint(1, 1);
        }
        return scaleConstraint;
    }

    /**
     * Determines whether the operations are effectively calling for the
     * unmodified source image, based on the given source format.
     *
     * @param fullSize Full size of the source image.
     * @param format   Source image format.
     * @return         Whether the operations are effectively calling for the
     *                 unmodified source image.
     */
    public boolean hasEffect(Dimension fullSize, Format format) {
        if (getMetaIdentifier() != null &&
                getMetaIdentifier().getScaleConstraint() != null &&
                getMetaIdentifier().getScaleConstraint().hasEffect()) {
            return true;
        }
        if (!format.equals(getOutputFormat())) {
            return true;
        }
        for (Operation op : this) {
            if (op.hasEffect(fullSize, this)) {
                // 1. Ignore overlays when the output format is PDF.
                // 2. Ignore Encodes when the given output format is the same
                //    as the instance's output format. (This helps enable
                //    streaming source images without re-encoding them.)
                if (op instanceof Overlay &&
                        getOutputFormat().equals(Format.get("pdf"))) { // (1)
                    continue;
                } else if (op instanceof Encode &&
                        format.equals(((Encode) op).getFormat())) { // (2)
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
     * @throws IllegalStateException if the instance is frozen.
     * @see #setMetaIdentifier(MetaIdentifier)
     */
    public void setIdentifier(Identifier identifier) {
        checkFrozen();
        this.identifier = identifier;
    }

    /**
     * <p>Sets the effective base scale of the source image upon which the
     * instance is to be applied.</p>
     *
     * @param metaIdentifier
     * @throws IllegalStateException if the instance is frozen.
     * @see #setIdentifier(Identifier)
     */
    public void setMetaIdentifier(MetaIdentifier metaIdentifier) {
        checkFrozen();
        this.metaIdentifier = metaIdentifier;
    }

    /**
     * @param pageIndex Zero-based page index.
     * @throws IllegalStateException if the instance is frozen.
     * @deprecated Once the {@code page} query argument is no longer supported,
     *             the page index should be retrieved from the {@link
     *             #getMetaIdentifier() meta-identifier}.
     */
    @Deprecated
    public void setPageIndex(int pageIndex) {
        checkFrozen();
        if (pageIndex < 0) {
            throw new IllegalArgumentException("Page index must be >= 0");
        }
        this.pageIndex = pageIndex;
    }

    public Stream<Operation> stream() {
        return operations.stream();
    }

    /**
     * <p>Returns a filename-safe string guaranteed to uniquely represent the
     * instance. The filename is in the format:</p>
     *
     * <p>{@literal [hashed identifier]_[page number + hashed scale constraint + operation list + options list].[output format extension]}</p>
     *
     * @return Filename string.
     */
    public String toFilename() {
        final List<String> parts = new ArrayList<>();
        // Add page index if != 0
        if (getPageIndex() != 0) {
            parts.add("" + getPageIndex());
        }
        // Add scale constraint
        if (getScaleConstraint().hasEffect()) {
            parts.add(0, getScaleConstraint().toString());
        }
        // Add operations
        parts.addAll(stream().
                filter(Operation::hasEffect).
                map(Operation::toString).
                collect(Collectors.toList()));
        // Add options
        for (String key : getOptions().keySet()) {
            parts.add(key + ":" + this.getOptions().get(key));
        }

        String opsString = StringUtils.md5(String.join("_", parts));

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
        return StringUtils.md5(idStr) + "_" + opsString + extension;
    }

    /**
     * <p>Returns a map representing the instance with the following format
     * (expressed in JSON, with {@link Map}s expressed as objects and
     * {@link List}s expressed as arrays):</p>
     *
     * <pre>{
     *     "identifier": "result of {@link Identifier#toString()}",
     *     "scale_constraint": result of {@link ScaleConstraint#toMap()}
     *     "operations": [
     *         result of {@link Operation#toMap}
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
        map.put("page_index", getPageIndex());
        map.put("scale_constraint", getScaleConstraint().toMap());
        map.put("operations", this.stream()
                .filter(op -> op.hasEffect(fullSize, this))
                .map(op -> op.toMap(fullSize, getScaleConstraint()))
                .collect(Collectors.toUnmodifiableList()));
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
        if (getPageIndex() != 0) {
            parts.add(getPageIndex() + "");
        }
        parts.add(getScaleConstraint().toString());
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
     *     set}</li>
     *     <li>Checks that an {@link Encode} is present</li>
     *     <li>Calls {@link Operation#validate} on each {@link Operation}</li>
     *     <li>Checks that the resulting scale is not larger than allowed by
     *     the {@link #getScaleConstraint() scale constraint}</li>
     *     <li>Checks that the resulting pixel area is greater than zero and
     *     less than or equal to {@link Key#MAX_PIXELS} (if set)</li>
     * </ol>
     *
     * @param fullSize     Full size of the source image on which the instance
     *                     is being applied.
     * @param sourceFormat Source image format.
     * @throws IllegalSizeException  if the resulting size exceeds {@link
     *         Key#MAX_PIXELS}.
     * @throws IllegalScaleException if the resulting scale exceeds that
     *         allowed by the {@link #getScaleConstraint() scale constraint},
     *         if set.
     * @throws ValidationException if the instance is invalid in some other way.
     */
    public void validate(Dimension fullSize,
                         Format sourceFormat) throws ValidationException {
        // Ensure that an identifier is set.
        if (getIdentifier() == null) {
            throw new ValidationException("Identifier is not set.");
        }
        // Ensure that an Encode operation is present.
        if (getFirst(Encode.class) == null) {
            throw new ValidationException(
                    "Missing " + Encode.class.getSimpleName() + " operation");
        }
        // Validate each operation.
        for (Operation op : this) {
            op.validate(fullSize, getScaleConstraint());
        }

        Dimension resultingSize = getResultingSize(fullSize);

        // If there is a scale constraint set, ensure that the resulting scale
        // will not be greater than 100%.
        final ScaleConstraint scaleConstraint = getScaleConstraint();
        if (scaleConstraint.hasEffect()) {
            Scale scale = (Scale) getFirst(Scale.class);
            if (scale == null) {
                scale = new ScaleByPercent();
            }
            final double delta = Math.max(1 / fullSize.width(), 1 / fullSize.height());
            final double[] scales = scale.getResultingScales(fullSize, scaleConstraint);

            if (Arrays.stream(scales)
                    .filter(s -> s - delta > scaleConstraint.getRational().doubleValue())
                    .findAny()
                    .isPresent()) {
                throw new IllegalScaleException();
            }
        }

        System.out.println("----");
        // Ensure that the resulting pixel area is positive.
        if (resultingSize.isEmpty()) {
            throw new ValidationException("Resulting pixel area is empty.");
        }

        // Ensure that the resulting pixel area is less than or equal to the
        // max allowed area, unless the processing is a no-op.
        final long maxAllowedSize =
                Configuration.getInstance().getLong(Key.MAX_PIXELS, 0);
        if (maxAllowedSize > 0 && hasEffect(fullSize, sourceFormat) &&
                resultingSize.width() * resultingSize.height() > maxAllowedSize) {
            throw new IllegalSizeException();
        }
    }

}
