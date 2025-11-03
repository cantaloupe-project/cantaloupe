package edu.illinois.library.cantaloupe.resource;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.delegate.UnavailableException;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Query;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.util.StringUtils;
import edu.illinois.library.cantaloupe.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public class IIIFRequest {

    /**
     * Cached by {@link #getIdentifier()}.
     */
    private Identifier identifier;

    /**
     * Cached by {@link #getMetaIdentifier()}.
     */
    private MetaIdentifier metaIdentifier;

    /**
     * Set by {@link #getDelegateProxy()}.
     */
    private DelegateProxy delegateProxy;

    public IIIFRequest(HttpServletRequest request, List<String> pathArguments, Configuration configuration) {
        this.wrappedRequest = request;
        this.pathArguments = pathArguments;
        this.configuration = configuration;
    }

        /**
     * <p>Returns the decoded identifier path component of the URI. (This may
     * not be the identifier that the client supplies or sees; for that, use
     * {@link #getPublicIdentifier()}.)</p>
     *
     * <p>N.B.: Depending on the image request endpoint API, The return value
     * may include "meta-information" that is not part of the identifier but is
     * encoded along with it. In that case, it is not safe to consume via this
     * method, and {@link #getMetaIdentifier()} should be used instead.</p>
     *
     * @return Identifier, or {@code null} if the URI does not have an
     *         identifier path component.
     * @see #getMetaIdentifier()
     * @see #getPublicIdentifier()
     */
    public Identifier getIdentifier() {
        if (identifier == null) {
            String pathComponent = getIdentifierPathComponent();
            if (pathComponent != null) {
                identifier = Identifier.fromURIPathComponent(pathComponent);
            }
        }
        return identifier;
    }

    public static final String PUBLIC_IDENTIFIER_HEADER = "X-Forwarded-ID";


    /**
     * <p>Returns the identifier that the client sees. This will be the value
     * of the {@link #PUBLIC_IDENTIFIER_HEADER} header, if available, or else
     * the {@code identifier} URI path component.</p>
     *
     * <p>The result is not decoded, as the encoding may be influenced by
     * {@link Key#SLASH_SUBSTITUTE}, for example.</p>
     *
     * @see #getIdentifier()
     */
    public String getPublicIdentifier() {
        return getHeaders().getFirstValue(
                PUBLIC_IDENTIFIER_HEADER,
                getIdentifierPathComponent());
    }


    /**
     * <p>Returns the first {@link #getPathArguments() path argument}. (Most
     * resources have an identifier as the first path argument, so this will
     * work for them, but if not, an override will be necessary.)</p>
     *
     * <p>The result is not decoded and may be a {@link MetaIdentifier
     * meta-identifier}. As such, it is not usable without additional
     * processing.</p>
     *
     * @return Identifier, or {@code null} if no path arguments are
     *         available.
     */
    public String getIdentifierPathComponent() {
        List<String> args = getPathArguments();
        return (!args.isEmpty()) ? args.get(0) : null;
    }


    /**
     * Returns the decoded identifier path component of the URI, which may
     * include page number or other information. (This may not be the path
     * component that the client supplies or sees; for that, use {@link
     * #getPublicIdentifier()}.)
     *
     * @return Instance corresponding to the first {@link Request#getPathArguments()
     *         path argument}, or {@code null} if no path arguments are
     *         available.
     * @see Request#getIdentifier()
     * @see Request#getPublicIdentifier()
     */
    public MetaIdentifier getMetaIdentifier() {
        if (metaIdentifier == null) {
            String pathComponent = getIdentifierPathComponent();
            if (pathComponent != null) {
                metaIdentifier = MetaIdentifier.fromURIPathComponent(
                        pathComponent, getDelegateProxy());
                metaIdentifier.freeze();
            }
        }
        return metaIdentifier;
    }


    /**
     * @return Instance for the current request. The result is cached. May be
     *         {@code null}.
     */
    public final DelegateProxy getDelegateProxy() {
        DelegateProxyService service = DelegateProxyService.getInstance();
        if (delegateProxy == null && service.isDelegateAvailable()) {
            try {
                delegateProxy = service.newDelegateProxy(getRequestContext());
            } catch (UnavailableException e) {
                LOGGER.debug("newDelegateProxy(): {}", e.getMessage());
            }
        }
        return delegateProxy;
    }

    private static final String PAGE_NUMBER_QUERY_ARG = "page";
    private static final String TIME_QUERY_ARG        = "time";

    /**
     * <p>Returns the page index (i.e. {@literal page number - 1}), which may
     * come from one of two sources, in order of preference:</p>
     *
     * <ol>
     *     <li>The {@link IIIFRequest#getMetaIdentifier()
     *     meta-identifier}</li>
     *     <li>The {@link #PAGE_NUMBER_QUERY_ARG page number query argument
     *     (deprecated in 5.0)</li>
     * </ol>
     *
     * <p>If neither of those contain a page number, {@code 0} is returned.</p>
     *
     * @return Page index.
     */
    public int getPageIndex() {
        // Check the meta-identifier.
        int index = 0;
        if (getMetaIdentifier().getPageNumber() != null) {
            index = getMetaIdentifier().getPageNumber() - 1;
        }
        if (index == 0) {
            // Check the `page` query argument (deprecated in 5.0).
            String arg = getReference().getQuery()
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
                arg = getReference().getQuery()
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


    private HttpServletRequest wrappedRequest;
    private Headers headers;
    private Reference reference;
    private List<String> pathArguments;
    private final RequestContext requestContext = new RequestContext();

    protected static final Logger LOGGER =
            LoggerFactory.getLogger(IIIFRequest.class);

    /**
     * URL argument values that can be used with the {@code cache} query key to
     * bypass all caching.
     */
    private static final Set<String> CACHE_BYPASS_ARGUMENTS =
            Set.of("false", "nocache");

    private Configuration configuration;


    public String getContextPath() {
        return wrappedRequest.getContextPath();
    }

    /**
     * Returns the segments of the URI path that are considered arguments.
     * (These may correspond to regex match groups in {@link Route}.)
     *
     * @return Path arguments, or an empty list if there are none.
     */
    private final List<String> getPathArguments() {
        return pathArguments;
    }

    /**
     * @return Instance with basic info already set.
     */
    public final RequestContext getRequestContext() {
        return requestContext;
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
     * @see IIIFRequest#getPublicReference()
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
        final String baseUri = configuration.getString(Key.BASE_URI, "");
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
