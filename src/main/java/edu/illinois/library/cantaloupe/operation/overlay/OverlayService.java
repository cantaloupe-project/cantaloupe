package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;

import javax.script.ScriptException;

/**
 * Provides information about overlays, including whether they are enabled,
 * and access to new {@link Overlay} instances, if so.
 */
public final class OverlayService {

    enum Strategy {

        /**
         * Gets global overlay properties from configuration keys.
         */
        BASIC,

        /**
         * Uses the result of a delegate method to get overlay properties
         * per-request.
         */
        DELEGATE_METHOD

    }

    private boolean isEnabled = false;
    private Strategy strategy;

    public OverlayService() throws ConfigurationException {
        readEnabled();
        readStrategy();
    }

    /**
     * Factory method that returns a new {@link Overlay} based on either the
     * configuration, or the delegate method return value, depending on the
     * setting of {@link Key#OVERLAY_STRATEGY}.
     *
     * @param delegateProxy Required for {@link Strategy#DELEGATE_METHOD}.
     *                      May be {@literal null}.
     * @return              Overlay respecting the overlay strategy and given
     *                      arguments, or {@literal null}.
     */
    public Overlay newOverlay(DelegateProxy delegateProxy)
            throws ScriptException, ConfigurationException {
        final Configuration config = Configuration.getInstance();
        switch (getStrategy()) {
            case BASIC:
                switch (config.getString(Key.OVERLAY_TYPE, "")) {
                    case "image":
                        return new BasicImageOverlayService().getOverlay();
                    case "string":
                        return new BasicStringOverlayService().getOverlay();
                }
                break;
            case DELEGATE_METHOD:
                return new DelegateOverlayService().getOverlay(delegateProxy);
        }
        return null;
    }

    Strategy getStrategy() {
        return strategy;
    }

    /**
     * @return Whether overlays are enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    private void readEnabled() {
        setEnabled(Configuration.getInstance().
                getBoolean(Key.OVERLAY_ENABLED, false));
    }

    private void readStrategy() throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        final String configValue = config.getString(
                Key.OVERLAY_STRATEGY, "BasicStrategy");
        switch (configValue) {
            case "ScriptStrategy":
                setStrategy(Strategy.DELEGATE_METHOD);
                break;
            case "BasicStrategy":
                setStrategy(Strategy.BASIC);
                break;
            default:
                throw new ConfigurationException("Unsupported value for " +
                        Key.OVERLAY_STRATEGY);
        }
    }

    /**
     * @param enabled Whether overlays should be enabled.
     */
    void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    /**
     * @param strategy Overlay strategy to use.
     */
    void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * @param outputImageSize
     * @return Whether an overlay should be applied to an output image with
     *         the given dimensions.
     */
    public boolean shouldApplyToImage(Dimension outputImageSize) {
        switch (strategy) {
            case BASIC:
                return BasicOverlayService.shouldApplyToImage(outputImageSize);
            default:
                // The delegate method will decide.
                return true;
        }
    }

}
