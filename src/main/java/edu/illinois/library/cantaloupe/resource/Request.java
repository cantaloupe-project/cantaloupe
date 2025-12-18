package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.http.Cookies;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Query;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * Wraps an {@link HttpServletRequest}, adding some convenience methods.
 */
public final class Request {

    private HttpServletRequest wrappedRequest;

    private Cookies cookies;
    private Headers headers;
    private Reference reference;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Request.class);

    /**
     * URL argument values that can be used with the {@code cache} query key to
     * bypass all caching.
     */
    private static final Set<String> CACHE_BYPASS_ARGUMENTS =
            Set.of("false", "nocache");

    /**
     * @param request Request that the new instance will wrap.
     */
    Request(HttpServletRequest request) {
        this.wrappedRequest = request;
    }

    public String getContextPath() {
        return wrappedRequest.getContextPath();
    }



    public Headers getHeaders() {
        if (headers == null) {
            headers = new Headers();
            final Enumeration<String> names = wrappedRequest.getHeaderNames();
            if (names != null) {
                while (names.hasMoreElements()) {
                    final String name = names.nextElement();
                    final Enumeration<String> values = wrappedRequest.getHeaders(name);
                    while (values.hasMoreElements()) {
                        headers.add(name, values.nextElement());
                    }
                }
            }
        }
        return headers;
    }

    /**
     * @return Stream for reading the request entity.
     */
    public InputStream getInputStream() throws IOException {
        return wrappedRequest.getInputStream();
    }

    public Method getMethod() {
        return Method.valueOf(wrappedRequest.getMethod());
    }


    /**
     * @return Whether there is a {@code cache} argument set to {@code false}
     *         or {@code nocache} in the URI query string indicating that cache
     *         reads and writes are both bypassed.
     */
    public final boolean isBypassingCache() {
        String value = getReference().getQuery().getFirstValue("cache");
        return (value != null) && CACHE_BYPASS_ARGUMENTS.contains(value);
    }

    /**
     * If true, then the requestor wishes us to reprocesses and recache the derivative
     * image before delivering it
     * @return Whether there is a {@code cache} argument set to {@code recache}
     *         in the URI query string indicating that cache reads are
     *         bypassed.
     */
    public final boolean isBypassingCacheRead() {
        String value = getReference().getQuery().getFirstValue("cache");
        return "recache".equals(value);
    }

    /**
     * @return Full request URI including query. Note that this may not be the
     *         URI that the user agent supplies or sees.
     * @see AbstractResource#getPublicReference()
     */
    public Reference getReference() {
        if (reference == null) {
            reference = new Reference(wrappedRequest.getRequestURL().toString());

            String q = wrappedRequest.getQueryString();
            if (q != null && !q.isEmpty()) {
                reference.setQuery(new Query(q));
            }
        }
        return reference;
    }

        /**
     * <p>Returns a reference to the base URI path of the application.</p>
     *
     * <p>{@link Key#BASE_URI} is respected, if set. Otherwise, the {@code
     * X-Forwarded-*} request headers are respected, if available. Finally,
     * Servlet-supplied information is used otherwise.</p>
     *
     * @see #getPublicReference()
     */
    public Reference getPublicRootReference() {
        Reference ref = new Reference(getReference());
        ref.getQuery().clear();
        ref.setPath(getContextPath());

        // If base_uri is set in the configuration, build a URI based on that.
        final String baseUri = Configuration.getInstance()
                .getString(Key.BASE_URI, "");
        if (!baseUri.isEmpty()) {
            final Reference baseRef = new Reference(baseUri);
            ref.setScheme(baseRef.getScheme());
            ref.setHost(baseRef.getHost());
            ref.setPort(baseRef.getPort());
            ref.setPath(StringUtils.stripEnd(baseRef.getPath(), "/"));
            LOGGER.debug("Base URI from assembled from {} key: {}",
                    Key.BASE_URI, ref);
        } else {
            // Try to use X-Forwarded-* headers.
            ref.applyProxyHeaders(getHeaders());
            LOGGER.debug("Base URI assembled from X-Forwarded headers: {}",
                    ref);
        }
        return ref;
    }


    /**
     * Variant of {@link #getPublicReference()} that replaces the identifier
     * path component's meta-identifier if an identifier path component is
     * available.
     */
    public Reference getPublicReference(MetaIdentifier newMetaIdentifier, String identifierPathComponent, DelegateProxy delegateProxy) {
        final Reference publicRef         = new Reference(getPublicReference());
        final List<String> pathComponents = publicRef.getPathComponents();
        final int identifierIndex         = pathComponents.indexOf(identifierPathComponent);

        final String newMetaIdentifierString = newMetaIdentifier.toURIPathComponent(delegateProxy);
        publicRef.setPathComponent(identifierIndex, newMetaIdentifierString);
        return publicRef;
    }

    /**
     * <p>Returns the current public reference.</p>
     *
     * <p>{@link Key#BASE_URI} is respected, if set. Otherwise, the {@code
     * X-Forwarded-*} request headers are respected, if available. Finally,
     * Servlet-supplied information is used otherwise.</p>
     *
     * <p>Note that the return value may not be something the client is
     * expecting to see&mdash;for example, any {@link #getIdentifier()
     * identifier} present in the URI path is not {@link #getPublicIdentifier()
     * translated}.</p>
     *
     * @see #getPublicRootReference()
     */
    public Reference getPublicReference() {
        final Reference ref        = getPublicRootReference();
        final Reference requestRef = new Reference(getReference());
        final Reference appRootRef = new Reference(requestRef);
        appRootRef.setPath(getContextPath());
        final String appRootRelativePath =
                requestRef.getRelativePath(appRootRef.getPath());
        if (!appRootRelativePath.isEmpty()) {
            String path = StringUtils.stripEnd(ref.getPath(), "/") + "/" +
                    StringUtils.stripStart(appRootRelativePath, "/");
            ref.setPath(path);
        }
        return ref;
    }

    /**
     * @return Client IP address. Note that this may not be the user agent IP
     *         address, as in the case of e.g. running behind a reverse proxy
     *         server.
     * @see RequestContextDecorator#getCanonicalClientIPAddress()
     */
    public String getRemoteAddr() {
        return wrappedRequest.getRemoteAddr();
    }

    /**
     * @return Wrapped request.
     */
    public HttpServletRequest getServletRequest() {
        return wrappedRequest;
    }

}
