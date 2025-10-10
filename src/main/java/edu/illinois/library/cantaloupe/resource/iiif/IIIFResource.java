package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.auth.AuthInfo;
import edu.illinois.library.cantaloupe.auth.Authorizer;
import edu.illinois.library.cantaloupe.auth.AuthorizerFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.RequestContextDecorator;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.StringRepresentation;
import edu.illinois.library.cantaloupe.util.TimeUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class IIIFResource extends AbstractResource {

    private static final String PAGE_NUMBER_QUERY_ARG = "page";
    private static final String TIME_QUERY_ARG        = "time";

    @Override
    public void doInit() throws Exception {
        super.doInit();
        RequestContextDecorator.decorateRequestContext(
                            getRequestContext(),
                            getMetaIdentifier(),
                            getRequest().getPublicReference(),
                            getRequest());
        addHeaders();
    }

    private void addHeaders() {
        getResponse().setHeader("Access-Control-Allow-Origin", "*");
        getResponse().setHeader("Vary",
                "Accept, Accept-Charset, Accept-Encoding, Accept-Language, Origin");
        if (!getRequest().isBypassingCache()) {
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
        MetaIdentifier newMetaId = getMetaIdentifier().getNormalizedScaleConstraintMetaIdentifier();
        if (newMetaId == null) {
            return false;
        }
        Reference newRef = getRequest().getPublicReference(newMetaId, getRequest().getIdentifierPathComponent(), getDelegateProxy());
        getResponse().setStatus(301);
        getResponse().setHeader("Location", newRef.toString());
        new StringRepresentation("Redirect: " + newRef + "\n")
                .write(getResponse().getOutputStream());
        return true;
    }

    protected void setLastModifiedHeader(Instant lastModified) {
        getResponse().setHeader("Last-Modified",
                DateTimeFormatter.RFC_1123_DATE_TIME
                        .withLocale(Locale.UK)
                        .withZone(ZoneId.systemDefault())
                        .format(lastModified));
    }

    /**
     * <p>Uses an {@link Authorizer} to determine how to respond to the
     * request. The response is modified if necessary.</p>
     *
     * <p>The authorization system (rooted in the {@link
     * edu.illinois.library.cantaloupe.delegate.DelegateMethod#AUTHORIZE
     * authorization delegate method} supports simple boolean authorization
     * which maps to the HTTP 200 and 403 statuses.</p>
     *
     * <p>Authorization can simultaneously be used in the context of the
     * <a href="https://iiif.io/api/auth/1.0/">IIIF Authentication API, where
     * it works a little differently. Here, HTTP 401 is returned instead of
     * 403, and the response body <strong>does</strong> include image
     * information. (See
     * <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">
     * Interaction with Access-Controlled Resources</a>. This means that IIIF
     * information endpoints should swallow any {@link ResourceException}s with
     * HTTP 401 status.</p>
     *
     * @return Whether authorization was successful. {@code false} indicates a
     *         redirect, and client code should abort.
     * @throws IOException if there was an I/O error while checking
     *         authorization.
     * @throws ResourceException if authorization resulted in an HTTP 400-level
     *         response.
     */
    protected final boolean authorize() throws IOException, ResourceException {
        final Authorizer authorizer =
                new AuthorizerFactory().newAuthorizer(getDelegateProxy());
        final AuthInfo info = authorizer.authorize();
        if (info != null) {
            return processAuthInfo(info);
        }
        return true;
    }

    /**
     * <p>Uses an {@link Authorizer} to determine how to respond to the
     * request. The response is modified if necessary.</p>
     *
     * <p>The authorization system (rooted in the {@link
     * edu.illinois.library.cantaloupe.delegate.DelegateMethod#AUTHORIZE
     * authorization delegate method} supports simple boolean authorization
     * which maps to the HTTP 200 and 403 statuses. In the event of a 403,
     * IIIF image information should not be included in the response body.</p>
     *
     * <p>Authorization can simultaneously be used in the context of the
     * <a href="https://iiif.io/api/auth/1.0/">IIIF Authentication API, where
     * it works a little differently. Here, HTTP 401 is returned instead of
     * 403, and the response body <strong>does</strong> include image
     * information. (See
     * <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">
     * Interaction with Access-Controlled Resources</a>. This means that IIIF
     * information endpoints should swallow any {@link ResourceException}s with
     * HTTP 401 status.</p>
     *
     * @return Whether authorization was successful. {@code false} indicates a
     *         redirect, and client code should abort.
     * @throws IOException if there was an I/O error while checking
     *         authorization.
     * @throws ResourceException if authorization resulted in an HTTP 400-level
     *         response.
     */
    protected final boolean preAuthorize() throws IOException, ResourceException {
        final Authorizer authorizer =
                new AuthorizerFactory().newAuthorizer(getDelegateProxy());
        final AuthInfo info = authorizer.preAuthorize();
        if (info != null) {
            return processAuthInfo(info);
        }
        return true;
    }

    private boolean processAuthInfo(AuthInfo info)
            throws IOException, ResourceException {
        final int code                      = info.getResponseStatus();
        final String location               = info.getRedirectURI();
        final MetaIdentifier metaIdentifier = new MetaIdentifier(getMetaIdentifier());
        metaIdentifier.setScaleConstraint(info.getScaleConstraint());

        if (location != null) {
            getResponse().setStatus(code);
            getResponse().setHeader("Cache-Control", "no-cache");
            getResponse().setHeader("Location", location);
            new StringRepresentation("Redirect: " + location)
                    .write(getResponse().getOutputStream());
            return false;
        } else if (metaIdentifier.getScaleConstraint() != null) {
            Reference publicRef = getRequest().getPublicReference(metaIdentifier, getRequest().getIdentifierPathComponent(), getDelegateProxy());
            getResponse().setStatus(code);
            getResponse().setHeader("Cache-Control", "no-cache");
            getResponse().setHeader("Location", publicRef.toString());
            new StringRepresentation("Redirect: " + publicRef)
                    .write(getResponse().getOutputStream());
            return false;
        } else if (code >= 400) {
            getResponse().setStatus(code);
            getResponse().setHeader("Cache-Control", "no-cache");
            if (code == 401) {
                getResponse().setHeader("WWW-Authenticate",
                        info.getChallengeValue());
            }
            throw new ResourceException(new Status(code));
        }
        return true;
    }
}
