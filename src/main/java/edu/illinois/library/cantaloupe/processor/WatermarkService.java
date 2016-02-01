package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Provides information about watermarking configuration. Actual
 * watermark-related image processing is handled in more codec-specific
 * classes.
 */
public abstract class WatermarkService {

    private static Logger logger = LoggerFactory.
            getLogger(WatermarkService.class);

    public static final String WATERMARK_ENABLED_CONFIG_KEY = "watermark.enabled";
    public static final String WATERMARK_FILE_CONFIG_KEY = "watermark.image";
    public static final String WATERMARK_INSET_CONFIG_KEY = "watermark.inset";
    public static final String WATERMARK_POSITION_CONFIG_KEY = "watermark.position";

    /**
     * @return File corresponding to
     *         {@link WatermarkService#WATERMARK_FILE_CONFIG_KEY}, or null if it is
     *         not set.
     */
    public static File getWatermarkImage() {
        final Configuration config = Application.getConfiguration();
        final String path = config.
                getString(WATERMARK_FILE_CONFIG_KEY, "");
        if (path.length() > 0) {
            return new File(path);
        }
        return null;
    }

    /**
     * Returns the space in pixels between the edge of the watermark and the
     * edge of the image.
     *
     * @return Watermark inset, defaulting to 0 if
     *         {@link WatermarkService#WATERMARK_INSET_CONFIG_KEY} is not set.
     */
    public static int getWatermarkInset() {
        final Configuration config = Application.getConfiguration();
        try {
            final int configValue = config.
                    getInt(WATERMARK_INSET_CONFIG_KEY, 0);
            if (configValue > 0) {
                return configValue;
            }
        } catch (ConversionException e) {
            logger.error("Invalid {} value", WATERMARK_INSET_CONFIG_KEY);
        }
        return 0;
    }

    /**
     * @return Watermark position, or null if
     *         {@link WatermarkService#WATERMARK_POSITION_CONFIG_KEY} is not set.
     */
    public static Position getWatermarkPosition() {
        final Configuration config = Application.getConfiguration();
        final String configValue = config.
                getString(WATERMARK_POSITION_CONFIG_KEY, "");
        if (configValue.length() > 0) {
            final String enumStr = StringUtils.replace(configValue, " ", "_").
                    toUpperCase();
            try {
                return Position.valueOf(enumStr);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid {} value: {}",
                        WATERMARK_POSITION_CONFIG_KEY, configValue);
            }
        }
        return null;
    }

    /**
     * @return Whether {@link #WATERMARK_ENABLED_CONFIG_KEY} is true.
     */
    public static boolean isEnabled() {
        return Application.getConfiguration().
                getBoolean(WATERMARK_ENABLED_CONFIG_KEY, false);
    }

}
