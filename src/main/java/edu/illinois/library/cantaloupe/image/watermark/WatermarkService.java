package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dimension;
import java.io.File;

/**
 * Provides information about watermarking, including whether it is enabled,
 * and access to new {@link Watermark} instances, if so.
 */
public abstract class WatermarkService {

    public static final String WATERMARK_ENABLED_CONFIG_KEY =
            "watermark.enabled";
    public static final String WATERMARK_FILE_CONFIG_KEY = "watermark.image";
    public static final String WATERMARK_INSET_CONFIG_KEY = "watermark.inset";
    public static final String WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY =
            "watermark.output_height_threshold";
    public static final String WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY =
            "watermark.output_width_threshold";
    public static final String WATERMARK_POSITION_CONFIG_KEY =
            "watermark.position";

    /**
     * Factory method.
     *
     * @return Watermark corresponding to the application configuration.
     * @throws ConfigurationException
     */
    public static Watermark newWatermark()
            throws ConfigurationException {
        return new Watermark(getWatermarkImage(), getWatermarkPosition(),
                getWatermarkInset());
    }

    /**
     * @return File corresponding to
     *         {@link WatermarkService#WATERMARK_FILE_CONFIG_KEY}, or null if
     *         it is not set.
     * @throws ConfigurationException
     */
    private static File getWatermarkImage() throws ConfigurationException {
        final Configuration config = Application.getConfiguration();
        final String path = config.
                getString(WATERMARK_FILE_CONFIG_KEY, "");
        if (path.length() > 0) {
            return new File(path);
        }
        throw new ConfigurationException(
                WATERMARK_FILE_CONFIG_KEY + " is not set.");
    }

    /**
     * Returns the space in pixels between the edge of the watermark and the
     * edge of the image.
     *
     * @return Watermark inset, defaulting to 0 if
     *         {@link WatermarkService#WATERMARK_INSET_CONFIG_KEY} is not set.
     * @throws ConfigurationException
     */
    private static int getWatermarkInset() throws ConfigurationException {
        final Configuration config = Application.getConfiguration();
        try {
            final int configValue = config.
                    getInt(WATERMARK_INSET_CONFIG_KEY, 0);
            if (configValue > 0) {
                return configValue;
            }
        } catch (ConversionException e) {
            throw new ConfigurationException(e.getMessage());
        }
        throw new ConfigurationException(
                WATERMARK_INSET_CONFIG_KEY + " is not set.");
    }

    /**
     * @return Watermark position, or null if
     *         {@link WatermarkService#WATERMARK_POSITION_CONFIG_KEY} is not
     *         set.
     * @throws ConfigurationException
     */
    private static Position getWatermarkPosition()
            throws ConfigurationException {
        final Configuration config = Application.getConfiguration();
        final String configValue = config.
                getString(WATERMARK_POSITION_CONFIG_KEY, "");
        if (configValue.length() > 0) {
            final String enumStr = StringUtils.replace(configValue, " ", "_").
                    toUpperCase();
            try {
                return Position.valueOf(enumStr);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(
                        "Invalid " + WATERMARK_POSITION_CONFIG_KEY +
                                " value: " + configValue);
            }
        }
        throw new ConfigurationException(
                WATERMARK_POSITION_CONFIG_KEY + " is not set.");
    }

    /**
     * @return Whether {@link #WATERMARK_ENABLED_CONFIG_KEY} is true.
     */
    public static boolean isEnabled() {
        return Application.getConfiguration().
                getBoolean(WATERMARK_ENABLED_CONFIG_KEY, false);
    }

    /**
     * @param outputImageSize
     * @return Whether a watermark should be applied to an output image with
     * the given dimensions.
     */
    public static boolean shouldApplyToImage(Dimension outputImageSize) {
        final Configuration config = Application.getConfiguration();
        final int minOutputWidth =
                config.getInt(WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 0);
        final int minOutputHeight =
                config.getInt(WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 0);
        return (outputImageSize.width > minOutputWidth &&
                outputImageSize.height > minOutputHeight);
    }

}
