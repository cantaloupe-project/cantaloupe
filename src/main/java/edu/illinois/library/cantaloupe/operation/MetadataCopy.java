package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.util.Collections;
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
     * Does nothing.
     */
    @Override
    public void freeze() {
        // no-op
    }

    /**
     * @return True.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    /**
     * @param fullSize
     * @param opList
     * @return True.
     */
    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return hasEffect();
    }

    /**
     * @return Single-entry map with a {@literal class} key.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize,
                                     ScaleConstraint scaleConstraint) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return The string {@literal mdcopy}.
     */
    @Override
    public String toString() {
        return "mdcopy";
    }

}
