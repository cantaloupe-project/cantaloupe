package edu.illinois.library.cantaloupe.operation.overlay;

import javax.script.ScriptException;

import edu.illinois.library.cantaloupe.config.ConfigurationException;

/**
 * Provides access to {@link Overlay}s.
 */
interface OverlayService {

    /**
     * @return Whether the instance is capable of supplying any instances via
     *         {@link #newOverlay()}.
     */
    boolean isAvailable();

    Overlay newOverlay() throws ConfigurationException, ScriptException;

}
