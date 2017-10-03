package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Provides information about overlays, including whether they are enabled,
 * and access to new {@link Overlay} instances, if so.
 */
public class OverlayService {

    enum Strategy {
        /** Uses <code>overlays.BasicStrategy.*</code> configuration keys to
         * get global overlay properties. */
        BASIC,

        /** Uses the result of a delegate method to get overlay properties
         * per-request. */
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
     * @param opList Required for ScriptStrategy.
     * @param fullSize Required for ScriptStrategy.
     * @param requestURI Required for ScriptStrategy.
     * @param requestHeaders Required for ScriptStrategy.
     * @param clientIP Required for ScriptStrategy.
     * @param cookies Required for ScriptStrategy.
     * @return Overlay respecting the overlay strategy and given arguments,
     *         or <code>null</code>.
     */
    public Overlay newOverlay(OperationList opList,
                              Dimension fullSize,
                              URI requestURI,
                              Map<String,String> requestHeaders,
                              String clientIP,
                              Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException, ConfigurationException {
        switch (getStrategy()) {
            case BASIC:
                switch (Configuration.getInstance().
                        getString(Key.OVERLAY_TYPE, "")) {
                    case "image":
                        return new BasicImageOverlayService().getOverlay();
                    case "string":
                        return new BasicStringOverlayService().getOverlay();
                }
                break;
            case DELEGATE_METHOD:
                return new DelegateOverlayService().getOverlay(
                        opList, fullSize, requestURI, requestHeaders, clientIP,
                        cookies);
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
