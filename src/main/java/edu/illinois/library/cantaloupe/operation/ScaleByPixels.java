package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.util.Arrays;
import java.util.Objects;

public class ScaleByPixels extends Scale implements Operation {

    public enum Mode {

        /**
         * Scale to fit the X axis inside a rectangle's X axis, maintaining
         * aspect ratio.
         */
        ASPECT_FIT_WIDTH,

        /**
         * Scale to fit the Y axis inside a rectangle's Y axis, maintaining
         * aspect ratio.
         */
        ASPECT_FIT_HEIGHT,

        /**
         * Scale to fit entirely inside a rectangle, maintaining aspect ratio.
         */
        ASPECT_FIT_INSIDE,

        /**
         * Fill an arbitrary rectangle without regard to aspect ratio.
         */
        NON_ASPECT_FILL
    }

    /**
     * One or the other may be {@code null}.
     */
    private Integer width, height;

    private Mode scaleMode;

    /**
     * No-op constructor.
     */
    public ScaleByPixels() {
    }

    /**
     * @param width  May be {@code null} if {@literal mode} is {@link
     *               Mode#ASPECT_FIT_HEIGHT}.
     * @param height May be {@code null} if {@literal mode} is {@link
     *               Mode#ASPECT_FIT_WIDTH}.
     * @param mode   Scale mode.
     */
    public ScaleByPixels(Integer width, Integer height, Mode mode) {
        this();
        setWidth(width);
        setHeight(height);
        setMode(mode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ScaleByPixels) {
            ScaleByPixels other = (ScaleByPixels) obj;
            return Objects.equals(other.getWidth(), getWidth()) &&
                    Objects.equals(other.getHeight(), getHeight()) &&
                    Objects.equals(other.getMode(), getMode()) &&
                    Objects.equals(other.getFilter(), getFilter());
        }
        return super.equals(obj);
    }

    /**
     * This should not be used when figuring out how to apply an instance to an
     * image. For that, {@link #getResultingSize} and {@link
     * #getResultingScales} should be used instead.
     *
     * @return Absolute pixel height. May be {@code null}.
     */
    public Integer getHeight() {
        return height;
    }

    public Mode getMode() {
        return scaleMode;
    }

    @Override
    public ReductionFactor getReductionFactor(final Dimension reducedSize,
                                              final ScaleConstraint scaleConstraint,
                                              final int maxFactor) {
        final double scScale = scaleConstraint.getRational().doubleValue();
        ReductionFactor rf   = new ReductionFactor();

        switch (getMode()) {
            case ASPECT_FIT_WIDTH:
                double hvScale = getWidth() / reducedSize.width() * scScale;
                rf = ReductionFactor.forScale(hvScale);
                break;
            case ASPECT_FIT_HEIGHT:
                hvScale = getHeight() / reducedSize.height() * scScale;
                rf = ReductionFactor.forScale(hvScale);
                break;
            case ASPECT_FIT_INSIDE:
                double hScale = getWidth() / reducedSize.width() * scScale;
                double vScale = getHeight() / reducedSize.height() * scScale;
                rf = ReductionFactor.forScale(Math.min(hScale, vScale));
                break;
        }
        if (rf.factor > maxFactor) {
            rf.factor = maxFactor;
        }
        return rf;
    }

    @Override
    public double[] getResultingScales(Dimension fullSize,
                                       ScaleConstraint scaleConstraint) {
        final double[] result = new double[2];

        switch (getMode()) {
            case ASPECT_FIT_HEIGHT:
                result[0] = result[1] = getHeight() / fullSize.height();
                break;
            case ASPECT_FIT_WIDTH:
                result[0] = result[1] = getWidth() / fullSize.width();
                break;
            case ASPECT_FIT_INSIDE:
                result[0] = result[1] = Math.min(
                        getWidth() / fullSize.width(),
                        getHeight() / fullSize.height());
                if (result[0] > getMaxScale() || result[1] > getMaxScale()) {
                    result[0] = result[1] = getMaxScale();
                }
                break;
            default:
                result[0] = getWidth() / fullSize.width();
                result[1] = getHeight() / fullSize.height();
                break;
        }
        return result;
    }

