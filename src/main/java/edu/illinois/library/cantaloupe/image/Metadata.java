package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.illinois.library.cantaloupe.image.exif.Directory;
import edu.illinois.library.cantaloupe.image.exif.Field;
import edu.illinois.library.cantaloupe.image.exif.Tag;
import edu.illinois.library.cantaloupe.image.iptc.DataSet;
import edu.illinois.library.cantaloupe.image.xmp.MapReader;
import edu.illinois.library.cantaloupe.image.xmp.Utils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Physical embedded image metadata.
 *
 * @see <a href="https://wwwimages2.adobe.com/content/dam/acom/en/devnet/xmp/pdfs/XMP%20SDK%20Release%20cc-2016-08/XMPSpecificationPart1.pdf">
 *     XMP Specification Part 1: Data Model, Serialization, and Core
 *     Properties</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Metadata.class);

    private static final String XMP_ORIENTATION_PREDICATE =
            "http://ns.adobe.com/tiff/1.0/Orientation";

    protected Directory exif;
    protected List<DataSet> iptcDataSets;
    protected String xmp;
    protected Object nativeMetadata;

    /**
     * Cached by {@link #loadXMP()}.
     */
    private transient Model xmpModel;

    /**
     * Cached by {@link #getOrientation()}.
     */
    private transient Orientation orientation;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Metadata) {
            Metadata other = (Metadata) obj;
            return Objects.equals(other.getEXIF(), getEXIF()) &&
                    Objects.equals(other.getIPTC(), getIPTC()) &&
                    Objects.equals(other.getXMP(), getXMP()) &&
                    Objects.equals(other.getNativeMetadata(), getNativeMetadata());
        }
        return super.equals(obj);
    }

    /**
     * @return EXIF data.
     */
    @JsonProperty
    public Optional<Directory> getEXIF() {
        return Optional.ofNullable(exif);
    }

    /**
     * @return List of IPTC IIM data sets.
     */
    @JsonProperty
    public Optional<List<DataSet>> getIPTC() {
        return Optional.ofNullable(iptcDataSets);
    }

    /**
     * @return Format-native metadata, or {@literal null} if none is present.
     */
    @JsonProperty("native")
    public Optional<?> getNativeMetadata() {
        return Optional.ofNullable(nativeMetadata);
    }

    /**
     * <p>Reads the orientation from the {@literal Orientation} tag in {@link
     * #getEXIF() EXIF data}, falling back to the {@link
     * #XMP_ORIENTATION_PREDICATE XMP orientation triple} in {@link #getXMP()
     * XMP data}. The result is cached.</p>
     *
     * @return Image orientation. Will be {@link Orientation#ROTATE_0} if
     *         orientation is not contained in EXIF or XMP data.
     */
    @JsonIgnore
    public Orientation getOrientation() {
        if (orientation == null) {
            try {
                getEXIF().ifPresent(exif -> readOrientationFromEXIF());

                if (orientation == null) {
                    getXMP().ifPresent(xmp -> readOrientationFromXMP());
                }
                if (orientation == null) {
                    orientation = Orientation.ROTATE_0;
                }
            } catch (IllegalArgumentException | RiotException e) {
                LOGGER.info("readOrientation(): {}", e.getMessage());
                orientation = Orientation.ROTATE_0;
            }
        }
        return orientation;
    }

    private void readOrientationFromEXIF() {
        Field field = exif.getField(Tag.ORIENTATION);
        Object value = exif.getValue(Tag.ORIENTATION);
        if (field != null && value != null) {
            switch (field.getDataType()) {
              case LONG:
              case SLONG:
                orientation = Orientation.forEXIFOrientation(Math.toIntExact((long) value));
                break;
              case SHORT:
              case SSHORT:
                orientation = Orientation.forEXIFOrientation((int) value);
                break;
              default:
                LOGGER.warn("readOrientationFromEXIF(): Unsupported Orientation data type: {}",
                        field.getDataType());
            }
        }
    }

    private void readOrientationFromXMP() {
        getXMPModel().ifPresent(model -> {
            final NodeIterator it = model.listObjectsOfProperty(
                    model.createProperty(XMP_ORIENTATION_PREDICATE));
            if (it.hasNext()) {
                final int value = it.next().asLiteral().getInt();
                orientation = Orientation.forEXIFOrientation(value);
            }
        });
    }

    /**
     * @return RDF/XML string in UTF-8 encoding. The root element is {@literal
     *         rdf:RDF}, and there is no packet wrapper.
     */
    @JsonProperty
    public Optional<String> getXMP() {
        return Optional.ofNullable(xmp);
    }

    /**
     * @return Map of elements found in the XMP data. If none are found, the
     *         map is empty.
     */
    @JsonIgnore
    public Map<String,Object> getXMPElements() {
        loadXMP();
        if (xmpModel != null) {
            try {
                MapReader reader = new MapReader(xmpModel);
                return reader.readElements();
            } catch (IOException e) {
                LOGGER.warn("getXMPElements(): {}", e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    /**
     * @return XMP model backed by the contents of {@link #getXMP()}.
     */
    @JsonIgnore
    public Optional<Model> getXMPModel() {
        loadXMP();
        return Optional.ofNullable(xmpModel);
    }

    @Override
    public int hashCode() {
        final int[] codes = new int[4];
        if (exif != null) {
            codes[0] = exif.hashCode();
        }
        if (iptcDataSets != null) {
            codes[1] = iptcDataSets.hashCode();
        }
        if (xmp != null) {
            codes[2] = xmp.hashCode();
        }
        if (nativeMetadata != null) {
            codes[3] = nativeMetadata.hashCode();
        }
        return Arrays.hashCode(codes);
    }

    /**
     * Reads {@link #xmp} into {@link #xmpModel}.
     */
    private void loadXMP() {
        final Optional<String> xmp = getXMP();
        if (xmpModel == null && xmp.isPresent()) {
            RIOT.init();

            xmpModel = ModelFactory.createDefaultModel();
            String base = null;
            if (xmp.get().indexOf("rdf:about=''") != -1 || xmp.get().indexOf("rdf:about=\"\"") != -1) {
                // Version 4.8+ of jena requires a rdf:about link to not be empty
                base = "http://example.com";
            }

            try (StringReader reader = new StringReader(xmp.get())) {
                xmpModel.read(reader, base, "RDF/XML");
            } catch (RiotException e) {
                if (e.getMessage().indexOf("Base URI is null, but there are relative URIs to resolve") != -1) {
                    // Version 4.8+ of jena requires a rdf:about link to not be empty
                    try (StringReader reader = new StringReader(xmp.get())) {
                        xmpModel.read(reader, "http://example.com", "RDF/XML");
                    } catch (RiotException exception) {
                        LOGGER.info("loadXMP(): {}", exception.getMessage());
                    }    
                } else {
                    LOGGER.info("loadXMP(): {}", e.getMessage());
                    throw e;
                }
            } catch (NullPointerException e) {
                // The XMP string may be invalid RDF/XML, or there may be a bug
                // in Jena (that would be the NPE). Not much we can do.
                LOGGER.info("loadXMP(): {}", e.getMessage());
            }
        }
    }

    /**
     * @param exif EXIF directory (IFD0). May be {@literal null}.
     */
    public void setEXIF(Directory exif) {
        if (exif != null) {
            this.exif = exif;
        } else {
            this.exif        = null;
            this.orientation = null;
        }
    }

    /**
     * @param dataSets IPTC IIM data sets. May be {@literal null}.
     */
    public void setIPTC(List<DataSet> dataSets) {
        this.iptcDataSets = dataSets;
    }

    /**
     * N.B.: Using native metadata requires overriding {@link #toMap()}.
     *
     * @param nativeMetadata Format-native metadata. <strong>Must be
     *                       Jackson-serializable</strong>. May be {@literal
     *                       null}.
     */
    public void setNativeMetadata(Object nativeMetadata) {
        this.nativeMetadata = nativeMetadata;
    }

    /**
     * @param xmp UTF-8 bytes. May be {@literal null}.
     */
    public void setXMP(byte[] xmp) {
        if (xmp != null) {
            setXMP(new String(xmp, StandardCharsets.UTF_8));
        } else {
            this.xmp         = null;
            this.xmpModel = null;
            this.orientation = null;
        }
    }

    /**
     * @param xmp UTF-8 string. May be {@literal null}.
     */
    public void setXMP(String xmp) {
        if (xmp != null) {
            this.xmp = Utils.trimXMP(xmp);
        } else {
            this.xmp         = null;
            this.xmpModel = null;
            this.orientation = null;
        }
    }

    /**
     * <p>Returns a map with the following structure:</p>
     *
     * {@code
     * {
     *     "exif": See {@link Directory#toMap()},
     *     "iptc": See {@link DataSet#toMap()},
     *     "xmp_string": "<rdf:RDF>...</rdf:RDF>",
     *     "xmp_model": [Jena model],
     *     "xmp_elements": {@link Map}
     *     "native": String
     * }}
     *
     * <p>N.B.: Subclasses that use the {@literal native} key should override
     * and set it to a map rather than a string.</p>
     *
     * @return Map representation of the instance.
     */
    public Map<String,Object> toMap() {
        final Map<String,Object> map = new HashMap<>(5);
        // EXIF
        getEXIF().ifPresent(exif -> map.put("exif", exif.toMap()));
        // IPTC
        getIPTC().ifPresent(iptc -> {
            if (!iptc.isEmpty()) {
                map.put("iptc", iptc.stream()
                        .map(DataSet::toMap)
                        .collect(Collectors.toList()));
            }
        });
        // XMP
        getXMP().ifPresent(xmp -> map.put("xmp_string", xmp));
        getXMPModel().ifPresent(model -> map.put("xmp_model", model));
        map.put("xmp_elements", getXMPElements());
        // Native metadata
        getNativeMetadata().ifPresent(nm -> map.put("native", nm));
        return Collections.unmodifiableMap(map);
    }

}
