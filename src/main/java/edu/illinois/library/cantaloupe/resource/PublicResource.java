package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Scale;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class PublicResource extends AbstractResource {

    /**
     * URL argument values that can be used with the {@code cache} query key to
     * bypass all caching.
     */
    private static final Set<String> CACHE_BYPASS_ARGUMENTS =
            Set.of("false", "nocache");

    @Override
    public void doInit() throws Exception {
        super.doInit();
        addHeaders();
    }

    private void addHeaders() {
        getResponse().setHeader("Access-Control-Allow-Origin", "*");
        getResponse().setHeader("Vary",
                "Accept, Accept-Charset, Accept-Encoding, Accept-Language, Origin");

        if (!isBypassingCache()) {
            final Configuration config = Configuration.getInstance();
            if (config.getBoolean(Key.CLIENT_CACHE_ENABLED, false)) {
                final List<String> directives = new ArrayList<>();
                final String maxAge = config.getString(Key.CLIENT_CACHE_MAX_AGE, "");
                if (!maxAge.isEmpty()) {
                    directives.add("max-age=" + maxAge);
                }
                String sMaxAge = config.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE, "");
                if (!sMaxAge.isEmpty()) {
                    directives.add("s-maxage=" + sMaxAge);
                }
                if (config.getBoolean(Key.CLIENT_CACHE_PUBLIC, true)) {
                    directives.add("public");
                } else if (config.getBoolean(Key.CLIENT_CACHE_PRIVATE, false)) {
                    directives.add("private");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_CACHE, false)) {
                    directives.add("no-cache");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_STORE, false)) {
                    directives.add("no-store");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE, false)) {
                    directives.add("must-revalidate");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE, false)) {
                    directives.add("proxy-revalidate");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM, false)) {
                    directives.add("no-transform");
                }
                getResponse().setHeader("Cache-Control",
                        String.join(", ", directives));
            }
        }
    }

    /**
     * @return Page index (a.k.a. {@literal page number - 1}) from the {@code
     *         page} query argument, or {@code 0} if not supplied.
     */
    protected int getPageIndex() {
        String arg = getRequest().getReference().getQuery()
                .getFirstValue("page", "1");
        try {
            int index = Integer.parseInt(arg) - 1;
            if (index >= 0) {
                return index;
            }
        } catch (NumberFormatException ignore) {
        }
        return 0;
    }

    /**
     * @return Whether there is a {@code cache} argument set to {@code false}
     *         or {@code nocache} in the URI query string indicating that cache
     *         reads and writes are both bypassed.
     */
    protected final boolean isBypassingCache() {
        String value = getRequest().getReference().getQuery().getFirstValue("cache");
        return (value != null) && CACHE_BYPASS_ARGUMENTS.contains(value);
    }

    /**
     * @return Whether there is a {@code cache} argument set to {@code recache}
     *         in the URI query string indicating that cache reads are
     *         bypassed.
     */
    protected final boolean isBypassingCacheRead() {
        String value = getRequest().getReference().getQuery().getFirstValue("cache");
        return "recache".equals(value);
    }

    /**
     * <p>If an identifier is present in the URI, and it contains a scale
     * constraint suffix in a non-normalized form, this method redirects to
     * a normalized URI.</p>
     *
     * <p>Examples:</p>
     *
     * <dl>
     *     <dt>1:2</dt>
     *     <dd>no redirect</dd>
     *     <dt>2:4</dt>
     *     <dd>redirect to 1:2</dd>
     *     <dt>1:1 and 5:5</dt>
     *     <dd>redirect to no constraint</dd>
     * </dl>
     *
     * @return {@code true} if redirecting. Clients should stop processing if
     *         this is the case.
     */
    protected final boolean redirectToNormalizedScaleConstraint()
            throws IOException {
        final ScaleConstraint scaleConstraint = getScaleConstraint();
        // If an identifier is present in the URI, and it contains a scale
        // constraint suffix...
        if (scaleConstraint != null) {
            Reference newRef = null;
            // ...and the numerator and denominator are equal, redirect to the
            // non-suffixed identifier.
            if (scaleConstraint.getRational().getNumerator() ==
                    scaleConstraint.getRational().getDenominator()) {
                newRef = getPublicReference(scaleConstraint);
            } else {
                ScaleConstraint reducedConstraint = scaleConstraint.getReduced();
                // ...and the fraction is not reduced, redirect to the reduced
                // version.
                if (!reducedConstraint.equals(scaleConstraint)) {
                    newRef = getPublicReference(reducedConstraint);
                }
            }
            if (newRef != null) {
                getResponse().setStatus(301);
                getResponse().setHeader("Location", newRef.toString());
                new StringRepresentation("Redirect: " + newRef + "\n")
                        .write(getResponse().getOutputStream());
                return true;
            }
        }
        return false;
    }

    /**
     * @param virtualSize Orientation-aware full source image size.
     * @param scale       May be {@code null}.
     */
    protected void validateScale(Dimension virtualSize,
                                 Scale scale) throws ScaleRestrictedException {
        final Configuration config = Configuration.getInstance();
        final double maxScale      = config.getDouble(Key.MAX_SCALE, 1.0);
        if (maxScale > 0.0001) { // A maxScale of 0 indicates no max.
            ScaleConstraint scaleConstraint = (getScaleConstraint() != null) ?
                    getScaleConstraint() : new ScaleConstraint(1, 1);
            double scalePct = scaleConstraint.getRational().doubleValue();
            if (scale != null) {
                scalePct = Arrays.stream(scale.getResultingScales(virtualSize,
                        scaleConstraint)).max().orElse(1);
            }
            if (scalePct > maxScale) {
                throw new ScaleRestrictedException(maxScale);
            }
        }
    }

}