    /**
     * @param imageSize       Image whose dimensions have been halved {@link
     *                        ReductionFactor#factor} times.
     * @param reductionFactor Number of times the given dimensions have been
     *                        halved.
     * @param scaleConstraint Scale constraint relative to the full source
     *                        image dimensions.
     * @return                Resulting dimensions when the scale is applied to
     *                        the constrained view of the given full size.
     */
    @Override
    public Dimension getResultingSize(Dimension imageSize,
                                      ReductionFactor reductionFactor,
                                      ScaleConstraint scaleConstraint) {
        final Dimension size = new Dimension(imageSize);

        switch (getMode()) {
            case ASPECT_FIT_HEIGHT:
                double scalePct = getHeight() / size.height();
                size.setWidth(size.width() * scalePct);
                size.setHeight(size.height() * scalePct);
                break;
            case ASPECT_FIT_WIDTH:
                scalePct = getWidth() / size.width();
                size.setWidth(size.width() * scalePct);
                size.setHeight(size.height() * scalePct);
                break;
            case ASPECT_FIT_INSIDE:
                scalePct = Math.min(
                        getWidth() / size.width(),
                        getHeight() / size.height());
                size.setWidth(size.width() * scalePct);
                size.setHeight(size.height() * scalePct);
                if ((size.width() / imageSize.width() > getMaxScale() ||
                        size.height() / imageSize.height() > getMaxScale())) {
                    size.setWidth(imageSize.width());
                    size.setHeight(imageSize.height());
                }
                break;
            case NON_ASPECT_FILL:
                size.setWidth(getWidth());
                size.setHeight(getHeight());
                break;
        }
        return size;
    }

    /**
     * This should not be used when figuring out how to apply an instance to an
     * image. For that, {@link #getResultingSize} and {@link
     * #getResultingScales} should be used instead.
     *
     * @return Absolute pixel width. May be {@code null}.
     */
    public Integer getWidth() {
        return width;
    }

    @Override
    public boolean hasEffect() {
        return getHeight() != null || getWidth() != null;
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        if (opList.getScaleConstraint().hasEffect()) {
            return true;
        }

        Dimension cropSize = fullSize;
        for (Operation op : opList) {
            if (op instanceof Crop) {
                cropSize = op.getResultingSize(
                        cropSize, opList.getScaleConstraint());
            }
        }

        switch (getMode()) {
            case ASPECT_FIT_WIDTH:
                return (Math.abs(getWidth() - cropSize.width()) > DELTA);
            case ASPECT_FIT_HEIGHT:
                return (Math.abs(getHeight() - cropSize.height()) > DELTA);
            default:
                return (Math.abs(getWidth() - cropSize.width()) > DELTA ||
                        Math.abs(getHeight() - cropSize.height()) > DELTA);
        }
    }

    @Override
    public int hashCode() {
        int[] codes = new int[4];
        codes[0] = getWidth();
        codes[1] = getHeight();
        codes[2] = (getMode() != null) ? getMode().hashCode() : 0;
        codes[3] = (getFilter() != null) ? getFilter().hashCode() : 0;
        return Arrays.hashCode(codes);
    }

    /**
     * @param height Integer greater than 0. May be {@code null}.
     * @throws IllegalArgumentException if the given height is invalid.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setHeight(Integer height) {
        checkFrozen();
        if (height != null && height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * @param scaleMode Scale mode to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setMode(Mode scaleMode) {
        checkFrozen();
        this.scaleMode = scaleMode;
    }

    /**
     * @param width Integer greater than 0. May be {@code null}.
     * @throws IllegalArgumentException If the given width is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setWidth(Integer width) {
        checkFrozen();
        if (width != null && width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    /**
     * <p>Returns a string representation of the instance, guaranteed to
     * uniquely represent the instance. The format is:</p>
     *
     * <dl>
     *     <dt>No-op</dt>
     *     <dd>{@literal none}</dd>
     *     <dt>Aspect-fit-inside</dt>
     *     <dd>{@literal !w,h(,filter)}</dd>
     *     <dt>Other</dt>
     *     <dd>{@literal w,h(,filter)}</dd>
     * </dl>
     *
     * @return String representation of the instance.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (!hasEffect()) {
            builder.append("none");
        } else {
            if (Mode.ASPECT_FIT_INSIDE.equals(getMode())) {
                builder.append("!");
            }
            if (getWidth() != null && getWidth() > 0) {
                builder.append(getWidth());
            }
            builder.append(",");
            if (getHeight() != null && getHeight() > 0) {
                builder.append(getHeight());
            }
        }
        if (getFilter() != null) {
            builder.append(",");
            builder.append(getFilter().name().toLowerCase());
        }
        return builder.toString();
    }

}
