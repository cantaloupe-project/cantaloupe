package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.OverlayService;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionService;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
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

import static edu.illinois.library.cantaloupe.processor.Processor.DOWNSCALE_FILTER_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.Processor.UPSCALE_FILTER_CONFIG_KEY;

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
public final class OperationList implements Comparable<OperationList>,
        Iterable<Operation> {

    private static final Logger logger = LoggerFactory.
            getLogger(OperationList.class);

    public static final short MAX_OUTPUT_QUALITY = 100;

    private boolean frozen = false;
    private Identifier identifier;
    private List<Operation> operations = new ArrayList<>();
    private Map<String,Object> options = new HashMap<>();
    private Compression outputCompression = Compression.UNDEFINED;
    private Format outputFormat;
    private boolean outputInterlacing = false;
    private int outputQuality = MAX_OUTPUT_QUALITY;

    /**
     * No-op constructor.
     */
    public OperationList() {}

    public OperationList(Identifier identifier) {
        this();
        setIdentifier(identifier);
    }

    public OperationList(Identifier identifier, Format outputFormat) {
        this(identifier);
        setOutputFormat(outputFormat);
    }

    /**
     * @param op Operation to add. Null values will be discarded.
     * @throws UnsupportedOperationException If the instance is frozen.
     */
    public void add(Operation op) {
        if (frozen) {
            throw new UnsupportedOperationException();
        }
        if (op != null) {
            operations.add(op);
        }
    }

    /**
     * <p>Most image-processing operations (crop, scale, etc.) are specified in
     * a client request to an endpoint. This method adds any operations or
     * options that endpoints have nothing to do with. (Normally these will come
     * either from the configuration, or a delegate method.)</p>
     *
     * <p>This method should be called after all endpoint operations have been
     * added, as it may modify them.</p>
     *
     * @param sourceImageSize Full size of the source image.
     * @param clientIp        Client IP address.
     * @param requestUrl      Request URL.
     * @param requestHeaders  Request headers.
     * @param cookies         Client cookies.
     */
    public void applyNonEndpointMutations(final Dimension sourceImageSize,
                                          final String clientIp,
                                          final URL requestUrl,
                                          final Map<String,String> requestHeaders,
                                          final Map<String,String> cookies) {
        final Configuration config = Configuration.getInstance();

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
                logger.debug("applyNonEndpointMutations(): redactions are " +
                        "disabled; skipping.");
            }
        } catch (DelegateScriptDisabledException e) {
            logger.debug("applyNonEndpointMutations(): delegate script is " +
                    "disabled; skipping redactions.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        // Scale filter
        final Scale scale = (Scale) getFirst(Scale.class);
        if (scale != null) {
            final Float scalePct = scale.getResultingScale(sourceImageSize);
            if (scalePct != null) {
                final String filterKey = (scalePct > 1) ?
                        UPSCALE_FILTER_CONFIG_KEY : DOWNSCALE_FILTER_CONFIG_KEY;
                try {
                    final String filterStr = config.getString(filterKey);
                    final Scale.Filter filter =
                            Scale.Filter.valueOf(filterStr.toUpperCase());
                    scale.setFilter(filter);
                } catch (Exception e) {
                    logger.warn("applyNonEndpointMutations(): invalid value for {}",
                            filterKey);
                }
            }
        }

        // Rotation background color
        if (!getOutputFormat().supportsTransparency()) {
            final String bgColor =
                    config.getString(Processor.BACKGROUND_COLOR_CONFIG_KEY, "black");
            final Rotate rotate = (Rotate) getFirst(Rotate.class);
            if (rotate != null) {
                rotate.setFillColor(Color.fromString(bgColor));
            }
        }

        // Sharpening
        float sharpen = config.getFloat(Processor.SHARPEN_CONFIG_KEY, 0f);
        if (sharpen > 0.001f) {
            add(new Sharpen(sharpen));
        }

        // Overlay
        try {
            final OverlayService service = new OverlayService();
            if (service.isEnabled()) {
                final Overlay overlay = service.newOverlay(
                        this, sourceImageSize, requestUrl, requestHeaders,
                        clientIp, cookies);
                add(overlay);
            } else {
                logger.debug("applyNonEndpointMutations(): overlays are " +
                        "disabled; skipping.");
            }
        } catch (DelegateScriptDisabledException e) {
            logger.debug("applyNonEndpointMutations(): delegate script is " +
                    "disabled; skipping overlay.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        // Metadata copies
        if (config.getBoolean(Processor.PRESERVE_METADATA_CONFIG_KEY, false)) {
            add(new MetadataCopy());
        }

        switch (getOutputFormat()) {
            case JPG:
                // Interlacing
                final boolean progressive =
                        config.getBoolean(Processor.JPG_PROGRESSIVE_CONFIG_KEY, false);
                setOutputInterlacing(progressive);
                // Quality
                final int quality =
                        config.getInt(Processor.JPG_QUALITY_CONFIG_KEY, 80);
                setOutputQuality(quality);
                break;
            case TIF:
                // Compression
                final String compressionStr =
                        config.getString(Processor.TIF_COMPRESSION_CONFIG_KEY, "LZW");
                final Compression compression =
                        Compression.valueOf(compressionStr.toUpperCase());
                setOutputCompression(compression);
                break;
        }
    }

    /**
     * @throws UnsupportedOperationException If the instance is frozen.
     */
    public void clear() {
        if (frozen) {
            throw new UnsupportedOperationException();
        }
        operations.clear();
    }

    @Override
    public int compareTo(OperationList ops) {
        return this.toString().compareTo(ops.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OperationList) {
            return obj.toString().equals(this.toString());
        }
        return super.equals(obj);
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
     * @return Output compression type. This only applies to certain output
     *         formats.
     */
    public Compression getOutputCompression() {
        return outputCompression;
    }

    public Format getOutputFormat() {
        return outputFormat;
    }

    /**
     * @return Output quality in the range of 1-{@link #MAX_OUTPUT_QUALITY}.
     *         This only applies to certain output formats, and perhaps also
     *         only with certain compressions.
     */
    public int getOutputQuality() {
        return outputQuality;
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
                // 1. Ignore overlays when the output format is PDF.
                // 2. Ignore MetadataCopies. If the instance would otherwise
                //    be a no-op, metadata will get passed through anyway, and
                //    if it isn't, then this method will return false anyway.
                if (!(op instanceof Overlay &&                   // (1)
                        getOutputFormat().equals(Format.PDF)) && // (1)
                        !(op instanceof MetadataCopy)) {         // (2)
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return Interlacing status. This only applies to output formats that
     *         support interlacing.
     */
    public boolean isOutputInterlacing() {
        return outputInterlacing;
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
     * @param identifier
     * @throws UnsupportedOperationException If the instance is frozen.
     */
    public void setIdentifier(Identifier identifier) {
        if (frozen) {
            throw new UnsupportedOperationException();
        }
        this.identifier = identifier;
    }

    public void setOutputCompression(Compression compression) {
        this.outputCompression = compression;
    }

    /**
     * @param outputFormat
     * @throws UnsupportedOperationException If the instance is frozen.
     */
    public void setOutputFormat(Format outputFormat) {
        if (frozen) {
            throw new UnsupportedOperationException();
        }
        this.outputFormat = outputFormat;
    }

    public void setOutputInterlacing(boolean interlacing) {
        this.outputInterlacing = interlacing;
    }

    /**
     * @param quality
     * @throws IllegalArgumentException If the given quality is outside the
     *         range of 1-{@link #MAX_OUTPUT_QUALITY}.
     */
    public void setOutputQuality(int quality) throws IllegalArgumentException {
        if (quality < 1 || quality > MAX_OUTPUT_QUALITY) {
            throw new IllegalArgumentException(
                    "Quality must be in the range of 1-" + MAX_OUTPUT_QUALITY + ".");
        }
        this.outputQuality = quality;
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
        // Add other instance properties
        if (isOutputInterlacing()) {
            opStrings.add("interlace");
        }
        if (getOutputQuality() < MAX_OUTPUT_QUALITY) {
            opStrings.add("quality:" + getOutputQuality());
        }
        if (getOutputCompression() != null &&
                !getOutputCompression().equals(Compression.UNCOMPRESSED) &&
                !getOutputCompression().equals(Compression.UNDEFINED)) {
            opStrings.add("compression:" + getOutputCompression());
        }

        String opsString = StringUtils.join(opStrings, "_");

        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(opsString.getBytes(Charset.forName("UTF8")));
            opsString = Hex.encodeHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.error("toFilename(): {}", e.getMessage());
        }

        return getIdentifier().toFilename() + "_" + opsString + "." +
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
     *         result of {@link Operation#toMap(Dimension)}
     *     ],
     *     "options": {
     *         "key": value
     *     },
     *     "output_format": result of {@link Format#toMap}
     *     "output_interlacing": boolean,
     *     "output_quality": short,
     *     "output_compression": string
     * }</pre>
     *
     * @param fullSize Full size of the source image on which the instance is
     *                 being applied.
     * @return Map representation of the instance.
     */
    public Map<String,Object> toMap(Dimension fullSize) {
        final Map<String,Object> map = new HashMap<>();
        map.put("identifier", getIdentifier().toString());
        map.put("operations", this.stream().
                filter(Operation::hasEffect).
                map(op -> op.toMap(fullSize)).
                collect(Collectors.toList()));
        map.put("options", getOptions());
        map.put("output_format", getOutputFormat().toMap());
        map.put("output_interlacing", isOutputInterlacing());
        map.put("output_quality", getOutputQuality());
        map.put("output_compression", getOutputCompression().toString());
        return map;
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
        for (String key : this.getOptions().keySet()) {
            parts.add(key + ":" + this.getOptions().get(key));
        }

        if (isOutputInterlacing()) {
            parts.add("interlace");
        }
        if (getOutputQuality() < MAX_OUTPUT_QUALITY) {
            parts.add("quality:" + getOutputQuality());
        }
        if (getOutputCompression() != null &&
                !getOutputCompression().equals(Compression.UNCOMPRESSED) &&
                !getOutputCompression().equals(Compression.UNDEFINED)) {
            parts.add("compression:" + getOutputCompression());
        }

        return StringUtils.join(parts, "_") + "." +
                getOutputFormat().getPreferredExtension();
    }

    /**
     * Validates the instance, throwing a {@link ValidationException} if
     * invalid.
     *
     * @param fullSize Full size of the source image on which the instance is
     *                 being applied.
     * @throws ValidationException
     */
    public void validate(Dimension fullSize) throws ValidationException {
        for (Operation op : this) {
            op.validate(fullSize);
        }
    }

}
