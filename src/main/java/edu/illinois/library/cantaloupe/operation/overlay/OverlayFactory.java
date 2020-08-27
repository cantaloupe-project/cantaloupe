package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Provides access to {@link Overlay} instances.
 */
public final class OverlayFactory {

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

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OverlayFactory.class);

    private Strategy strategy;

    public OverlayFactory() throws ConfigurationException {
        readStrategy();
    }

    private OverlayService newOverlayService(DelegateProxy delegateProxy)
            throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        OverlayService instance = null;
        switch (getStrategy()) {
            case BASIC:
                switch (config.getString(Key.OVERLAY_TYPE, "")) {
                    case "image":
                        instance = new BasicImageOverlayService();
                        break;
                    case "string":
                        instance = new BasicStringOverlayService();
                        break;
                }
                break;
            case DELEGATE_METHOD:
                instance = new DelegateOverlayService(delegateProxy);
                break;
        }
        if (instance != null) {
            LOGGER.trace("Using a {}", instance.getClass().getSimpleName());
        } else {
            LOGGER.trace("No {} available",
                    OverlayService.class.getSimpleName());
        }
        return instance;
    }

    /**
     * Factory method that returns a new {@link Overlay} based on either the
     * configuration, or the delegate method return value, depending on the
     * setting of {@link Key#OVERLAY_STRATEGY}.
     *
     * @param delegateProxy Required when {@link #getStrategy()} returns {@link
     *                      Strategy#DELEGATE_METHOD}. May be {@code null}
     *                      otherwise.
     * @return              Instance respecting the overlay strategy and given
     *                      arguments.
     */
    public Optional<Overlay> newOverlay(DelegateProxy delegateProxy)
            throws Exception {
        OverlayService service = newOverlayService(delegateProxy);
        if (service != null && service.isAvailable()) {
            return Optional.ofNullable(service.newOverlay());
        }
        return Optional.empty();
    }

    Strategy getStrategy() {
        return strategy;
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
