package edu.illinois.library.cantaloupe.image;

/**
 * Interface to be implemented by all image-processing operations. Clients
 * (e.g. {@link edu.illinois.library.cantaloupe.processor.Processor}s) should
 * check instances' type and recast.
 */
public interface Operation {

    /**
     * @return Whether an application of the operation would result in an
     * identical source image.
     */
    boolean isNoOp();

}
