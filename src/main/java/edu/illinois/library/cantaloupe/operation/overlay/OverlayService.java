package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
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

    static final String ENABLED_CONFIG_KEY = "overlays.enabled";
    static final String STRATEGY_CONFIG_KEY = "overlays.strategy";

    private boolean isEnabled = false;
    private Strategy strategy;

    public OverlayService() throws ConfigurationException {
        readEnabled();
        readStrategy();
    }

    /**
     * Factory method that returns a new {@link Overlay} based on either the
     * configuration, or the delegate method return value, depending on the
     * setting of {@link #STRATEGY_CONFIG_KEY}.
     *
     * @param opList Required for ScriptStrategy.
     * @param fullSize Required for ScriptStrategy.
     * @param requestUrl Required for ScriptStrategy.
     * @param requestHeaders Required for ScriptStrategy.
     * @param clientIp Required for ScriptStrategy.
     * @param cookies Required for ScriptStrategy.
     * @return Overlay respecting the overlay strategy and given arguments,
     *         or null.
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     * @throws ConfigurationException
     */
    public Overlay newOverlay(OperationList opList,
                              Dimension fullSize,
                              URL requestUrl,
                              Map<String,String> requestHeaders,
                              String clientIp,
                              Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException, ConfigurationException {
        switch (getStrategy()) {
            case BASIC:
                switch (ConfigurationFactory.getInstance().
                        getString(BasicOverlayService.TYPE_CONFIG_KEY, "")) {
                    case "image":
                        return new BasicImageOverlayService().getOverlay();
                    case "string":
                        return new BasicStringOverlayService().getOverlay();
                }
                break;
            case DELEGATE_METHOD:
                return new DelegateOverlayService().getOverlay(
                        opList, fullSize, requestUrl, requestHeaders, clientIp,
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
        setEnabled(ConfigurationFactory.getInstance().
                getBoolean(ENABLED_CONFIG_KEY, false));
    }

    private void readStrategy() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String configValue = config.
                getString(STRATEGY_CONFIG_KEY, "BasicStrategy");
        switch (configValue) {
            case "ScriptStrategy":
                setStrategy(Strategy.DELEGATE_METHOD);
                break;
            case "BasicStrategy":
                setStrategy(Strategy.BASIC);
                break;
            default:
                throw new ConfigurationException("Unsupported value for " +
                        STRATEGY_CONFIG_KEY);
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
