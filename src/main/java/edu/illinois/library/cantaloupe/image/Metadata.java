package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Normalized image metadata.
 */
public class Metadata {

    private Object exif, iptc;
    private String xmp;
    private Orientation orientation;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Metadata) {
            Metadata other = (Metadata) obj;
            return Objects.equals(other.exif, exif) &&
                    Objects.equals(other.iptc, iptc) &&
                    Objects.equals(other.xmp, xmp);
        }
        return super.equals(obj);
    }

    /**
     * @return EXIF data, or {@literal null} if none is present. The data may
     *         be a raw byte array or a {@link
     *         it.geosolutions.imageio.plugins.tiff.TIFFDirectory}.
     */
    public Object getEXIF() {
        return exif;
    }

    /**
     * @return IPTC IIM data, or {@literal null} if none is present. The data
     *         may be a raw byte array or a {@link
     *         it.geosolutions.imageio.plugins.tiff.TIFFDirectory}.
     */
    public Object getIPTC() {
        return iptc;
    }

    /**
     * @return Orientation of the image based on the EXIF {@literal
     *         Orientation} tag. If unknown or not specified, implementations
     *         should return {@link Orientation#ROTATE_0}.
     */
    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * Returns an RDF/XML string in UTF-8 encoding. The root element is
     * {@literal rdf:RDF}, and there is no packet wrapper.
     *
     * @return XMP data packet, or {@literal null} if no XMP data is available.
     */
    public String getXMP() {
        return xmp;
    }

    @Override
    public int hashCode() {
        // TODO: this is a bad implementation
        int code = 0;
        if (exif != null) {
            code += exif.hashCode();
        }
        if (iptc != null) {
            code += iptc.hashCode();
        }
        if (xmp != null) {
            code += xmp.hashCode();
        }
        return Integer.hashCode(code);
    }

    public void setEXIF(Object exif) {
        this.exif = exif;
    }

    public void setIPTC(Object iptc) {
        this.iptc = iptc;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public void setXMP(byte[] xmp) {
        setXMP(new String(xmp, StandardCharsets.UTF_8));
    }

    public void setXMP(String xmp) {
        this.xmp = StringUtils.trimXMP(xmp);
    }

}
