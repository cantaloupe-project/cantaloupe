package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

public abstract class PublicResource extends AbstractResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PublicResource.class);

    protected Future<Path> tempFileFuture;

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

    @Override
    public void destroy() {
        // If a temp file was created in the source of fulfilling the request,
        // it will need to be deleted.
        if (tempFileFuture != null) {
            try {
                Path tempFile = tempFileFuture.get();
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception e) {
                LOGGER.error("destroy(): {}", e.getMessage(), e);
            }
        }
    }

    /**
     * <p>Returns the info for the source image corresponding to the
     * given identifier as efficiently as possible.</p>
     *
     * @param identifier Image identifier.
     * @param proc       Processor from which to read the info if it can't be
     *                   retrieved from a cache.
     * @return           Info for the image with the given identifier.
     */
    protected final Info getOrReadInfo(final Identifier identifier,
                                       final Processor proc) throws IOException {
        Info info;
        if (!isBypassingCache()) {
            info = new CacheFacade().getOrReadInfo(identifier, proc).orElseThrow();
            info.setIdentifier(identifier);
        } else {
            LOGGER.debug("getOrReadInfo(): bypassing the cache, as requested");
            info = proc.readInfo();
            info.setIdentifier(identifier);
        }
        return info;
    }

    /**
     * @return Page index (a.k.a. page number - 1) from the {@literal page}
     *         query argument, or {@literal 0} if not supplied.
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
     * @return Whether there is a {@literal cache} argument set to {@literal
     *         false} in the URI query string.
     */
    protected final boolean isBypassingCache() {
        return "false".equals(getRequest().getReference().getQuery()
                .getFirstValue("cache"));
    }

    /**
     * <p>If an identifier is present in the URI, and it contains a scale
     * constraint suffix in a non-normalized form, this method will redirect to
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
     * @return {@literal true} if redirecting. Clients should stop processing
     *         if this is the case.
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
     * @param scale       May be {@literal null}.
     */
    protected void validateScale(Dimension virtualSize,
                                 Scale scale) throws ScaleRestrictedException {
        final ScaleConstraint scaleConstraint = (getScaleConstraint() != null) ?
                getScaleConstraint() : new ScaleConstraint(1, 1);
        double scalePct = scaleConstraint.getRational().doubleValue();
        if (scale != null) {
            scalePct = Arrays.stream(scale.getResultingScales(virtualSize,
                            scaleConstraint)).max().orElse(1);
        }

        final Configuration config = Configuration.getInstance();
        final double maxScale      = config.getDouble(Key.MAX_SCALE, 1.0);
        if (maxScale > 0.0001 && scalePct > maxScale) {
            throw new ScaleRestrictedException(maxScale);
        }
    }

}
