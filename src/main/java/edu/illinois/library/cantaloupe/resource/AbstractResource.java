package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.auth.AuthInfo;
import edu.illinois.library.cantaloupe.auth.Authorizer;
import edu.illinois.library.cantaloupe.auth.AuthorizerFactory;
import edu.illinois.library.cantaloupe.auth.CredentialStore;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.delegate.UnavailableException;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Abstract HTTP resource. Instances should subclass and override one or
 * more of the HTTP-method-specific methods {@link #doGET()} etc., and may
 * optionally use {@link #doInit()} and {@link #destroy()}.</p>
 *
 * <p>Unlike {@link jakarta.servlet.http.HttpServlet}s, instances are only used
 * once and not shared across threads.</p>
 */
public abstract class AbstractResource {

    public static final String PUBLIC_IDENTIFIER_HEADER = "X-Forwarded-ID";

    static final String RESPONSE_CONTENT_DISPOSITION_QUERY_ARG =
            "response-content-disposition";

    /**
     * Set by {@link #getDelegateProxy()}.
     */
    private DelegateProxy delegateProxy;

    private List<String> pathArguments          = Collections.emptyList();
    private final RequestContext requestContext = new RequestContext();
    private Request request;
    private HttpServletResponse response;

    /**
     * Cached by {@link #getIdentifier()}.
     */
    private Identifier identifier;

    /**
     * Cached by {@link #getMetaIdentifier()}.
     */
    private MetaIdentifier metaIdentifier;

    /**
     * <p>Returns a sanitized value for a {@code Content-Disposition} header
     * based on the value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     * query argument.</p>
     *
     * <p>If the disposition is {@code attachment} and the filename is not
     * set, it is set to a reasonable value based on the given identifier and
     * output format.</p>
     *
     * @param queryArg      Value of the unsanitized {@link
     *                      #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG} query
     *                      argument.
     * @param identifierStr Identifier or meta-identifier.
     * @param outputFormat  Output format.
     * @return              Value for a {@code Content-Disposition} header,
     *                      which may be {@code null}.
     */
    private static String getSafeContentDisposition(String queryArg,
                                                    String identifierStr,
                                                    Format outputFormat) {
        String disposition = null;
        if (queryArg != null) {
            queryArg = URLDecoder.decode(queryArg, StandardCharsets.UTF_8);
            if (queryArg.startsWith("inline")) {
                disposition = "inline; filename=\"" +
                        safeContentDispositionFilename(identifierStr, outputFormat) + "\"";
            } else if (queryArg.startsWith("attachment")) {
                final List<String> dispositionParts = new ArrayList<>(3);
                dispositionParts.add("attachment");

                // Check for ISO-8859-1 filename pattern
                Pattern pattern = Pattern.compile(".*filename=\"?([^\"]*)\"?.*");
                Matcher matcher = pattern.matcher(queryArg);
                String filename;
                if (matcher.matches()) {
                    // Filter out filename-unsafe characters as well as "..".
                    filename = StringUtils.sanitize(
                            matcher.group(1),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtils.ASCII_FILENAME_UNSAFE_REGEX));
                } else {
                    filename = safeContentDispositionFilename(identifierStr,
                            outputFormat);
                }
                dispositionParts.add("filename=\"" + filename + "\"");

                // Check for Unicode filename pattern
                pattern = Pattern.compile(".*filename\\*= ?(utf-8|UTF-8)''([^\"]*).*");
                matcher = pattern.matcher(queryArg);
                if (matcher.matches()) {
                    // Filter out filename-unsafe characters as well as "..".
                    filename = StringUtils.sanitize(
                            matcher.group(2),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtils.UNICODE_FILENAME_UNSAFE_REGEX,
                                    Pattern.UNICODE_CHARACTER_CLASS));
                    filename = Reference.encode(filename);
                    dispositionParts.add("filename*= UTF-8''" + filename);
                }
                disposition = String.join("; ", dispositionParts);
            }
        }
        return disposition;
    }

    private static String safeContentDispositionFilename(String identifierStr,
                                                         Format outputFormat) {
        return identifierStr.replaceAll(StringUtils.ASCII_FILENAME_UNSAFE_REGEX, "_") +
                "." + outputFormat.getPreferredExtension();
    }

    /**
     * <p>Initialization method, called after all necessary setters have been
     * called but before any request-handler method (like {@link #doGET()}
     * etc.)</p>
     *
     * <p>Overrides must call {@code super}.</p>
     */
    public void doInit() throws Exception {
        response.setHeader("X-Powered-By",
                Application.getName() + "/" + Application.getVersion());
        // Log request info.
        getLogger().info("Handling {} {}",
                request.getMethod(), request.getReference().getPath());
        getLogger().debug("Request headers: {}",
                request.getHeaders().stream()
                        .map(h -> h.getName() + ": " +
                                ("Authorization".equals(h.getName()) ? "******" : h.getValue()))
                        .collect(Collectors.joining("; ")));
    }

    /**
     * <p>Called at the end of the instance's lifecycle.</p>
     *
     * <p>Overrides must call {@code super}.</p>
     */
    public void destroy() {
    }

    /**
     * <p>Must be overridden by implementations that support {@literal
     * DELETE}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     */
    public void doDELETE() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
    }

    /**
     * <p>Must be overridden by implementations that support {@literal GET}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     */
    public void doGET() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
    }

    /**
     * This implementation simply calls {@link #doGET}. When that is
     * overridden, this may also be overridden in order to set headers only and
     * not compute a response body.
     */
    public void doHEAD() throws Exception {
        doGET();
    }

    /**
     * May be overridden by implementations that support {@literal OPTIONS}.
     */
    protected void doOPTIONS() {
        Method[] methods = getSupportedMethods();
        if (methods.length > 0) {
            response.setStatus(Status.NO_CONTENT.getCode());
            response.setHeader("Allow", Arrays.stream(methods)
                    .map(Method::toString)
                    .collect(Collectors.joining(",")));
        } else {
            response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
        }
    }

    /**
     * <p>Must be overridden by implementations that support {@literal
     * POST}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     */
    public void doPOST() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
    }

    /**
     * <p>Must be overridden by implementations that support {@literal PUT}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     */
    public void doPUT() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
    }

    /**
     * Checks the {@code Authorization} header for credentials that exist in
     * the given {@link CredentialStore}. If not found, sends a {@code
     * WWW-Authenticate} header and throws an exception.
     *
     * @param realm           Basic realm.
     * @param credentialStore Credential store.
     * @throws ResourceException if authentication failed.
     */
    protected final void authenticateUsingBasic(String realm,
                                                CredentialStore credentialStore)
            throws ResourceException {
        boolean isAuthenticated = false;
        String header = getRequest().getHeaders().getFirstValue("Authorization", "");
        if ("Basic ".equals(header.substring(0, Math.min(header.length(), 6)))) {
            String encoded = header.substring(6);
            String decoded = new String(Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length == 2) {
                String user = parts[0];
                String secret = parts[1];
                if (secret.equals(credentialStore.getSecret(user))) {
                    isAuthenticated = true;
                }
            }
        }
        if (!isAuthenticated) {
            getResponse().setHeader("WWW-Authenticate",
                    "Basic realm=\"" + realm + "\" charset=\"UTF-8\"");
            throw new ResourceException(Status.UNAUTHORIZED);
        }
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
            Reference publicRef = getPublicReference(metaIdentifier);
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

    /**
     * @return Template variables common to most or all templates, such as
     *         variables that appear in a common header.
     */
    protected final Map<String, Object> getCommonTemplateVars() {
        final Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        try {
            String baseURI = getPublicRootReference().toString();
            // Normalize the base URI. Note that the <base> tag will need it to
            // have a trailing slash.
            if (baseURI.endsWith("/")) {
                baseURI = baseURI.substring(0, baseURI.length() - 2);
            }
            vars.put("baseUri", baseURI);
        } catch (IllegalArgumentException e) {
            throw new IllegalClientArgumentException(e);
        }
        return vars;
    }

    /**
     * @return Instance for the current request. The result is cached. May be
     *         {@code null}.
     */
    protected final DelegateProxy getDelegateProxy() {
        if (delegateProxy == null && DelegateProxyService.isDelegateAvailable()) {
            DelegateProxyService service = DelegateProxyService.getInstance();
            try {
                delegateProxy = service.newDelegateProxy(getRequestContext());
            } catch (UnavailableException e) {
                getLogger().debug("newDelegateProxy(): {}", e.getMessage());
            }
        }
        return delegateProxy;
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
    protected Identifier getIdentifier() {
        if (identifier == null) {
            String pathComponent = getIdentifierPathComponent();
            if (pathComponent != null) {
                identifier = Identifier.fromURIPathComponent(pathComponent);
            }
        }
        return identifier;
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
    protected String getIdentifierPathComponent() {
        List<String> args = getPathArguments();
        return (!args.isEmpty()) ? args.get(0) : null;
    }

    abstract protected Logger getLogger();

    /**
     * Returns the decoded identifier path component of the URI, which may
     * include page number or other information. (This may not be the path
     * component that the client supplies or sees; for that, use {@link
     * #getPublicIdentifier()}.)
     *
     * @return Instance corresponding to the first {@link #getPathArguments()
     *         path argument}, or {@code null} if no path arguments are
     *         available.
     * @see #getIdentifier()
     * @see #getPublicIdentifier()
     */
    protected MetaIdentifier getMetaIdentifier() {
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
     * Returns the segments of the URI path that are considered arguments.
     * (These may correspond to regex match groups in {@link Route}.)
     *
     * @return Path arguments, or an empty list if there are none.
     */
    protected final List<String> getPathArguments() {
        return pathArguments;
    }

    /**
     * @return List of client-preferred media types as expressed in the
     *         {@code Accept} request header.
     * @see    <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">
     *         RFC 2616</a>
     */
    protected final List<String> getPreferredMediaTypes() {
        class Preference implements Comparable<Preference> {
            private String mediaType;
            private float qValue;

            @Override
            public int compareTo(Preference o) {
                if (o.qValue < qValue) {
                    return -1;
                } else if (o.qValue > o.qValue) {
                    return 1;
                }
                return 0;
            }
        }

        final List<Preference> preferences = new ArrayList<>();
        final String acceptHeader = request.getHeaders().getFirstValue("Accept");
        if (acceptHeader != null) {
            String[] clauses = acceptHeader.split(",");
            for (String clause : clauses) {
                String[] parts        = clause.split(";");
                Preference preference = new Preference();
                preference.mediaType  = parts[0].trim();
                if ("*/*".equals(preference.mediaType)) {
                    continue;
                }
                if (parts.length > 1) {
                    String q = parts[1].trim();
                    if (q.startsWith("q=")) {
                        q = q.substring(2);
                        preference.qValue = Float.parseFloat(q);
                    }
                } else {
                    preference.qValue = 1;
                }
                preferences.add(preference);
            }
        }
        return preferences.stream()
                .sorted()
                .map(p -> p.mediaType)
                .collect(Collectors.toUnmodifiableList());
    }

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
    protected String getPublicIdentifier() {
        return request.getHeaders().getFirstValue(
                PUBLIC_IDENTIFIER_HEADER,
                getIdentifierPathComponent());
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
    protected Reference getPublicReference() {
        final Reference ref        = getPublicRootReference();
        final Reference requestRef = new Reference(getRequest().getReference());
        final Reference appRootRef = new Reference(requestRef);
        appRootRef.setPath(getRequest().getContextPath());
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
     * Variant of {@link #getPublicReference()} that replaces the identifier
     * path component's meta-identifier if an identifier path component is
     * available.
     *
     * @param newMetaIdentifier Meta-identifier.
     */
    protected Reference getPublicReference(MetaIdentifier newMetaIdentifier) {
        final Reference publicRef         = new Reference(getPublicReference());
        final List<String> pathComponents = publicRef.getPathComponents();
        final int identifierIndex         = pathComponents.indexOf(
                getIdentifierPathComponent());

        final String newMetaIdentifierString =
                newMetaIdentifier.toURIPathComponent(getDelegateProxy());
        publicRef.setPathComponent(identifierIndex, newMetaIdentifierString);
        return publicRef;
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
    protected Reference getPublicRootReference() {
        Reference ref = new Reference(getRequest().getReference());
        ref.getQuery().clear();
        ref.setPath(getRequest().getContextPath());

        // If base_uri is set in the configuration, build a URI based on that.
        final String baseUri = Configuration.getInstance()
                .getString(Key.BASE_URI, "");
        if (!baseUri.isEmpty()) {
            final Reference baseRef = new Reference(baseUri);
            ref.setScheme(baseRef.getScheme());
            ref.setHost(baseRef.getHost());
            ref.setPort(baseRef.getPort());
            ref.setPath(StringUtils.stripEnd(baseRef.getPath(), "/"));
            getLogger().debug("Base URI from assembled from {} key: {}",
                    Key.BASE_URI, ref);
        } else {
            // Try to use X-Forwarded-* headers.
            ref.applyProxyHeaders(getRequest().getHeaders());
            getLogger().debug("Base URI assembled from X-Forwarded headers: {}",
                    ref);
        }
        return ref;
    }

    /**
     * <p>Returns a sanitized value for a {@code Content-Disposition} header
     * based on the value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     * query argument.</p>
     *
     * <p>If the disposition is {@code attachment} and the filename is not
     * set, it will be set to a reasonable value based on the given identifier
     * and output format.</p>
     *
     * @return Value for a {@code Content-Disposition} header, which may be
     *         {@code null}.
     */
    protected String getRepresentationDisposition(String identifierStr,
                                                  Format outputFormat) {
        var queryArg = getRequest().getReference().getQuery()
                .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        return getSafeContentDisposition(queryArg, identifierStr, outputFormat);
    }

    /**
     * @return Request being handled.
     */
    protected final Request getRequest() {
        return request;
    }

    /**
     * @return Instance with basic info already set.
     */
    protected final RequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * @return Response to be sent.
     */
    protected final HttpServletResponse getResponse() {
        return response;
    }

    /**
     * <p>This implementation returns a one-element array containing {@link
     * Method#OPTIONS}. It can be overridden to declare or not declare support
     * for:</p>
     *
     * <ul>
     *     <li>{@link #doGET() GET} (note that {@link #doHEAD() HEAD} is
     *     implicitly supported when this is supported)</li>
     *     <li>{@link #doPOST() POST}</li>
     *     <li>{@link #doPUT() PUT}</li>
     *     <li>{@link #doDELETE() DELETE}</li>
     * </ul>
     *
     * <p>Overrides should include {@link Method#OPTIONS}.</p>
     */
    public Method[] getSupportedMethods() {
        return new Method[] { Method.OPTIONS };
    }

    /**
     * @param limitToTypes Media types to limit the result to, in order of most
     *                     to least preferred by the application.
     * @return             Best media type conforming to client preferences as
     *                     expressed in the {@code Accept} header; or {@code
     *                     null} if negotiation failed.
     */
    protected final String negotiateContentType(List<String> limitToTypes) {
        return getPreferredMediaTypes().stream()
                .filter(limitToTypes::contains)
                .findFirst()
                .orElse(null);
    }

    final void setPathArguments(List<String> pathArguments) {
        this.pathArguments = pathArguments;
    }

    /**
     * @param request Request being handled.
     */
    public final void setRequest(Request request) {
        this.request = request;
    }

    /**
     * @param response Response that will be sent.
     */
    public final void setResponse(HttpServletResponse response) {
        this.response = response;
    }

}
