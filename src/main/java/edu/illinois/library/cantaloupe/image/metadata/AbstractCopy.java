package edu.illinois.library.cantaloupe.image.metadata;

import java.awt.Dimension;

abstract class AbstractCopy {

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return fullSize
     */
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    /**
     * @return False.
     */
    public boolean isNoOp() {
        return false;
    }

}
