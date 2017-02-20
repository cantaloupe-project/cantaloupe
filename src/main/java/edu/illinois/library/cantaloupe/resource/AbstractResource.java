package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionService;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.overlay.OverlayService;
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
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
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

import static edu.illinois.library.cantaloupe.processor.Processor.DOWNSCALE_FILTER_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.Processor.UPSCALE_FILTER_CONFIG_KEY;

public abstract class AbstractResource extends ServerResource {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractResource.class);

    public static final String AUTHORIZATION_DELEGATE_METHOD = "authorized?";
    public static final String BASE_URI_CONFIG_KEY =
            "base_uri";
    public static final String CLIENT_CACHE_ENABLED_CONFIG_KEY =
            "cache.client.enabled";
    public static final String CLIENT_CACHE_MAX_AGE_CONFIG_KEY =
            "cache.client.max_age";
    public static final String CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY =
            "cache.client.must_revalidate";
    public static final String CLIENT_CACHE_NO_CACHE_CONFIG_KEY =
            "cache.client.no_cache";
    public static final String CLIENT_CACHE_NO_STORE_CONFIG_KEY =
            "cache.client.no_store";
    public static final String CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY =
            "cache.client.no_transform";
    public static final String CLIENT_CACHE_PRIVATE_CONFIG_KEY =
            "cache.client.private";
    public static final String CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY =
            "cache.client.proxy_revalidate";
    public static final String CLIENT_CACHE_PUBLIC_CONFIG_KEY =
            "cache.client.public";
    public static final String CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY =
            "cache.client.shared_max_age";
    public static final String CONTENT_DISPOSITION_CONFIG_KEY =
            "endpoint.iiif.content_disposition";
    public static final String MAX_PIXELS_CONFIG_KEY =
            "max_pixels";
    public static final String SLASH_SUBSTITUTE_CONFIG_KEY =
            "slash_substitute";

    private static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";

    /**
     * @return Map of template variables common to most or all views, such as
     *         variables that appear in a common header.
     */
    public static Map<String, Object> getCommonTemplateVars(Request request) {
        Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        vars.put("baseUri", getPublicRootRef(request).toString());
        return vars;
    }

    /**
     * @param request
     * @return Root reference usable in public, respecting the
     *         <code>base_uri</code> option in the application configuration.
     */
    public static Reference getPublicRootRef(final Request request) {
        Reference rootRef = new Reference(request.getRootRef());

        final String baseUri = ConfigurationFactory.getInstance().
                getString(BASE_URI_CONFIG_KEY);
        if (baseUri != null && baseUri.length() > 0) {
            final Reference baseRef = new Reference(baseUri);
            rootRef.setScheme(baseRef.getScheme());
            rootRef.setHostDomain(baseRef.getHostDomain());
            // if the "port" is a local socket, Reference will serialize it as
            // -1.
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

                final Protocol protocol = protocolStr.toUpperCase().equals("HTTPS") ?
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
        return rootRef;
    }

    /**
     * @param identifier
     * @param outputFormat
     * @return Content disposition based on the setting of
     *         {@link #CONTENT_DISPOSITION_CONFIG_KEY} in the application
     *         configuration. If it is set to <code>attachment</code>, the
     *         disposition will have a filename set to a reasonable value based
     *         on the given identifier and output format.
     */
    public static Disposition getRepresentationDisposition(
            Identifier identifier, Format outputFormat) {
        Disposition disposition = new Disposition();
        switch (Configuration.getInstance().
                getString(CONTENT_DISPOSITION_CONFIG_KEY, "none")) {
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
     * <p>Most image-processing operations (crop, scale, etc.) are specified in
     * a client request to an endpoint. This method adds any operations or
     * options that endpoints have nothing to do with.</p>
     *
     * <p>It will also call {@link OperationList#freeze()} on the operation
     * list before returning.</p>
     *
     * @param opList Operation list to add the operations and/or options to.
     * @param fullSize Full size of the source image.
     */
    protected void addNonEndpointOperations(final OperationList opList,
                                            final Dimension fullSize) {
        final Configuration config = Configuration.getInstance();

        // Redactions
        try {
            if (RedactionService.isEnabled()) {
                List<Redaction> redactions = RedactionService.redactionsFor(
                        opList.getIdentifier(),
                        getRequest().getHeaders().getValuesMap(),
                        getCanonicalClientIpAddress(),
                        getRequest().getCookies().getValuesMap());
                for (Redaction redaction : redactions) {
                    opList.add(redaction);
                }
            } else {
                logger.debug("addNonEndpointOperations(): redactions are " +
                        "disabled; skipping.");
            }
        } catch (DelegateScriptDisabledException e) {
            logger.debug("addNonEndpointOperations(): delegate script is " +
                    "disabled; skipping redactions.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        // Scale filter
        final Scale scale = (Scale) opList.getFirst(Scale.class);
        if (scale != null) {
            final Float scalePct = scale.getResultingScale(fullSize);
            if (scalePct != null) {
                final String filterKey = (scalePct > 1) ?
                        UPSCALE_FILTER_CONFIG_KEY : DOWNSCALE_FILTER_CONFIG_KEY;
                try {
                    final String filterStr = config.getString(filterKey);
                    final Scale.Filter filter =
                            Scale.Filter.valueOf(filterStr.toUpperCase());
                    scale.setFilter(filter);
                } catch (Exception e) {
                    logger.warn("addNonEndpointOperations(): invalid value for {}",
                            filterKey);
                }
            }
        }

        // Rotation background color
        if (!opList.getOutputFormat().supportsTransparency()) {
            final String bgColor =
                    config.getString(Processor.BACKGROUND_COLOR_CONFIG_KEY, "black");
            final Rotate rotate = (Rotate) opList.getFirst(Rotate.class);
            if (rotate != null) {
                rotate.setFillColor(Color.fromString(bgColor));
            }
        }

        // Sharpening
        float sharpen = config.getFloat(Processor.SHARPEN_CONFIG_KEY, 0f);
        if (sharpen > 0.001f) {
            opList.add(new Sharpen(sharpen));
        }

        // Overlay
        try {
            final OverlayService service = new OverlayService();
            if (service.isEnabled()) {
                final Overlay overlay = service.newOverlay(
                        opList, fullSize, getReference().toUrl(),
                        getRequest().getHeaders().getValuesMap(),
                        getCanonicalClientIpAddress(),
                        getRequest().getCookies().getValuesMap());
                opList.add(overlay);
            } else {
                logger.debug("addNonEndpointOperations(): overlays are " +
                        "disabled; skipping.");
            }
        } catch (DelegateScriptDisabledException e) {
            logger.debug("addNonEndpointOperations(): delegate script is " +
                    "disabled; skipping overlay.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        // Metadata copies
        if (config.getBoolean(Processor.PRESERVE_METADATA_CONFIG_KEY, false)) {
            opList.add(new MetadataCopy());
        }

        switch (opList.getOutputFormat()) {
            case JPG:
                // Interlacing
                final boolean progressive =
                        config.getBoolean(Processor.JPG_PROGRESSIVE_CONFIG_KEY, false);
                opList.setOutputInterlacing(progressive);
                // Quality
                final int quality =
                        config.getInt(Processor.JPG_QUALITY_CONFIG_KEY, 80);
                opList.setOutputQuality(quality);
                break;
            case TIF:
                // Compression
                final String compressionStr =
                        config.getString(Processor.TIF_COMPRESSION_CONFIG_KEY, "LZW");
                final Compression compression =
                        Compression.valueOf(compressionStr.toUpperCase());
                opList.setOutputCompression(compression);
                break;
        }

        // At this point, the list is complete, so it can be safely frozen.
        // This will prevent it from being modified by clients (e.g. processors)
        // which could interfere with e.g. caching.
        opList.freeze();
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
     * via {@link #SLASH_SUBSTITUTE_CONFIG_KEY}.
     *
     * @param uriPathComponent Path component (a part of the path before,
     *                         after, or between slashes)
     * @return Path component with slashes decoded
     */
    protected final String decodeSlashes(final String uriPathComponent) {
        final String substitute = ConfigurationFactory.getInstance().
                getString(SLASH_SUBSTITUTE_CONFIG_KEY, "");
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
            final Configuration config = ConfigurationFactory.getInstance();
            final boolean enabled = config.getBoolean(
                    CLIENT_CACHE_ENABLED_CONFIG_KEY, false);
            if (enabled) {
                final String maxAge = config.getString(
                        CLIENT_CACHE_MAX_AGE_CONFIG_KEY);
                if (maxAge != null && maxAge.length() > 0) {
                    directives.add(CacheDirective.maxAge(Integer.parseInt(maxAge)));
                }
                String sMaxAge = config.getString(
                        CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY);
                if (sMaxAge != null && sMaxAge.length() > 0) {
                    directives.add(CacheDirective.
                            sharedMaxAge(Integer.parseInt(sMaxAge)));
                }
                if (config.getBoolean(CLIENT_CACHE_PUBLIC_CONFIG_KEY, true)) {
                    directives.add(CacheDirective.publicInfo());
                } else if (config.getBoolean(CLIENT_CACHE_PRIVATE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.privateInfo());
                }
                if (config.getBoolean(CLIENT_CACHE_NO_CACHE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.noCache());
                }
                if (config.getBoolean(CLIENT_CACHE_NO_STORE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.noStore());
                }
                if (config.getBoolean(CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.mustRevalidate());
                }
                if (config.getBoolean(CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY, false)) {
                    directives.add(CacheDirective.proxyMustRevalidate());
                }
                if (config.getBoolean(CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY, false)) {
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
        final long maxAllowedSize = (ops.isNoOp(format)) ?
                0 : ConfigurationFactory.getInstance().getLong(MAX_PIXELS_CONFIG_KEY, 0);

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

}
