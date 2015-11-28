package edu.illinois.library.cantaloupe.image;

/**
 * Interface to be implemented by all image-processing operations. Clients
 * (e.g. {@link #Processor}s) should check instances' type and recast.
 */
public interface Operation {

    /**
     * @return Whether the operation "does anything," or whether it is a no-op.
     */
    boolean isNoOp();

}
