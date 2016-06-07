package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates an operation that copies the metadata of a source image
 * into a derivative image.</p>
 *
 * <p>What constitutes "metadata" is left to the discretion of the image
 * reader and writer. In the future this class may be enhanced to distinguish
 * between EXIF, IPTC, XMP, etc.</p>
 */
public class MetadataCopy implements Operation {

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return fullSize
     */
    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    /**
     * @return False.
     */
    @Override
    public boolean isNoOp() {
        return false;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Single-entry map with key of <var>operation</var> pointing to
     *         <code>metadata_copy</code>.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("operation", "metadata_copy");
        return map;
    }

    /**
     * @return The string <code>mdcopy</code>.
     */
    @Override
    public String toString() {
        return "mdcopy";
    }

}
