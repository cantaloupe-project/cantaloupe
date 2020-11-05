package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.util.TimeUtils;

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
    private static final String PAGE_NUMBER_QUERY_ARG = "page";
    private static final String TIME_QUERY_ARG        = "time";

    @Override
    public void doInit() throws Exception {
        super.doInit();
        if (DelegateProxyService.isDelegateAvailable()) {
            RequestContext context = getRequestContext();
            context.setLocalURI(getRequest().getReference().toURI());
            context.setRequestURI(getPublicReference().toURI());
            context.setRequestHeaders(getRequest().getHeaders().toMap());
            context.setClientIP(getCanonicalClientIPAddress());
            context.setCookies(getRequest().getCookies().toMap());
            MetaIdentifier metaID = getMetaIdentifier();
            if (metaID != null) {
                context.setIdentifier(metaID.getIdentifier());
                context.setPageNumber(metaID.getPageNumber());
                ScaleConstraint scaleConstraint = metaID.getScaleConstraint();
                if (scaleConstraint == null) {
                    // Delegate users will appreciate not having to check for
                    // null.
                    scaleConstraint = new ScaleConstraint(1, 1);
                }
                context.setScaleConstraint(scaleConstraint);
            }
        }
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
     * @return User agent's IP address, respecting the {@code X-Forwarded-For}
     *         request header, if present.
     */
    private String getCanonicalClientIPAddress() {
        // The value is expected to be in the format: "client, proxy1, proxy2"
        final String forwardedFor =
                getRequest().getHeaders().getFirstValue("X-Forwarded-For", "");
        if (!forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        } else {
            // Fall back to the client IP address.
            return getRequest().getRemoteAddr();
        }
    }

    /**
     * <p>Returns the page index (i.e. {@literal page number - 1}), which may
     * come from one of two sources, in order of preference:</p>
     *
     * <ol>
     *     <li>The {@link AbstractResource#getMetaIdentifier()
     *     meta-identifier}</li>
     *     <li>The {@link #PAGE_NUMBER_QUERY_ARG page number query argument
     *     (deprecated in 5.0)</li>
     * </ol>
     *
     * <p>If neither of those contain a page number, {@code 0} is returned.</p>
     *
     * @return Page index.
     */
    protected int getPageIndex() {
        // Check the meta-identifier.
        int index = 0;
        if (getMetaIdentifier().getPageNumber() != null) {
            index = getMetaIdentifier().getPageNumber() - 1;
        }
        if (index == 0) {
            // Check the `page` query argument (deprecated in 5.0).
            String arg = getRequest().getReference().getQuery()
                    .getFirstValue(PAGE_NUMBER_QUERY_ARG, "1");
            try {
                index = Integer.parseInt(arg) - 1;
                if (index < 0) {
                    index = 0;
                }
            } catch (NumberFormatException ignore) {
                // Client supplied a bogus page number, so use 0.
            }
            if (index == 0) {
                // Check the `time` query argument (deprecated in 5.0).
                arg = getRequest().getReference().getQuery()
                        .getFirstValue(TIME_QUERY_ARG, "00:00:00");
                try {
                    index = TimeUtils.toSeconds(arg);
                } catch (IllegalArgumentException ignore) {
                    // Client supplied a bogus time, so use 0.
                }
            }
        }
        return index;
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
     *     <dd>No redirect</dd>
     *     <dt>2:4</dt>
     *     <dd>Redirect to 1:2</dd>
     *     <dt>1:1 and 5:5</dt>
     *     <dd>Redirect to no constraint</dd>
     * </dl>
     *
     * @return {@code true} if redirecting. Clients should stop processing if
     *         this is the case.
     */
    protected final boolean redirectToNormalizedScaleConstraint()
            throws IOException {
        final MetaIdentifier metaIdentifier = getMetaIdentifier();
        // If a meta-identifier is present in the URI...
        if (metaIdentifier != null) {
            final ScaleConstraint scaleConstraint =
                    metaIdentifier.getScaleConstraint();
            // and it contains a scale constraint...
            if (scaleConstraint != null) {
                Reference newRef = null;
                // ...and the numerator and denominator are equal, redirect to
                // the non-suffixed identifier.
                if (!scaleConstraint.hasEffect()) {
                    metaIdentifier.setScaleConstraint(null);
                    newRef = getPublicReference(metaIdentifier);
                } else {
                    ScaleConstraint reducedConstraint =
                            scaleConstraint.getReduced();
                    // ...and the fraction is not reduced, redirect to the
                    // reduced version.
                    if (!reducedConstraint.equals(scaleConstraint)) {
                        metaIdentifier.setScaleConstraint(reducedConstraint);
                        newRef = getPublicReference(metaIdentifier);
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
        }
        return false;
    }

    /**
     * @param virtualSize   Orientation-aware full source image size.
     * @param scale         May be {@code null}.
     * @param invalidStatus Status code to return when the given scale fails
     *                      validation.
     */
    protected void validateScale(Dimension virtualSize,
                                 Scale scale,
                                 Status invalidStatus) throws ScaleRestrictedException {
        final ScaleConstraint scaleConstraint =
                (getMetaIdentifier().getScaleConstraint() != null) ?
                getMetaIdentifier().getScaleConstraint() : new ScaleConstraint(1, 1);
        double scalePct = scaleConstraint.getRational().doubleValue();
        if (scale != null) {
            scalePct = Arrays.stream(
                    scale.getResultingScales(virtualSize, scaleConstraint))
                    .max().orElse(1);
        }
        final Configuration config = Configuration.getInstance();
        final double maxScale      = config.getDouble(Key.MAX_SCALE, 1.0);
        if (maxScale > 0.0001 && scalePct > maxScale) {
            throw new ScaleRestrictedException(invalidStatus, maxScale);
        }
    }

}
