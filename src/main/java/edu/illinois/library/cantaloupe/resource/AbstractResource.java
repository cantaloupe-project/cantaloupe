package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.auth.AuthInfo;
import edu.illinois.library.cantaloupe.auth.Authorizer;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.restlet.Request;
import org.restlet.data.CacheDirective;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * N.B. Subclasses should add custom response headers to the Series returned by
 * {@link #getBufferedResponseHeaders()}.
 */
public abstract class AbstractResource extends ServerResource {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractResource.class);

    private static final String ACCEL_REDIRECT_URI_PREFIX =
            "/cantaloupe_sendfile";
    private static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";

    private static final TemplateCache templateCache = new TemplateCache();

    private Series<Header> bufferedResponseHeaders = new Series<>(Header.class);

    /**
     * @return Map of template variables common to most or all views, such as
     *         variables that appear in a common header.
     */
    public static Map<String, Object> getCommonTemplateVars(Request request) {
        Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        if (request != null) { // this will be null when testing
            Reference publicRef = getPublicRootRef(request.getRootRef(),
                    request.getHeaders());
            vars.put("baseUri", publicRef.toString());
        }
        return vars;
    }

    /**
     * <p>Returns a root reference (URI) that can be used in public for the
     * purposes of display or internal linking.</p>
     *
     * <p>The URI respects {@link Key#BASE_URI}, if set. Otherwise, it
     * respects the <code>X-Forwarded-*</code> request headers, if available.
     * Finally, the server hostname etc. otherwise.</p>
     *
     * @param requestRootRef
     * @param requestHeaders
     * @return Root reference usable in public.
     */
    protected static Reference getPublicRootRef(Reference requestRootRef,
                                                Series<Header> requestHeaders) {
        if (requestHeaders == null) {
            requestHeaders = new Series<>(Header.class);
        }
        final Reference rootRef = new Reference(requestRootRef);

        // If base_uri is set in the configuration, build a URI based on that.
        final String baseUri = Configuration.getInstance().
                getString(Key.BASE_URI);
        if (baseUri != null && baseUri.length() > 0) {
            final Reference baseRef = new Reference(baseUri);
            rootRef.setScheme(baseRef.getScheme());
            rootRef.setHostDomain(baseRef.getHostDomain());
            // if the "port" is a local socket, Reference will serialize it
            // as -1, so avoid that.
            if (baseRef.getHostPort() < 0) {
                rootRef.setHostPort(null);
            } else {
                rootRef.setHostPort(baseRef.getHostPort());
            }
            rootRef.setPath(StringUtils.stripEnd(baseRef.getPath(), "/"));

            logger.debug("Base URI from assembled from configuration ({}): {}",
                    Key.BASE_URI, rootRef);
        } else {
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

                logger.debug("X-Forwarded headers: Proto: {}; Host: {}; " +
                                "Port: {}; Path: {}",
                        protocolHeader, hostHeader, portHeader, pathHeader);

                final String hostStr = hostHeader.split(",")[0].trim();
                final String protocolStr =
                        protocolHeader.split(",")[0].trim().toUpperCase();
                final Protocol protocol = protocolStr.equals("HTTPS") ?
                        Protocol.HTTPS : Protocol.HTTP;
                final String pathStr = pathHeader.split(",")[0].trim();
                final String portStr = portHeader.split(",")[0].trim();
                Integer port = Integer.parseInt(portStr);
                if ((port == 80 && protocol.equals(Protocol.HTTP)) ||
                        (port == 443 && protocol.equals(Protocol.HTTPS))) {
                    port = null;
                }

                rootRef.setHostDomain(hostStr);
                rootRef.setPath(StringUtils.stripEnd(pathStr, "/"));
                rootRef.setProtocol(protocol);
                rootRef.setHostPort(port);

                logger.debug("Base URI assembled from X-Forwarded headers: {}",
                                rootRef);
            } else {
                logger.debug("Base URI assembled from request: {}", rootRef);
            }
        }
        return rootRef;
    }

    /**
     * @param identifier
     * @param outputFormat
     * @return Content disposition based on the setting of
     *         {@link Key#IIIF_CONTENT_DISPOSITION} in the application
     *         configuration. If it is set to <code>attachment</code>, the
     *         disposition will have a filename set to a reasonable value
     *         based on the given identifier and output format.
     */
    public static Disposition getRepresentationDisposition(
            Identifier identifier, Format outputFormat) {
        Disposition disposition = new Disposition();
        switch (Configuration.getInstance().
                getString(Key.IIIF_CONTENT_DISPOSITION, "none")) {
            case "inline":
                disposition.setType(Disposition.TYPE_INLINE);
                break;
            case "attachment":
                disposition.setType(Disposition.TYPE_ATTACHMENT);
                disposition.setFilename(
                        identifier.toString().replaceAll(
                                FILENAME_CHARACTERS, "_") +
                                "." + outputFormat.getPreferredExtension());
                break;
        }
        return disposition;
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        getResponse().getHeaders().add("X-Powered-By",
                "Cantaloupe/" + Application.getVersion());
        logger.info("doInit(): handling {} {}", getMethod(), getReference());
    }

    /**
     * <p>Adds an <code>X-Sendfile</code> (or equivalent) header to the
     * response if the <code>X-Sendfile-Type</code> request header is set.</p>
     *
     * <p>N.B. You probably want to return an EmptyRepresentation soon after
     * calling this. (Don't forget to set its media type.)</p>
     *
     * @param path Path of the file relative to the proxy server.
     * @param root Root path required for <code>X-Accel-Redirect</code>.
     */
    protected void addXSendfileHeader(Path path, Path root) {
        // Check the input.
        if (path == null) {
            logger.error("addXSendfileHeader(): pathname not provided " +
                    "(this may be a bug)");
            return;
        }

        // If there is an X-Sendfile-Type request header, set the X-Sendfile
        // (or equivalent) response header.
        final Header typeHeader =
                getRequest().getHeaders().getFirst("X-Sendfile-Type", true);
        if (typeHeader != null) {
            getResponse().getHeaders().add(typeHeader.getValue(),
                    path.toString());
            final String headerName = typeHeader.getValue();
            // If we are sending an X-Sendfile header, the value will be the
            // absolute path of the file. For X-Accel-Redirect, it will be the
            // pathname of the file relative to the given root, prefixed by
            // ACCEL_REDIRECT_URI_PREFIX.
            String headerValue;
            switch (headerName.toLowerCase()) {
                case "x-accel-redirect":
                    headerValue = ACCEL_REDIRECT_URI_PREFIX + "/" +
                            StringUtils.stripStart(path.toString(),
                                    root.toString());
                    break;
                default:
                    headerValue = path.toString();
                    break;
            }
            logger.debug("Setting {}: {}", headerName, headerValue);
            getResponse().getHeaders().add(headerName, headerValue);
        } else {
            logger.debug("No X-Sendfile-Type request header. " +
                    "X-Sendfile header won't be sent.");
        }
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
            final OperationList opList, final Dimension fullSize)
            throws IOException, ScriptException, AccessDeniedException {
        final Authorizer authorizer = new Authorizer(getReference().toString(),
                getCanonicalClientIpAddress(),
                getRequest().getHeaders().getValuesMap(),
                getRequest().getCookies().getValuesMap());
        final AuthInfo info = authorizer.authorize(opList, fullSize);

        if (info.getRedirectURI() != null) {
            final URL location = info.getRedirectURI();
            final int code = info.getRedirectStatus();
            logger.info("checkAuthorization(): redirecting to {} via HTTP {}",
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
     * Some web servers have issues dealing with encoded slashes (%2F) in URLs.
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
        if (substitute.length() > 0) {
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
            logger.warn("Cache-Control headers are invalid: {}",
                    e.getMessage());
        }
        return directives;
    }

    /**
     * @return Best guess at the user agent's IP address, respecting the
     *         <code>X-Forwarded-For</code> request header, if present.
     */
    protected String getCanonicalClientIpAddress() {
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
     * @return Identifier component of the URI, decoded and ready for use.
     */
    protected Identifier getIdentifier() {
        final Map<String,Object> attrs = getRequest().getAttributes();
        // Get the raw identifier from the URI.
        final String urlIdentifier = (String) attrs.get("identifier");
        // Decode entities.
        final String decodedIdentifier = Reference.decode(urlIdentifier);
        // Decode slash substitutes.
        final String identifier = decodeSlashes(decodedIdentifier);

        logger.debug("getIdentifier(): requested: {} / decoded: {} / " +
                        "slashes substituted: {}",
                urlIdentifier, decodedIdentifier, identifier);

        return new Identifier(identifier);
    }

    /**
     * Gets the image info corresponding to the given identifier, first by
     * checking the cache and then, if necessary, by reading it from the image
     * and caching the result.
     *
     * @param identifier
     * @param proc
     * @return Info for the image with the given identifier, retrieved
     *         from the given processor.
     * @throws ProcessorException
     * @throws CacheException
     */
    protected final Info getOrReadInfo(final Identifier identifier,
                                       final Processor proc)
            throws ProcessorException, CacheException {
        Info info = null;
        if (!isBypassingCache()) {
            DerivativeCache cache = CacheFactory.getDerivativeCache();
            if (cache != null) {
                final Stopwatch watch = new Stopwatch();
                info = cache.getImageInfo(identifier);
                if (info != null) {
                    logger.debug("getOrReadInfo(): retrieved dimensions of {} from cache in {} msec",
                            identifier, watch.timeElapsed());
                } else {
                    info = readInfo(identifier, proc);
                    cache.put(identifier, info);
                }
            }
        } else {
            logger.debug("getOrReadInfo(): bypassing the cache, as requested");
        }
        if (info == null) {
            info = readInfo(identifier, proc);
        }
        return info;
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

    protected boolean isXSendfileSupported() {
        final Header typeHeader =
                getRequest().getHeaders().getFirst("X-Sendfile-Type", true);
        if (typeHeader != null) {
            return "x-sendfile".equals(typeHeader.getValue().toLowerCase()) ||
                    "x-accel-redirect".equals(typeHeader.getValue().toLowerCase());
        }
        return false;
    }

    /**
     * Reads the information of the source image.
     *
     * @param identifier
     * @param proc
     * @return
     * @throws ProcessorException
     */
    private Info readInfo(final Identifier identifier,
                          final Processor proc) throws ProcessorException {
        final Stopwatch watch = new Stopwatch();
        final Info info = proc.readImageInfo();
        logger.debug("readInfo(): read from {} in {} msec", identifier,
                watch.timeElapsed());
        return info;
    }

    /**
     * @param name Template pathname, with leading slash.
     * @return Representation using the given template and the common template
     *         variables.
     */
    public Representation template(String name) {
        return template(name, getCommonTemplateVars(getRequest()));
    }

    /**
     * @param name Template pathname, with leading slash.
     * @return Representation using the given template and the given template
     *         variables.
     */
    public Representation template(String name, Map<String,Object> vars) {
        final String template = templateCache.get(name);
        if (template != null) {
            try {
                return new TemplateRepresentation(
                        new StringRepresentation(template), vars,
                        MediaType.TEXT_HTML);
            } catch (IOException e) {
                logger.error(e.getMessage());
                return new StringRepresentation(e.getMessage());
            }
        }
        return new EmptyRepresentation();
    }

    /**
     * <p>Checks that the requested area is greater than zero and less than or
     * equal to {@link Key#MAX_PIXELS}.</p>
     *
     * <p>This does not check that any requested crop lies entirely within the
     * bounds of the source image.</p>
     *
     * @param opList
     * @param sourceFormat
     * @param info
     */
    protected final void validateRequestedArea(final OperationList opList,
                                               final Format sourceFormat,
                                               final Info info)
            throws EmptyPayloadException, PayloadTooLargeException {
        final Dimension resultingSize = opList.getResultingSize(info.getSize());

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
