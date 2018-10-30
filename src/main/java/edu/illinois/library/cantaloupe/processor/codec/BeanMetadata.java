package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;

import javax.imageio.metadata.IIOMetadataNode;
import java.nio.charset.StandardCharsets;

/**
 * Generic Bean-style implementation.
 */
public class BeanMetadata implements Metadata {

    private IIOMetadataNode tree;
    private Object exif, iptc;
    private String xmp;
    private Orientation orientation;

    @Override
    public IIOMetadataNode getAsTree() {
        return tree;
    }

    @Override
    public Object getEXIF() {
        return exif;
    }

    @Override
    public Object getIPTC() {
        return iptc;
    }

    @Override
    public Orientation getOrientation() {
        return orientation;
    }

    @Override
    public String getXMP() {
        return xmp;
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

    public void setTree(IIOMetadataNode tree) {
        this.tree = tree;
    }

    public void setXMP(byte[] xmp) {
        setXMP(new String(xmp, StandardCharsets.UTF_8));
    }

    public void setXMP(String xmp) {
        this.xmp = Util.trimXMP(xmp);
    }

}
