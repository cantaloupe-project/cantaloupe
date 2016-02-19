package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.ImageInfo;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.watermark.WatermarkService;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.restlet.Request;
import org.restlet.data.CacheDirective;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
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

    public static final String BASE_URI_CONFIG_KEY = "base_uri";
    public static final String CONTENT_DISPOSITION_CONFIG_KEY =
            "endpoint.iiif.content_disposition";
    public static final String MAX_PIXELS_CONFIG_KEY = "max_pixels";
    public static final String PURGE_MISSING_CONFIG_KEY =
            "cache.server.purge_missing";
    public static final String RESOLVE_FIRST_CONFIG_KEY =
            "cache.server.resolve_first";
    public static final String SLASH_SUBSTITUTE_CONFIG_KEY =
            "slash_substitute";

    /**
     * @return Map of template variables common to most or all views, such as
     * variables that appear in a common header.
     */
    public static Map<String, Object> getCommonTemplateVars(Request request) {
        Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        vars.put("baseUri", getPublicRootRef(request).toString());
        return vars;
    }

    /**
     * @param request
     * @return A root reference usable in public, respecting the
     * <code>base_uri</code> option in the application configuration.
     */
    public static Reference getPublicRootRef(final Request request) {
        Reference rootRef = new Reference(request.getRootRef());

        final String baseUri = Application.getConfiguration().
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
            final String protocolStr = headers.getFirstValue("X-Forwarded-Proto",
                    true, "HTTP");
            final String hostStr = headers.getFirstValue("X-Forwarded-Host",
                    true, null);
            final String portStr = headers.getFirstValue("X-Forwarded-Port",
                    true, null);
            final String pathStr = headers.getFirstValue("X-Forwarded-Path",
                    true, null);
            if (hostStr != null) {
                logger.info("Assembling base URI from X-Forwarded-* headers. " +
                                "Proto: {}; Host: {}; Port: {}; Path: {}",
                        protocolStr, hostStr, portStr, pathStr);

                rootRef.setHostDomain(hostStr);
                rootRef.setPath(pathStr);

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
     * @return A content disposition based on the setting of
     * {@link #CONTENT_DISPOSITION_CONFIG_KEY} in the application configuration.
     * If it is set to <code>attachment</code>, the disposition will have a
     * filename set to a reasonable value based on the given identifier and
     * output format.
     */
    public static Disposition getRepresentationDisposition(
            Identifier identifier, Format outputFormat) {
        Disposition disposition = new Disposition();
        switch (Application.getConfiguration().
                getString(CONTENT_DISPOSITION_CONFIG_KEY, "none")) {
            case "inline":
                disposition.setType(Disposition.TYPE_INLINE);
                break;
            case "attachment":
                disposition.setType(Disposition.TYPE_ATTACHMENT);
                disposition.setFilename(
                        identifier.toString().replaceAll(
                                ImageRepresentation.FILENAME_CHARACTERS, "_") +
                                "." + outputFormat.getPreferredExtension());
                break;
        }
        return disposition;
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        addHeader("X-Powered-By", "Cantaloupe/" + Application.getVersion());
    }

    /**
     * Convenience method that adds a response header.
     *
     * @param key Header key
     * @param value Header value
     */
    @SuppressWarnings({"unchecked"})
    protected final void addHeader(String key, String value) {
        Series<Header> responseHeaders = (Series<Header>) getResponse().
                getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Series(Header.class);
            getResponse().getAttributes().
                    put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add(new Header(key, value));
    }

    /**
     * @param opList
     * @param fullSize
     */
    public void addNonEndpointOperations(final OperationList opList,
                                         final Dimension fullSize) {
        if (WatermarkService.isEnabled()) {
            try {
                opList.add(WatermarkService.newWatermark(
                        opList, fullSize, getReference().toUrl(),
                        getRequest().getHeaders().getValuesMap(),
                        getRequest().getClientInfo().getAddress(),
                        getRequest().getCookies().getValuesMap()));
            } catch (DelegateScriptDisabledException e) {
                // no problem
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
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
        final String substitute = Application.getConfiguration().
                getString(SLASH_SUBSTITUTE_CONFIG_KEY, "");
        if (substitute.length() > 0) {
            return StringUtils.replace(uriPathComponent, substitute, "/");
        }
        return uriPathComponent;
    }

    protected final Identifier decodeSlashes(final Identifier identifier) {
        return new Identifier(decodeSlashes(identifier.toString()));
    }

    protected final List<CacheDirective> getCacheDirectives() {
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
                if (config.getBoolean("cache.client.public", true)) {
                    directives.add(CacheDirective.publicInfo());
                } else if (config.getBoolean("cache.client.private", false)) {
                    directives.add(CacheDirective.privateInfo());
                }
                if (config.getBoolean("cache.client.no_cache", false)) {
                    directives.add(CacheDirective.noCache());
                }
                if (config.getBoolean("cache.client.no_store", false)) {
                    directives.add(CacheDirective.noStore());
                }
                if (config.getBoolean("cache.client.must_revalidate", false)) {
                    directives.add(CacheDirective.mustRevalidate());
                }
                if (config.getBoolean("cache.client.proxy_revalidate", false)) {
                    directives.add(CacheDirective.proxyMustRevalidate());
                }
                if (config.getBoolean("cache.client.no_transform", false)) {
                    directives.add(CacheDirective.noTransform());
                }
            }
        } catch (NoSuchElementException e) {
            logger.warn("Cache-Control headers are invalid: {}",
                    e.getMessage());
        }
        return directives;
    }

    protected ImageRepresentation getRepresentation(OperationList ops,
                                                    Format format,
                                                    Disposition disposition,
                                                    Processor proc)
            throws IOException, ProcessorException {
        // Max allowed size is ignored when the processing is a no-op.
        final long maxAllowedSize = (ops.isNoOp(format)) ?
                0 : Application.getConfiguration().getLong(MAX_PIXELS_CONFIG_KEY, 0);

        final Dimension fullSize = proc.getSize();
        final Dimension effectiveSize = ops.getResultingSize(fullSize);
        if (maxAllowedSize > 0 &&
                effectiveSize.width * effectiveSize.height > maxAllowedSize) {
            throw new PayloadTooLargeException();
        }

        return new ImageRepresentation(fullSize, proc, ops, disposition);
    }

    /**
     * Gets the size of the image corresponding to the given identifier, first
     * by checking the cache and then, if necessary, by reading it from the
     * image and caching the result.
     *
     * @param identifier
     * @param proc
     * @return
     * @throws Exception
     */
    protected final Dimension getSize(final Identifier identifier,
                                      final Processor proc)
            throws Exception {
        Dimension size = null;
        Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            long msec = System.currentTimeMillis();
            final ImageInfo info = cache.getImageInfo(identifier);
            if (info != null) {
                size = info.getSize();
                logger.info("Retrieved dimensions of {} from cache in {} msec",
                        identifier, System.currentTimeMillis() - msec);
            } else {
                size = readSize(identifier, proc);
                cache.putImageInfo(identifier, new ImageInfo(size));
            }
        }
        if (size == null) {
            size = readSize(identifier, proc);
        }
        return size;
    }

    /**
     * Invokes a delegate script method to determine whether the request is
     * authorized.
     *
     * @param opList
     * @param fullSize
     * @return
     * @throws IOException
     * @throws ScriptException
     */
    protected final boolean isAuthorized(final OperationList opList,
                                         final Dimension fullSize)
            throws IOException, ScriptException {
        final Map<String,Integer> fullSizeArg = new HashMap<>();
        fullSizeArg.put("width", fullSize.width);
        fullSizeArg.put("height", fullSize.height);

        final Dimension resultingSize = opList.getResultingSize(fullSize);
        final Map<String,Integer> resultingSizeArg = new HashMap<>();
        resultingSizeArg.put("width", resultingSize.width);
        resultingSizeArg.put("height", resultingSize.height);

        // delegate method parameters
        final Object args[] = new Object[9];
        args[0] = opList.getIdentifier().toString();           // identifier
        args[1] = fullSizeArg;                                 // full_size
        args[2] = opList.toMap(fullSize).get("operations");    // operations
        args[3] = resultingSizeArg;                            // resulting_size
        args[4] = opList.toMap(fullSize).get("output_format"); // output_format
        args[5] = getReference().toString();                   // request_uri
        args[6] = getRequest().getHeaders().getValuesMap();    // request_headers
        args[7] = getRequest().getClientInfo().getAddress();   // client_ip
        args[8] = getRequest().getCookies().getValuesMap();    // cookies

        try {
            final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
            final String method = "authorized?";
            return (boolean) engine.invoke(method, args);
        } catch (DelegateScriptDisabledException e) {
            logger.info("isAuthorized(): delegate script disabled; allowing.");
            return true;
        }
    }

    /**
     * Reads the size of the source image.
     *
     * @param identifier
     * @param proc
     * @return
     * @throws Exception
     */
    private Dimension readSize(final Identifier identifier,
                               final Processor proc) throws Exception {
        final long msec = System.currentTimeMillis();
        Dimension size = proc.getSize();
        logger.info("Read dimensions of {} in {} msec", identifier,
                System.currentTimeMillis() - msec);
        return size;
    }

}
