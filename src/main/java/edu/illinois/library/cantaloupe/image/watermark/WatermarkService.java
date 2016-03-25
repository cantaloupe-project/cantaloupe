package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides information about watermarking, including whether it is enabled,
 * and access to new {@link Watermark} instances, if so.
 */
public abstract class WatermarkService {

    private enum Strategy {
        /** Uses "watermark.BasicStrategy.*" configuration keys to get
         * global watermark properties. */
        BASIC,

        /** Uses the result of a delegate script method to get watermark
         * properties for a particular request. */
        DELEGATE_SCRIPT;
    }

    private static Logger logger = LoggerFactory.
            getLogger(WatermarkService.class);

    public static final String WATERMARK_ENABLED_CONFIG_KEY =
            "watermark.enabled";
    public static final String WATERMARK_STRATEGY_CONFIG_KEY =
            "watermark.strategy";
    public static final String WATERMARK_FILE_CONFIG_KEY =
            "watermark.BasicStrategy.image";
    public static final String WATERMARK_INSET_CONFIG_KEY =
            "watermark.BasicStrategy.inset";
    public static final String WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY =
            "watermark.BasicStrategy.output_height_threshold";
    public static final String WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY =
            "watermark.BasicStrategy.output_width_threshold";
    public static final String WATERMARK_POSITION_CONFIG_KEY =
            "watermark.BasicStrategy.position";

    /**
     * Factory method that returns a new {@link Watermark} based on either
     * the configuration, or the delegate script method return value, depending
     * on the setting of {@link #WATERMARK_STRATEGY_CONFIG_KEY}.
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
    public static Watermark newWatermark(OperationList opList,
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
            case DELEGATE_SCRIPT:
                final Map<String,Object> defs = getWatermarkDefsFromScript(
                        opList, fullSize, requestUrl, requestHeaders, clientIp,
                        cookies);
                if (defs != null) {
                    image = (File) defs.get("file");
                    inset = ((Long) defs.get("inset")).intValue();
                    position = (Position) defs.get("position");
                }
                break;
            default:
                image = getBasicImage();
                inset = getBasicInset();
                position = getBasicPosition();
                break;
        }
        if (image != null && inset != null && position != null) {
            return new Watermark(image, position, inset);
        }
        return null;
    }

    /**
     * @param opList
     * @param fullSize
     * @param requestUrl
     * @param requestHeaders
     * @param clientIp
     * @param cookies
     * @return Map with "file", "inset", and "position" keys; or null
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     */
    private static Map<String,Object> getWatermarkDefsFromScript(
            OperationList opList, Dimension fullSize, URL requestUrl,
            Map<String,String> requestHeaders, String clientIp,
            Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final Map<String,Integer> fullSizeArg = new HashMap<>();
        fullSizeArg.put("width", fullSize.width);
        fullSizeArg.put("height", fullSize.height);

        final Dimension resultingSize = opList.getResultingSize(fullSize);
        final Map<String,Integer> resultingSizeArg = new HashMap<>();
        resultingSizeArg.put("width", resultingSize.width);
        resultingSizeArg.put("height", resultingSize.height);

        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object[] args = new Object[8];
        args[0] = opList.getIdentifier().toString();           // identifier
        args[1] = opList.toMap(fullSize).get("operations");    // operations
        args[2] = resultingSizeArg;                            // resulting_size
        args[3] = opList.toMap(fullSize).get("output_format"); // output_format
        args[4] = requestUrl.toString();                       // request_uri
        args[5] = requestHeaders;                              // request_headers
        args[6] = clientIp;                                    // client_ip
        args[7] = cookies;                                     // cookies
        final String method = "watermark";
        final Object result = engine.invoke(method, args);
        if (result == null || (result instanceof Boolean && !((Boolean) result))) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) result;
        map.put("file", new File((String) map.get("pathname")));
        map.remove("pathname");
        map.put("position", Position.fromString((String) map.get("position")));
        return map;
    }

    /**
     * Returns the value of {@link #WATERMARK_FILE_CONFIG_KEY} when
     * {@link #WATERMARK_STRATEGY_CONFIG_KEY} is set to "BasicStrategy."
     *
     * @return File
     * @throws ConfigurationException
     */
    private static File getBasicImage() throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        final String path = config.getString(WATERMARK_FILE_CONFIG_KEY, "");
        if (path.length() > 0) {
            return new File(path);
        }
        throw new ConfigurationException(
                WATERMARK_FILE_CONFIG_KEY + " is not set.");
    }

    /**
     * Returns the value of {@link #WATERMARK_INSET_CONFIG_KEY} when
     * {@link #WATERMARK_STRATEGY_CONFIG_KEY} is set to "BasicStrategy."
     *
     * @return Watermark inset, defaulting to 0 if
     *         {@link WatermarkService#WATERMARK_INSET_CONFIG_KEY} is not set.
     */
    private static int getBasicInset() {
        final Configuration config = Configuration.getInstance();
        return config.getInt(WATERMARK_INSET_CONFIG_KEY, 0);
    }

    /**
     * Returns the value of {@link #WATERMARK_POSITION_CONFIG_KEY} when
     * {@link #WATERMARK_STRATEGY_CONFIG_KEY} is set to "BasicStrategy."
     *
     * @return Watermark position, or null if
     *         {@link WatermarkService#WATERMARK_POSITION_CONFIG_KEY} is not
     *         set.
     * @throws ConfigurationException
     */
    private static Position getBasicPosition()
            throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        final String configValue = config.
                getString(WATERMARK_POSITION_CONFIG_KEY, "");
        if (configValue.length() > 0) {
            try {
                return Position.fromString(configValue);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(
                        "Invalid " + WATERMARK_POSITION_CONFIG_KEY +
                                " value: " + configValue);
            }
        }
        throw new ConfigurationException(
                WATERMARK_POSITION_CONFIG_KEY + " is not set.");
    }

    private static Strategy getStrategy() {
        final Configuration config = Configuration.getInstance();
        final String configValue = config.
                getString(WATERMARK_STRATEGY_CONFIG_KEY, "BasicStrategy");
        switch (configValue) {
            case "ScriptStrategy":
                return Strategy.DELEGATE_SCRIPT;
            default:
                return Strategy.BASIC;
        }
    }

    /**
     * @return Whether {@link #WATERMARK_ENABLED_CONFIG_KEY} is true.
     */
    public static boolean isEnabled() {
        return Configuration.getInstance().
                getBoolean(WATERMARK_ENABLED_CONFIG_KEY, false);
    }

    /**
     * @param outputImageSize
     * @return Whether a watermark should be applied to an output image with
     * the given dimensions.
     */
    public static boolean shouldApplyToImage(Dimension outputImageSize) {
        final Configuration config = Configuration.getInstance();
        final int minOutputWidth =
                config.getInt(WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 0);
        final int minOutputHeight =
                config.getInt(WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 0);
        return (outputImageSize.width >= minOutputWidth &&
                outputImageSize.height >= minOutputHeight);
    }

}
