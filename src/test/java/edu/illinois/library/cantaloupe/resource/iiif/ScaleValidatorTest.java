package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.resource.ScaleRestrictedException;
import edu.illinois.library.cantaloupe.test.BaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScaleValidatorTest extends BaseTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Reporter's context: max_scale = 1.0 (cantaloupe.properties.sample default).
        Configuration.getInstance().setProperty(Key.MAX_SCALE, 1.0);
    }

    /**
     * Issue #953: IIIF v2 {@code !w,h} (max-constrained) against a source
     * smaller than the requested box, with {@code max_scale = 1.0}.
     *
     * <p>Per IIIF Image API 2.0 §4.2: {@code !w,h} produces a result that is
     * "less than or equal to" the requested w,h. Source-size is a valid
     * output when source &lt; box.</p>
     *
     * <p>Per IIIF Image API 3.0 §4.2: {@code !w,h} "must be as large as
     * possible but not larger than the extracted region" — explicit
     * no-upscale.</p>
     *
     * <p>v2 {@link edu.illinois.library.cantaloupe.resource.iiif.v2.Size#toScale()}
     * marks the resulting {@link ScaleByPixels} with
     * {@code setUpscaleAllowed(false)}; the validator must then accept the
     * request because the effective scale is clamped to 1.0 (no upscaling
     * actually occurs).</p>
     */
    @Test
    void validateScaleAllowsAspectFitInsideWhenSourceSmallerThanBoxAndUpscaleDisallowed() {
        Dimension sourceSize = new Dimension(300, 300);
        ScaleByPixels scale  = new ScaleByPixels(
                600, 600, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        // This is what v2/Size.toScale() and v3/Size.toScale() (no caret)
        // will now set on the scale before handing it to the validator.
        scale.setUpscaleAllowed(false);
        MetaIdentifier metaId = new MetaIdentifier("test");

        assertDoesNotThrow(() -> ScaleValidator.validateScale(
                sourceSize, scale, Status.FORBIDDEN, metaId));
    }

    /**
     * Caret-prefixed v3 {@code ^!w,h} explicitly permits upscaling. When the
     * source is smaller than the box and {@code max_scale} is generous
     * enough, the scale must be allowed.
     */
    @Test
    void validateScaleAllowsAspectFitInsideWhenUpscaleAllowedAndMaxScalePermits() {
        Configuration.getInstance().setProperty(Key.MAX_SCALE, 2.0);
        Dimension sourceSize = new Dimension(300, 300);
        ScaleByPixels scale  = new ScaleByPixels(
                600, 600, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        // ^!w,h: upscaling allowed.
        scale.setUpscaleAllowed(true);
        MetaIdentifier metaId = new MetaIdentifier("test");

        assertDoesNotThrow(() -> ScaleValidator.validateScale(
                sourceSize, scale, Status.BAD_REQUEST, metaId));
    }

    /**
     * Default behaviour (no IIIF caller, direct Java API): backward-compat
     * means the implicit upscale-allowed default still trips the validator
     * when {@code max_scale = 1.0} and the requested box exceeds source.
     * This is the pre-#953 behaviour for any non-IIIF caller and must be
     * preserved.
     */
    @Test
    void validateScaleStillRejectsDirectApiUpscaleAttemptWhenMaxScaleIsOne() {
        Dimension sourceSize = new Dimension(300, 300);
        // No setUpscaleAllowed() call: default is true.
        ScaleByPixels scale  = new ScaleByPixels(
                600, 600, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        MetaIdentifier metaId = new MetaIdentifier("test");

        assertThrows(ScaleRestrictedException.class, () -> ScaleValidator.validateScale(
                sourceSize, scale, Status.FORBIDDEN, metaId));
    }

    /**
     * Rendering backstop: with {@code upscaleAllowed=false},
     * {@link ScaleByPixels#getResultingSize} clamps to source size so the
     * rendering pipeline produces a 300×300 image (not a 600×600 upscale).
     */
    @Test
    void getResultingSizeClampsToSourceWhenUpscaleDisallowed() {
        Dimension sourceSize = new Dimension(300, 300);
        ScaleByPixels scale  = new ScaleByPixels(
                600, 600, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        scale.setUpscaleAllowed(false);

        Dimension result = scale.getResultingSize(
                sourceSize, new ScaleConstraint(1, 1));
        assertEquals(300, result.intWidth());
        assertEquals(300, result.intHeight());
    }

    /**
     * Rendering backstop: with the default {@code upscaleAllowed=true},
     * {@link ScaleByPixels#getResultingSize} still scales to box (600×600).
     * This is the {@code ^!w,h} (caret) semantics and must be preserved.
     */
    @Test
    void getResultingSizeUpscalesWhenUpscaleAllowed() {
        Dimension sourceSize = new Dimension(300, 300);
        ScaleByPixels scale  = new ScaleByPixels(
                600, 600, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        scale.setUpscaleAllowed(true);

        Dimension result = scale.getResultingSize(
                sourceSize, new ScaleConstraint(1, 1));
        assertEquals(600, result.intWidth());
        assertEquals(600, result.intHeight());
    }
}
