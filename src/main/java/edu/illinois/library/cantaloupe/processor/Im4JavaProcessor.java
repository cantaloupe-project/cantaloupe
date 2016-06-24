package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.icc.IccProfile;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for processors that use the im4java library.
 *
 * @see <a href="http://im4java.sourceforge.net">im4java</a>
 */
abstract class Im4JavaProcessor extends AbstractProcessor {

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    protected StreamSource streamSource;

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
        SUPPORTED_IIIF_2_0_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));
        SUPPORTED_FEATURES.addAll(Arrays.asList(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.REGION_SQUARE,
                ProcessorFeature.ROTATION_ARBITRARY,
                ProcessorFeature.ROTATION_BY_90S,
                ProcessorFeature.SIZE_ABOVE_FULL,
                ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));
    }

    void assembleOperation(final IMOperation imOp,
                           final OperationList ops,
                           final Dimension fullSize,
                           final String backgroundColor) {
        for (Operation op : ops) {
            if (op instanceof Crop) {
                Crop crop = (Crop) op;
                if (!crop.isNoOp()) {
                    if (crop.getShape().equals(Crop.Shape.SQUARE)) {
                        final int shortestSide =
                                Math.min(fullSize.width, fullSize.height);
                        int x = (fullSize.width - shortestSide) / 2;
                        int y = (fullSize.height - shortestSide) / 2;
                        imOp.crop(shortestSide, shortestSide, x, y);
                    } else if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                        // im4java doesn't support cropping x/y by percentage
                        // (only width/height), so we have to calculate them.
                        int x = Math.round(crop.getX() * fullSize.width);
                        int y = Math.round(crop.getY() * fullSize.height);
                        int width = Math.round(crop.getWidth() * 100);
                        int height = Math.round(crop.getHeight() * 100);
                        imOp.crop(width, height, x, y, "%");
                    } else {
                        imOp.crop(Math.round(crop.getWidth()),
                                Math.round(crop.getHeight()),
                                Math.round(crop.getX()),
                                Math.round(crop.getY()));
                    }
                }
            } else if (op instanceof Scale) {
                Scale scale = (Scale) op;
                if (!scale.isNoOp()) {
                    if (scale.getPercent() != null) {
                        imOp.resize(Math.round(scale.getPercent() * 100),
                                Math.round(scale.getPercent() * 100), "%");
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        imOp.resize(scale.getWidth());
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        imOp.resize(null, scale.getHeight());
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        imOp.resize(scale.getWidth(), scale.getHeight(), "!");
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        imOp.resize(scale.getWidth(), scale.getHeight());
                    }
                }
            } else if (op instanceof Transpose) {
                switch ((Transpose) op) {
                    case HORIZONTAL:
                        imOp.flop();
                        break;
                    case VERTICAL:
                        imOp.flip();
                        break;
                }
            } else if (op instanceof Rotate) {
                final Rotate rotate = (Rotate) op;
                if (!rotate.isNoOp()) {
                    // If the output format supports transparency, make the
                    // background transparent. Otherwise, use a
                    // user-configurable background color.
                    if (ops.getOutputFormat().supportsTransparency()) {
                        imOp.background("none");
                    } else {
                        imOp.background(backgroundColor);
                    }
                    imOp.rotate((double) rotate.getDegrees());
                }
            } else if (op instanceof Filter) {
                switch ((Filter) op) {
                    case GRAY:
                        imOp.colorspace("Gray");
                        break;
                    case BITONAL:
                        imOp.monochrome();
                        break;
                }
            } else if (op instanceof IccProfile) {
                imOp.profile(((IccProfile) op).getFile().getAbsolutePath());
            }
        }

        if (!Configuration.getInstance().
                getBoolean(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, false)) {
            imOp.strip();
        }
    }

    public ImageInfo getImageInfo() throws ProcessorException {
        if (getAvailableOutputFormats().size() < 1) {
            throw new UnsupportedSourceFormatException(format);
        }
        try (InputStream inputStream = streamSource.newInputStream()) {
            Info sourceInfo = new Info(
                    format.getPreferredExtension() + ":-",
                    inputStream, true);
            return new ImageInfo(sourceInfo.getImageWidth(),
                    sourceInfo.getImageHeight(), sourceInfo.getImageWidth(),
                    sourceInfo.getImageHeight(), getSourceFormat());
        } catch (IM4JavaException | IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    public StreamSource getStreamSource() {
        return this.streamSource;
    }

    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    public void setStreamSource(StreamSource streamSource) {
        this.streamSource = streamSource;
    }

}
