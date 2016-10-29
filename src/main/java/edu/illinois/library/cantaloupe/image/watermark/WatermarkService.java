package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Provides information about watermarking, including whether it is enabled,
 * and access to new {@link Watermark} instances, if so.
 */
public class WatermarkService {

    enum Strategy {
        /** Uses <code>watermark.BasicStrategy.*</code> configuration keys to
         * get global watermark properties. */
        BASIC,

        /** Uses the result of a delegate method to get watermark properties
         * per-request. */
        DELEGATE_METHOD
    }

    public static final String ENABLED_CONFIG_KEY = "watermark.enabled";
    public static final String STRATEGY_CONFIG_KEY = "watermark.strategy";

    private BasicWatermarkService basicService = new BasicWatermarkService();
    private DelegateWatermarkService delegateService =
            new DelegateWatermarkService();
    private boolean isEnabled = false;
    private Strategy strategy;

    public WatermarkService() {
        setEnabled(ConfigurationFactory.getInstance().
                getBoolean(ENABLED_CONFIG_KEY, false));

        final Configuration config = ConfigurationFactory.getInstance();
        final String configValue = config.
                getString(STRATEGY_CONFIG_KEY, "BasicStrategy");
        switch (configValue) {
            case "ScriptStrategy":
                strategy = Strategy.DELEGATE_METHOD;
            default:
                strategy = Strategy.BASIC;
        }
    }

    /**
     * Factory method that returns a new {@link Watermark} based on either
     * the configuration, or the delegate script method return value, depending
     * on the setting of {@link #STRATEGY_CONFIG_KEY}.
     *
     * @param opList Required for ScriptStrategy.
     * @param fullSize Required for ScriptStrategy.
     * @param requestUrl Required for ScriptStrategy.
     * @param requestHeaders Required for ScriptStrategy.
     * @param clientIp Required for ScriptStrategy.
     * @param cookies Required for ScriptStrategy.
     * @return Watermark respecting the watermark strategy and given arguments,
     *         or null.
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     * @throws ConfigurationException
     */
    public Watermark newWatermark(OperationList opList,
                                  Dimension fullSize,
                                  URL requestUrl,
                                  Map<String,String> requestHeaders,
                                  String clientIp,
                                  Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException, ConfigurationException {
        File image = null;
        Integer inset = null;
        Position position = null;
        switch (getStrategy()) {
            case DELEGATE_METHOD:
                final Map<String,Object> defs =
                        delegateService.getWatermarkProperties(
                                opList, fullSize, requestUrl, requestHeaders,
                                clientIp, cookies);
                if (defs != null) {
                    image = (File) defs.get("file");
                    inset = ((Long) defs.get("inset")).intValue();
                    position = (Position) defs.get("position");
                }
                break;
            default:
                final BasicWatermarkService basicService =
                        new BasicWatermarkService();
                image = basicService.getImage();
                inset = basicService.getInset();
                position = basicService.getPosition();
                break;
        }
        if (image != null && position != null) {
            return new Watermark(image, position, inset);
        }
        return null;
    }

    private Strategy getStrategy() {
        return strategy;
    }

    /**
     * @return Whether watermarking is enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * @param enabled Whether watermarking should be enabled.
     */
    void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    /**
     * @param strategy Watermark strategy to use.
     */
    void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * @param outputImageSize
     * @return Whether a watermark should be applied to an output image with
     *         the given dimensions.
     */
    public boolean shouldApplyToImage(Dimension outputImageSize) {
        switch (strategy) {
            case BASIC:
                return basicService.shouldApplyToImage(outputImageSize);
            default:
                return true;
        }
    }

}
