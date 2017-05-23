package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
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
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class AbstractResource extends ServerResource {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractResource.class);

    public static final String AUTHORIZATION_DELEGATE_METHOD = "authorized?";
    private static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";

    private static final TemplateCache templateCache = new TemplateCache();

    /**
     * @return Map of template variables common to most or all views, such as
     *         variables that appear in a common header.
     */
    public static Map<String, Object> getCommonTemplateVars(Request request) {
        Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        if (request != null) { // this will be null when testing
            Reference publicRef = getPublicRootRef(request);
            vars.put("baseUri", publicRef.toString());
        }
        return vars;
    }

    /**
     * @param request
     * @return Root reference usable in public, respecting the
     *         <code>base_uri</code> option in the application configuration;
     *         or <code>null</code> if a <code>request</code> is
     *         <code>null</code>.
     */
    public static Reference getPublicRootRef(final Request request) {
        Reference rootRef = null;
        if (request != null) {
            rootRef = new Reference(request.getRootRef());

            final String baseUri = Configuration.getInstance().
                    getString(Key.BASE_URI);
            if (baseUri != null && baseUri.length() > 0) {
                final Reference baseRef = new Reference(baseUri);
                rootRef.setScheme(baseRef.getScheme());
                rootRef.setHostDomain(baseRef.getHostDomain());
                // if the "port" is a local socket, Reference will serialize it
                // as -1.
                if (baseRef.getHostPort() == -1) {
                    rootRef.setHostPort(null);
                } else {
                    rootRef.setHostPort(baseRef.getHostPort());
                }
                rootRef.setPath(StringUtils.stripEnd(baseRef.getPath(), "/"));
            } else {
                final Series<Header> headers = request.getHeaders();
                final String hostStr = headers.getFirstValue(
                        "X-Forwarded-Host", true, null);
                if (hostStr != null) {
                    final String protocolStr = headers.getFirstValue(
                            "X-Forwarded-Proto", true, "HTTP");
                    final String portStr = headers.getFirstValue(
                            "X-Forwarded-Port", true, "80");
                    final String pathStr = headers.getFirstValue(
                            "X-Forwarded-Path", true, "");
                    logger.debug("Assembling base URI from X-Forwarded headers. " +
                                    "Proto: {}; Host: {}; Port: {}; Path: {}",
                            protocolStr, hostStr, portStr, pathStr);

                    rootRef.setHostDomain(hostStr);
                    rootRef.setPath(StringUtils.stripEnd(pathStr, "/"));

                    final Protocol protocol =
                            protocolStr.toUpperCase().equals("HTTPS") ?
                                    Protocol.HTTPS : Protocol.HTTP;
                    rootRef.setProtocol(protocol);

                    Integer port = Integer.parseInt(portStr);
                    if ((port == 80 && protocol.equals(Protocol.HTTP)) ||
                            (port == 443 && protocol.equals(Protocol.HTTPS))) {
                        port = null;
                    }
                    rootRef.setHostPort(port);
                }
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
     * Adds an X-Sendfile (or equivalent) header to the response, if the
     * <code>X-Sendfile-Type</code> request header is set.
     *
     * @param relativePathname Relative pathname of the file.
     */
    protected void addXSendfileHeader(String relativePathname) {
        // Check the input.
        if (relativePathname == null || relativePathname.length() < 1) {
            logger.error("addXSendfileHeader(): relative pathname not " +
                    "provided (this may be a bug)");
            return;
        }

        // If there is an X-Sendfile-Type request header, set the X-Sendfile
        // (or equivalent) response header.
        final Header typeHeader =
                getRequest().getHeaders().getFirst("X-Sendfile-Type");
        if (typeHeader != null) {
            getResponse().getHeaders().add(typeHeader.getValue(),
                    relativePathname);
        } else {
            logger.debug("No X-Sendfile-Type request header. " +
                    "X-Sendfile will be disabled.");
        }
    }

    /**
     * <p>Invokes the {@link #AUTHORIZATION_DELEGATE_METHOD} delegate method to
     * determine whether the request is authorized.</p>
     *
     * <p>The delegate method may return a boolean or a hash. If it returns
     * <code>true</code>, the request is authorized. If it returns
     * <code>false</code>, an {@link AccessDeniedException} will be thrown.</p>
     *
     * <p>If it returns a hash, the hash must contain <code>location</code>,
     * <code>status_code</code>, and <code>status_line</code> keys.</p>
     *
     * <p>N.B. The reason there aren't separate delegate methods to perform
     * authorization and redirecting is because these will often require
     * similar or identical service requests on the part of the client. Having
     * one method handle both scenarios simplifies implementation and reduces
     * cost.</p>
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
        final Map<String,Integer> fullSizeArg = new HashMap<>();
        fullSizeArg.put("width", fullSize.width);
        fullSizeArg.put("height", fullSize.height);

        final Dimension resultingSize = opList.getResultingSize(fullSize);
        final Map<String,Integer> resultingSizeArg = new HashMap<>();
        resultingSizeArg.put("width", resultingSize.width);
        resultingSizeArg.put("height", resultingSize.height);

        final Map opListMap = opList.toMap(fullSize);

        try {
            final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
            Object result = engine.invoke(AUTHORIZATION_DELEGATE_METHOD,
                    opList.getIdentifier().toString(),         // identifier
                    fullSizeArg,                               // full_size
                    opListMap.get("operations"),               // operations
                    resultingSizeArg,                          // resulting_size
                    opListMap.get("output_format"),            // output_format
                    getReference().toString(),                 // request_uri
                    getRequest().getHeaders().getValuesMap(),  // request_headers
                    getCanonicalClientIpAddress(),             // client_ip
                    getRequest().getCookies().getValuesMap()); // cookies
            if (result instanceof Boolean) {
                if (!((boolean) result)) {
                    throw new AccessDeniedException();
                }
            } else {
                final Map redirectInfo = (Map) result;
                final String location = redirectInfo.get("location").toString();
                // Prevent circular redirects
                if (!getReference().toString().equals(location)) {
                    final int statusCode =
                            Integer.parseInt(redirectInfo.get("status_code").toString());
                    if (statusCode < 300 || statusCode > 399) {
                        throw new IllegalArgumentException(
                                "Status code must be in the range of 300-399.");
                    }

                    logger.info("checkAuthorization(): redirecting to {} via HTTP {}",
                            location, statusCode);
                    final Status status = Status.valueOf(statusCode);
                    final StringRepresentation rep =
                            new StringRepresentation("Redirect: " + status.getUri());
                    getResponse().setLocationRef(location);
                    getResponse().setStatus(status);
                    return rep;
                }
            }
        } catch (DelegateScriptDisabledException e) {
            logger.debug("checkAuthorization(): delegate script is disabled; allowing.");
            return null;
        }
        return null;
    }

    /**
     * Checks the given operation list against the given image size.
     *
     * @param opList
     * @param fullSize
     * @throws EmptyPayloadException
     */
    protected final void checkRequest(final OperationList opList,
                                      final Dimension fullSize)
            throws EmptyPayloadException {
        final Dimension resultingSize = opList.getResultingSize(fullSize);
        if (resultingSize.width < 1 || resultingSize.height < 1) {
            throw new EmptyPayloadException();
        }
    }

    /**
     * Some web servers have issues dealing with encoded slashes (%2F) in URLs.
     * This method enables the use of an alternate string to represent a slash
     * via {@link Key#SLASH_SUBSTITUTE}.
     *
     * @param uriPathComponent Path component (a part of the path before,
     *                         after, or between slashes)
     * @return Path component with slashes decoded
     */
    protected final String decodeSlashes(final String uriPathComponent) {
        final String substitute = Configuration.getInstance().
                getString(Key.SLASH_SUBSTITUTE, "");
        if (substitute.length() > 0) {
            return StringUtils.replace(uriPathComponent, substitute, "/");
        }
        return uriPathComponent;
    }

    protected final Identifier decodeSlashes(final Identifier identifier) {
        return new Identifier(decodeSlashes(identifier.toString()));
    }

    /**
     * @return List of cache directives according to the configuration, or an
     *         empty list if {@link #isBypassingCache()} returns
     *         <code>false</code>.
     */
    protected final List<CacheDirective> getCacheDirectives() {
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
     * @return The client IP address, respecting the X-Forwarded-For header,
     *         if present.
     */
    protected String getCanonicalClientIpAddress() {
        final List<String> forwardedIps = getRequest().getClientInfo().
                getForwardedAddresses();
        if (forwardedIps.size() > 0) {
            return forwardedIps.get(forwardedIps.size() - 1);
        }
        return getRequest().getClientInfo().getAddress();
    }

    protected ImageRepresentation getRepresentation(OperationList ops,
                                                    Format format,
                                                    Disposition disposition,
                                                    Processor proc)
            throws IOException, ProcessorException, CacheException {
        // Max allowed size is ignored when the processing is a no-op.
        final long maxAllowedSize = (ops.hasEffect(format)) ?
                Configuration.getInstance().getLong(Key.MAX_PIXELS, 0) : 0;

        final Info imageInfo = getOrReadInfo(ops.getIdentifier(), proc);
        final Dimension effectiveSize = ops.getResultingSize(imageInfo.getSize());
        if (maxAllowedSize > 0 &&
                effectiveSize.width * effectiveSize.height > maxAllowedSize) {
            throw new PayloadTooLargeException();
        }

        return new ImageRepresentation(imageInfo, proc, ops, disposition,
                isBypassingCache());
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
    private boolean isBypassingCache() {
        boolean bypassingCache = false;
        Parameter cacheParam = getReference().getQueryAsForm().getFirst("cache");
        if (cacheParam != null) {
            bypassingCache = "false".equals(cacheParam.getValue());
        }
        return bypassingCache;
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

}
