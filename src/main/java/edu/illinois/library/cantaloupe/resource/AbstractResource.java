package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;
import org.restlet.data.CacheDirective;
import org.restlet.data.Header;
import org.restlet.data.Reference;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class AbstractResource extends ServerResource {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractResource.class);

    protected List<CacheDirective> getCacheDirectives() {
        List<CacheDirective> directives = new ArrayList<>();
        try {
            Configuration config = Application.getConfiguration();
            boolean enabled = config.getBoolean("cache.client.enabled", false);
            if (enabled) {
                String maxAge = config.getString("cache.client.max_age");
                if (maxAge != null && maxAge.length() > 0) {
                    directives.add(CacheDirective.maxAge(Integer.parseInt(maxAge)));
                }
                String sMaxAge = config.getString("cache.client.shared_max_age");
                if (sMaxAge != null && sMaxAge.length() > 0) {
                    directives.add(CacheDirective.
                            sharedMaxAge(Integer.parseInt(sMaxAge)));
                }
                if (config.getBoolean("cache.client.public")) {
                    directives.add(CacheDirective.publicInfo());
                } else if (config.getBoolean("cache.client.private")) {
                    directives.add(CacheDirective.privateInfo());
                }
                if (config.getBoolean("cache.client.no_cache")) {
                    directives.add(CacheDirective.noCache());
                }
                if (config.getBoolean("cache.client.no_store")) {
                    directives.add(CacheDirective.noStore());
                }
                if (config.getBoolean("cache.client.must_revalidate")) {
                    directives.add(CacheDirective.mustRevalidate());
                }
                if (config.getBoolean("cache.client.proxy_revalidate")) {
                    directives.add(CacheDirective.proxyMustRevalidate());
                }
                if (config.getBoolean("cache.client.no_transform")) {
                    directives.add(CacheDirective.noTransform());
                }
            } else {
                logger.debug("Cache-Control headers are disabled. " +
                        "(cache.client.enabled = false)");
            }
        } catch (NoSuchElementException e) {
            logger.warn("Cache-Control headers are invalid: {}",
                    e.getMessage());
        }
        return directives;
    }

    /**
     * @return A root reference usable in public, with a scheme customizable in
     * the application configuration.
     */
    protected Reference getPublicRootRef() {
        Reference rootRef = getRootRef();
        try {
            if (Application.getConfiguration().getBoolean("generate_https_links")) {
                rootRef = rootRef.clone();
                rootRef.setScheme("https");
            }
        } catch (NoSuchElementException e) {
            logger.warn("Config file is missing the generate_https_links key.");
        }
        return rootRef;
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        // override the Server header
        // TODO: this doesn't affect redirecting responses
        this.getServerInfo().setAgent("Cantaloupe/" + Application.getVersion());
    }

    /**
     * Convenience method that adds a response header.
     *
     * @param key Header key
     * @param value Header value
     */
    @SuppressWarnings({"unchecked"})
    protected void addHeader(String key, String value) {
        Series<Header> responseHeaders = (Series<Header>) getResponse().
                getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Series(Header.class);
            getResponse().getAttributes().
                    put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add(new Header(key, value));
    }

}
