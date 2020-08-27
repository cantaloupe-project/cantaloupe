package edu.illinois.library.cantaloupe.operation.overlay;

/**
 * Provides access to {@link Overlay}s.
 */
interface OverlayService {

    /**
     * @return Whether the instance is capable of supplying any instances via
     *         {@link #newOverlay()}.
     */
    boolean isAvailable();

    Overlay newOverlay() throws Exception;

}
