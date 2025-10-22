package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
import edu.illinois.library.cantaloupe.resource.RequestContextDecorator;
import edu.illinois.library.cantaloupe.resource.StringRepresentation;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class IIIFResource extends AbstractResource {


    @Override
    public void doInit() throws Exception {
        super.doInit();
        RequestContextDecorator.decorateRequestContext(getRequest());
        addHeaders();
    }

    @Override
    protected IIIFRequest getRequest() {
        return (IIIFRequest) super.getRequest();
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
        MetaIdentifier newMetaId = getRequest().getMetaIdentifier().getNormalizedScaleConstraintMetaIdentifier();
        if (newMetaId == null) {
            return false;
        }
        Reference newRef = getRequest().getPublicReference(newMetaId, getRequest().getIdentifierPathComponent(), getRequest().getDelegateProxy());
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

}
