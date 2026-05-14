package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.delegate.UnavailableException;
import edu.illinois.library.cantaloupe.http.ContentTypeNegotiator;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
        logRequestStart();
    }

    protected void logRequestStart() {
        getLogger().info("Handling {} {}",
                request.getServletRequest().getMethod(), request.getReference().getPath());
        getLogger().debug("Request headers: {}",
                request.getHeaders().stream()
                        .map(h -> h.getName() + ": " +
                                ("Authorization".equals(h.getName()) ? "******" : h.getValue()))
                        .collect(Collectors.joining("; ")));
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
     * @return Template variables common to most or all templates, such as
     *         variables that appear in a common header.
     */
    protected final Map<String, Object> getCommonTemplateVars() {
        final Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        vars.put("basePath", getRequest().
                                 getHeaders().
                                 getFirstValue("X-Forwarded-Path", "/"));
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
        ContentTypeNegotiator negotiator = new ContentTypeNegotiator(request.getHeaders());
        return negotiator.getPreferredMediaTypes();
    }

    /**
     * <p>Returns the identifier that the client sees. This will be the value
     * of the {@link #PUBLIC_IDENTIFIER_HEADER} header, if available, or else
     * the {@code identifier} URI path component.</p>
     *
     * <p>The result is not decoded, as the encoding may be influenced by
     * {@link edu.illinois.library.cantaloupe.config.Key#SLASH_SUBSTITUTE}, for example.</p>
     *
     * @see #getIdentifier()
     */
    protected String getPublicIdentifier() {
        return request.getHeaders().getFirstValue(
                PUBLIC_IDENTIFIER_HEADER,
                getIdentifierPathComponent());
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
        String queryArg = getRequest().getReference().getQuery()
                .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        if (queryArg != null) {
            getLogger().warn("Passing the {} query argument is deprecated and support for it will be " +
                             "removed in the next major release. Consider the `download' attribute on the anchor tag " +
                             "instead. See https://developer.mozilla.org/en-US/docs/Web/API/HTMLAnchorElement/download", RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        }

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
     * </ul>
     *
     * <p>Overrides should include {@link Method#OPTIONS}.</p>
     */
    public Method[] getSupportedMethods() {
        return new Method[] { Method.OPTIONS };
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
