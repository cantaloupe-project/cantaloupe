package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.auth.AuthInfo;
import edu.illinois.library.cantaloupe.auth.Authorizer;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.restlet.Request;
import org.restlet.data.CacheDirective;
import org.restlet.data.Dimension;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Options;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * N.B. If subclasses need to send custom response headers, they should add
 * them to the {@link Series} returned by {@link #getBufferedResponseHeaders}.
 */
public abstract class AbstractResource extends ServerResource {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(AbstractResource.class);

    /**
     * Replaces {@link #PUBLIC_IDENTIFIER_HEADER_DEPRECATED}.
     */
    public static final String PUBLIC_IDENTIFIER_HEADER = "X-Forwarded-ID";

    /**
     * @deprecated Since version 4.0. Still respected, but superseded by
     *             {@link #PUBLIC_IDENTIFIER_HEADER}.
     */
    @Deprecated
    public static final String PUBLIC_IDENTIFIER_HEADER_DEPRECATED = "X-IIIF-ID";

    protected static final String RESPONSE_CONTENT_DISPOSITION_QUERY_ARG =
            "response-content-disposition";

    private Series<Header> bufferedResponseHeaders = new Series<>(Header.class);

    /**
     * @return Map of template variables common to most or all views, such as
     *         variables that appear in a common header.
     */
    public static Map<String, Object> getCommonTemplateVars(Request request) {
        Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        if (request != null) { // this will be null when testing
            Reference publicRef = getPublicReference(
                    request.getRootRef(),
                    request.getRootRef(),
                    request.getHeaders());
            vars.put("baseUri", publicRef.toString());
        }
        return vars;
    }

    /**
     * <p>Returns a root reference (URI) that can be used in public for display
     * or internal linking.</p>
     *
     * <p>The URI respects {@link Key#BASE_URI}, if set. Otherwise, it
     * respects the <code>X-Forwarded-*</code> request headers, if available.
     * Finally, the server hostname etc. otherwise.</p>
     *
     * @param requestRootRef Application root URI.
     * @param requestRef     Request URI.
     * @param requestHeaders Request headers.
     * @return Reference usable in public.
     */
    protected static Reference getPublicReference(Reference requestRootRef,
                                                  Reference requestRef,
                                                  Series<Header> requestHeaders) {
        String appRootRelativePath =
                requestRef.getRelativeRef(requestRootRef).getPath();
        if (".".equals(appRootRelativePath)) { // when the paths are the same
            appRootRelativePath = "";
        }
        Reference newRef;

        // If base_uri is set in the configuration, build a URI based on that.
        final String baseUri = Configuration.getInstance().
                getString(Key.BASE_URI);
        if (baseUri != null && baseUri.length() > 0) {
            final Reference baseRef = new Reference(baseUri);
            newRef = new Reference(requestRootRef);
            newRef.setScheme(baseRef.getScheme());
            newRef.setHostDomain(baseRef.getHostDomain());
            // if the "port" is a local socket, Reference will serialize it
            // as -1, so avoid that.
            if (baseRef.getHostPort() < 0) {
                newRef.setHostPort(null);
            } else {
                newRef.setHostPort(baseRef.getHostPort());
            }
            String pathStr = StringUtils.stripEnd(baseRef.getPath(), "/");
            if (!appRootRelativePath.isEmpty()) {
                pathStr = StringUtils.stripEnd(pathStr, "/") + "/" +
                        StringUtils.stripStart(appRootRelativePath, "/");
            }
            newRef.setPath(pathStr);

            LOGGER.debug("Base URI from assembled from {} key: {}",
                    Key.BASE_URI, newRef);

        } else {
            if (requestHeaders == null) {
                requestHeaders = new Series<>(Header.class);
            }
            // Try to use X-Forwarded-* headers.
            // N.B. Header values here may be comma-separated lists when
            // operating behind a chain of reverse proxies.
            final String hostHeader = requestHeaders.getFirstValue(
                    "X-Forwarded-Host", true, null);
            if (hostHeader != null) {
                final String protocolHeader = requestHeaders.getFirstValue(
                        "X-Forwarded-Proto", true, "HTTP");
                final String portHeader = requestHeaders.getFirstValue(
                        "X-Forwarded-Port", true, "80");
                final String pathHeader = requestHeaders.getFirstValue(
                        "X-Forwarded-Path", true, "");

                LOGGER.debug("X-Forwarded headers: Proto: {}; Host: {}; " +
                                "Port: {}; Path: {}",
                        protocolHeader, hostHeader, portHeader, pathHeader);

                final String hostStr = hostHeader.split(",")[0].trim();
                final String protocolStr =
                        protocolHeader.split(",")[0].trim().toUpperCase();
                final Protocol protocol = protocolStr.equals("HTTPS") ?
                        Protocol.HTTPS : Protocol.HTTP;
                final String portStr = portHeader.split(",")[0].trim();
                Integer port = Integer.parseInt(portStr);
                if ((port == 80 && protocol.equals(Protocol.HTTP)) ||
                        (port == 443 && protocol.equals(Protocol.HTTPS))) {
                    port = null;
                }

                String pathStr = pathHeader.split(",")[0].trim();
                if (!appRootRelativePath.isEmpty()) {
                    pathStr = StringUtils.stripEnd(pathStr, "/") + "/" +
                            StringUtils.stripStart(appRootRelativePath, "/");
                }

                newRef = new Reference(requestRootRef);
                newRef.setPath(pathStr);
                newRef.setHostDomain(hostStr);
                newRef.setPath(StringUtils.stripEnd(pathStr, "/"));
                newRef.setProtocol(protocol);
                newRef.setHostPort(port);

                LOGGER.debug("Base URI assembled from X-Forwarded headers: {}",
                                newRef);
            } else {
                newRef = requestRef;
                LOGGER.debug("Base URI assembled from request: {}", newRef);
            }
        }
        return newRef;
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();

        // We don't honor the Range header as most responses will be streamed.
        getResponse().getServerInfo().setAcceptingRanges(false);

        // "Dimensions" are added to the Vary header. Restlet doesn't supply
        // Origin by default, but it's needed.
        // See: https://github.com/medusa-project/cantaloupe/issues/107
        getResponse().getDimensions().addAll(Arrays.asList(
                Dimension.CHARACTER_SET,
                Dimension.ENCODING,
                Dimension.LANGUAGE,
                Dimension.ORIGIN));
        getResponse().getHeaders().add("X-Powered-By",
                Application.NAME + "/" + Application.getVersion());
        LOGGER.info("doInit(): handling {} {}", getMethod(), getReference());
    }

    /**
     * Enables HTTP OPTIONS requests. Restlet will set the <code>Allow</code>
     * header automatically.
     */
    @Options
    public Representation doOptions() {
        return new EmptyRepresentation();
    }

    /**
     * Uses an {@link Authorizer} to determine whether the request is
     * authorized.
     *
     * @param opList Operations requested on the image.
     * @param fullSize Full size of the requested image.
     * @return <code>null</code> if the request is authorized. Otherwise, a
     *         redirecting representation.
     * @throws IOException
     * @throws ScriptException
     * @throws AccessDeniedException If the delegate method returns
     *                               <code>false</code>.
     */
    protected final StringRepresentation checkAuthorization(
            final OperationList opList,
            final java.awt.Dimension fullSize)
            throws IOException, ScriptException, AccessDeniedException {
        final Authorizer authorizer = new Authorizer(getReference().toString(),
                getCanonicalClientIPAddress(),
                getRequest().getHeaders().getValuesMap(),
                getRequest().getCookies().getValuesMap());
        final AuthInfo info = authorizer.authorize(opList, fullSize);

        if (info.getRedirectURI() != null) {
            final URL location = info.getRedirectURI();
            final int code = info.getRedirectStatus();
            LOGGER.info("checkAuthorization(): redirecting to {} via HTTP {}",
                    location, code);
            final Status status = Status.valueOf(code);
            final StringRepresentation rep =
                    new StringRepresentation("Redirect: " + location);
            getResponse().setLocationRef(location.toString());
            getResponse().setStatus(status);
            return rep;
        } else if (!info.isAuthorized()) {
            throw new AccessDeniedException();
        }
        return null;
    }

    protected void commitCustomResponseHeaders() {
        getResponse().getHeaders().addAll(getBufferedResponseHeaders());
        getResponseCacheDirectives().addAll(getCacheDirectives());
    }

    /**
     * Some web servers have issues dealing with encoded slashes (%2F) in URIs.
     * This method enables the use of an alternate string to represent a slash
     * via {@link Key#SLASH_SUBSTITUTE}.
     *
     * @param uriPathComponent Path component (a part of the path before,
     *                         after, or between slashes)
     * @return Path component with slashes decoded.
     */
    private String decodeSlashes(final String uriPathComponent) {
        final String substitute = Configuration.getInstance().
                getString(Key.SLASH_SUBSTITUTE, "");
        if (!substitute.isEmpty()) {
            return StringUtils.replace(uriPathComponent, substitute, "/");
        }
        return uriPathComponent;
    }

    protected Series<Header> getBufferedResponseHeaders() {
        return bufferedResponseHeaders;
    }

    /**
     * @return List of cache directives according to the configuration, or an
     *         empty list if {@link #isBypassingCache()} returns
     *         <code>false</code>.
     */
    private List<CacheDirective> getCacheDirectives() {
        final List<CacheDirective> directives = new ArrayList<>();
        if (isBypassingCache()) {
            return directives;
        }
        try {
            final Configuration config = Configuration.getInstance();
            final boolean enabled =
                    config.getBoolean(Key.CLIENT_CACHE_ENABLED, false);
            if (enabled) {
                final String maxAge = config.getString(Key.CLIENT_CACHE_MAX_AGE);
                if (maxAge != null && maxAge.length() > 0) {
                    directives.add(CacheDirective.maxAge(Integer.parseInt(maxAge)));
                }
                String sMaxAge = config.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE);
                if (sMaxAge != null && sMaxAge.length() > 0) {
                    directives.add(CacheDirective.
                            sharedMaxAge(Integer.parseInt(sMaxAge)));
                }
                if (config.getBoolean(Key.CLIENT_CACHE_PUBLIC, true)) {
                    directives.add(CacheDirective.publicInfo());
                } else if (config.getBoolean(Key.CLIENT_CACHE_PRIVATE, false)) {
                    directives.add(CacheDirective.privateInfo());
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_CACHE, false)) {
                    directives.add(CacheDirective.noCache());
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_STORE, false)) {
                    directives.add(CacheDirective.noStore());
                }
                if (config.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE, false)) {
                    directives.add(CacheDirective.mustRevalidate());
                }
                if (config.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE, false)) {
                    directives.add(CacheDirective.proxyMustRevalidate());
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM, false)) {
                    directives.add(CacheDirective.noTransform());
                }
            }
        } catch (NoSuchElementException e) {
            LOGGER.warn("Cache-Control headers are invalid: {}",
                    e.getMessage());
        }
        return directives;
    }

    /**
     * @return Best guess at the user agent's IP address, respecting the
     *         <code>X-Forwarded-For</code> request header, if present.
     */
    protected String getCanonicalClientIPAddress() {
        String addr;
        // The value is supposed to be in the format: "client, proxy1, proxy2"
        final String forwardedFor =
                getRequest().getHeaders().getFirstValue("X-Forwarded-For", true);
        if (forwardedFor != null) {
            addr = forwardedFor.split(",")[0].trim();
        } else {
            // Fall back to the client IP address.
            addr = getRequest().getClientInfo().getAddress();
        }
        return addr;
    }

    /**
     * @return Decoded identifier component of the URI. N.B.: This may not be
     *         the identifier the user supplies or sees; for that, use
     *         {@link #getPublicIdentifier()}.
     * @see #getPublicIdentifier()
     */
    protected Identifier getIdentifier() {
        final Map<String,Object> attrs = getRequest().getAttributes();
        // Get the raw identifier from the URI.
        final String urlIdentifier = (String) attrs.get("identifier");
        // Decode entities.
        final String decodedIdentifier = Reference.decode(urlIdentifier);
        // Decode slash substitutes.
        final String identifier = decodeSlashes(decodedIdentifier);

        LOGGER.debug("Identifier requested: {} -> decoded: {} -> " +
                        "slashes substituted: {}",
                urlIdentifier, decodedIdentifier, identifier);

        return new Identifier(identifier);
    }

    /**
     * <p>Returns the image info for the source image corresponding to the
     * given identifier as efficiently as possible.</p>
     *
     * @param identifier
     * @param proc       Processor from which to read the info, if it can't be
     *                   retrieved from a cache.
     * @return           Info for the image with the given identifier.
     */
    protected final Info getOrReadInfo(final Identifier identifier,
                                       final Processor proc) throws IOException {
        Info info;
        if (!isBypassingCache()) {
            info = new CacheFacade().getOrReadInfo(identifier, proc);
        } else {
            LOGGER.debug("getOrReadInfo(): bypassing the cache, as requested");
            info = proc.readImageInfo();
        }
        return info;
    }

    /**
     * @return Value of either the {@link #PUBLIC_IDENTIFIER_HEADER} or
     *         {@link #PUBLIC_IDENTIFIER_HEADER_DEPRECATED} headers, if
     *         available, or else the {@literal identifier} URI path
     *         component.
     */
    protected String getPublicIdentifier() {
        final Map<String,Object> attrs = getRequest().getAttributes();
        final String urlID = (String) attrs.get("identifier");
        final String decodedID = Reference.decode(urlID);
        final String reSlashedID = decodeSlashes(decodedID);

        // Try to use the new header, if supplied.
        String header = PUBLIC_IDENTIFIER_HEADER;
        String headerID = getRequest().getHeaders().getFirstValue(header, true);
        if (headerID == null || headerID.isEmpty()) {
            // Fall back to the deprecated one.
            header = PUBLIC_IDENTIFIER_HEADER_DEPRECATED;
            headerID = getRequest().getHeaders().getFirstValue(header, true);
        }

        LOGGER.debug("Public identifier requested: {} -> decoded: {} -> " +
                        "slashes substituted: {} | {} header: {}",
                urlID, decodedID, reSlashedID, header, headerID);

        return (headerID != null && !headerID.isEmpty()) ?
                headerID : reSlashedID;
    }

    /**
     * @see #getPublicRootReference()
     */
    protected Reference getPublicReference() {
        final Request request = getRequest();
        return getPublicReference(request.getRootRef(),
                request.getResourceRef(), request.getHeaders());
    }

    /**
     * @see #getPublicReference()
     */
    protected Reference getPublicRootReference() {
        final Request request = getRequest();
        return getPublicReference(request.getRootRef(),
                request.getRootRef(), request.getHeaders());
    }

    /**
     * <p>Returns a content disposition based on the following in order of
     * preference:</p>
     *
     * <ol>
     *     <li>The value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     *     query argument</li>
     *     <li>The setting of {@link Key#IIIF_CONTENT_DISPOSITION} in the
     *     application configuration</li>
     * </ol>
     *
     * <p>Falls back to an empty disposition.</p>
     *
     * <p>If the disposition is <code>attachment</code> and the filename is
     * not set, it will be set to a reasonable value based on the given
     * identifier and output format.</p>
     *
     * @param queryArg Value of the
     *                 {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG} query
     *                 argument.
     * @param identifier
     * @param outputFormat
     */
    protected Disposition getRepresentationDisposition(String queryArg,
                                                       Identifier identifier,
                                                       Format outputFormat) {
        final Disposition disposition = new Disposition();
        // If a query argument value is available, use that. Otherwise, consult
        // the configuration.
        if (queryArg != null) {
            if (queryArg.startsWith("inline")) {
                disposition.setType(Disposition.TYPE_INLINE);
            } else if (queryArg.startsWith("attachment")) {
                Pattern pattern = Pattern.compile(".*filename=\\\"(.*)\\\".*");
                Matcher m = pattern.matcher(queryArg);
                String filename;
                if (m.matches()) {
                    // Filter out filename-unsafe characters as well as ".."
                    filename = StringUtil.sanitize(m.group(1),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtil.FILENAME_REGEX));
                } else {
                    filename = getContentDispositionFilename(identifier,
                            outputFormat);
                }
                disposition.setType(Disposition.TYPE_ATTACHMENT);
                disposition.setFilename(filename);
            }
        } else {
            switch (Configuration.getInstance()
                    .getString(Key.IIIF_CONTENT_DISPOSITION, "none")) {
                case "inline":
                    disposition.setType(Disposition.TYPE_INLINE);
                    break;
                case "attachment":
                    disposition.setType(Disposition.TYPE_ATTACHMENT);
                    disposition.setFilename(getContentDispositionFilename(
                            identifier, outputFormat));
                    break;
            }
        }
        return disposition;
    }

    protected RequestContext getRequestContext() {
        final RequestContext context = new RequestContext();
        context.setRequestURI(getReference().toString());
        context.setRequestHeaders(getRequest().getHeaders().getValuesMap());
        context.setClientIP(getCanonicalClientIPAddress());
        context.setCookies(getRequest().getCookies().getValuesMap());
        return context;
    }

    private String getContentDispositionFilename(Identifier identifier,
                                                 Format outputFormat) {
        return identifier.toString().replaceAll(StringUtil.FILENAME_REGEX, "_") +
                "." + outputFormat.getPreferredExtension();
    }

    /**
     * @return Whether there is a <var>cache</var> query parameter set to
     *         <code>false</code> in the URI.
     */
    protected boolean isBypassingCache() {
        boolean bypassingCache = false;
        Parameter cacheParam = getReference().getQueryAsForm().getFirst("cache");
        if (cacheParam != null) {
            bypassingCache = "false".equals(cacheParam.getValue());
        }
        return bypassingCache;
    }

    /**
     * @param name Template pathname, with leading slash.
     * @return     Representation using the given template and the common
     *             template variables.
     */
    public Representation template(String name) {
        return template(name, new HashMap<>());
    }

    /**
     * @param name Template pathname, with leading slash.
     * @param vars Template variables.
     * @return     Representation using the given template and the given
     *             template variables.
     */
    public Representation template(String name, Map<String,Object> vars) {
        vars.putAll(getCommonTemplateVars(getRequest()));
        return new VelocityRepresentation(name, vars);
    }

    /**
     * <p>Checks that the requested area is greater than zero and less than or
     * equal to {@link Key#MAX_PIXELS}.</p>
     *
     * <p>This does not check that any requested crop lies entirely within the
     * bounds of the source image.</p>
     */
    protected final void validateRequestedArea(final OperationList opList,
                                               final Format sourceFormat,
                                               final Info info)
            throws EmptyPayloadException, PayloadTooLargeException {
        final java.awt.Dimension resultingSize =
                opList.getResultingSize(info.getSize());

        if (resultingSize.width < 1 || resultingSize.height < 1) {
            throw new EmptyPayloadException();
        }

        // Max allowed size is ignored when the processing is a no-op.
        if (opList.hasEffect(sourceFormat)) {
            final long maxAllowedSize =
                    Configuration.getInstance().getLong(Key.MAX_PIXELS, 0);
            if (maxAllowedSize > 0 &&
                    resultingSize.width * resultingSize.height > maxAllowedSize) {
                throw new PayloadTooLargeException();
            }
        }
    }

}
