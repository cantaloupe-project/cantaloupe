package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.CacheDirective;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class AbstractResource extends ServerResource {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractResource.class);

    private static final String MAX_PIXELS_CONFIG_KEY = "max_pixels";
    protected static final String PURGE_MISSING_CONFIG_KEY =
            "cache.server.purge_missing";
    protected static final String RESOLVE_FIRST_CONFIG_KEY =
            "cache.server.resolve_first";

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
     * @return A root reference usable in public, respecting the
     * <code>base_uri</code> option in the application configuration.
     */
    protected Reference getPublicRootRef() { // TODO: move into a util class and unit-test
        Reference rootRef = getRootRef();
        final String baseUri = Application.getConfiguration().getString("base_uri");
        if (baseUri != null && baseUri.length() > 0) {
            rootRef = rootRef.clone();
            Reference baseRef = new Reference(baseUri);
            rootRef.setScheme(baseRef.getScheme());
            rootRef.setHostDomain(baseRef.getHostDomain());
            // if the "port" is a local socket, Reference will actually put -1
            // in the URL.
            if (baseRef.getHostPort() == -1) {
                rootRef.setHostPort(null);
            } else {
                rootRef.setHostPort(baseRef.getHostPort());
            }
            rootRef.setPath(StringUtils.stripEnd(baseRef.getPath(), "/"));
        }
        return rootRef;
    }

    protected ImageRepresentation getRepresentation(OperationList ops,
                                                    SourceFormat sourceFormat,
                                                    Resolver resolver,
                                                    Processor proc)
            throws Exception {
        final MediaType mediaType = new MediaType(
                ops.getOutputFormat().getMediaType());
        final long maxAllowedSize = Application.getConfiguration().
                getLong(MAX_PIXELS_CONFIG_KEY, 0);

        // FileResolver -> StreamProcessor: OK, using FileInputStream
        // FileResolver -> FileProcessor: OK, using File
        // StreamResolver -> StreamProcessor: OK, using InputStream
        // StreamResolver -> FileProcessor: NOPE
        if (!(resolver instanceof FileResolver) &&
                !(proc instanceof StreamProcessor)) {
            // FileProcessors can't work with StreamResolvers
            throw new UnsupportedSourceFormatException(
                    String.format("%s is not compatible with %s",
                            proc.getClass().getSimpleName(),
                            resolver.getClass().getSimpleName()));
        } else if (resolver instanceof FileResolver &&
                proc instanceof FileProcessor) {
            logger.debug("Using {} as a FileProcessor",
                    proc.getClass().getSimpleName());
            final FileProcessor fproc = (FileProcessor) proc;
            final File inputFile = ((FileResolver) resolver).
                    getFile(ops.getIdentifier());
            final Dimension fullSize = fproc.getSize(inputFile, sourceFormat);
            final Dimension effectiveSize = ops.getResultingSize(fullSize);
            if (maxAllowedSize > 0 &&
                    effectiveSize.width * effectiveSize.height > maxAllowedSize) {
                throw new PayloadTooLargeException();
            }
            return new ImageRepresentation(mediaType, sourceFormat, fullSize,
                    ops, inputFile);
        } else if (resolver instanceof StreamResolver) {
            logger.debug("Using {} as a StreamProcessor",
                    proc.getClass().getSimpleName());
            final StreamResolver sres = (StreamResolver) resolver;
            if (proc instanceof StreamProcessor) {
                final StreamProcessor sproc = (StreamProcessor) proc;
                InputStream inputStream = sres.
                        getInputStream(ops.getIdentifier());
                final Dimension fullSize = sproc.getSize(inputStream,
                        sourceFormat);
                final Dimension effectiveSize = ops.getResultingSize(fullSize);
                if (maxAllowedSize > 0 &&
                        effectiveSize.width * effectiveSize.height > maxAllowedSize) {
                    throw new PayloadTooLargeException();
                }
                // avoid reusing the stream
                inputStream = sres.getInputStream(ops.getIdentifier());
                return new ImageRepresentation(mediaType, sourceFormat,
                        fullSize, ops, inputStream);
            }
        }
        return null; // should never happen
    }

    /**
     * Gets the size of the image corresponding to the given identifier, first
     * by checking the cache and then, if necessary, by reading it from the
     * image and caching the result.
     *
     * @param identifier
     * @param proc
     * @param resolver
     * @param sourceFormat
     * @return
     * @throws Exception
     */
    protected Dimension getSize(Identifier identifier, Processor proc,
                                Resolver resolver, SourceFormat sourceFormat)
            throws Exception {
        Dimension size = null;
        Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            size = cache.getDimension(identifier);
            if (size == null) {
                size = readSize(identifier, resolver, proc, sourceFormat);
                cache.putDimension(identifier, size);
            }
        }
        if (size == null) {
            size = readSize(identifier, resolver, proc, sourceFormat);
        }
        return size;
    }

    /**
     * Reads the size from the source image.
     *
     * @param identifier
     * @param resolver
     * @param proc
     * @param sourceFormat
     * @return
     * @throws Exception
     */
    protected Dimension readSize(Identifier identifier, Resolver resolver,
                                 Processor proc, SourceFormat sourceFormat)
            throws Exception {
        Dimension size = null;
        if (resolver instanceof FileResolver) {
            if (proc instanceof FileProcessor) {
                size = ((FileProcessor)proc).getSize(
                        ((FileResolver) resolver).getFile(identifier),
                        sourceFormat);
            } else if (proc instanceof StreamProcessor) {
                size = ((StreamProcessor)proc).getSize(
                        ((StreamResolver) resolver).getInputStream(identifier),
                        sourceFormat);
            }
        } else if (resolver instanceof StreamResolver) {
            if (!(proc instanceof StreamProcessor)) {
                // StreamResolvers don't support FileProcessors
            } else {
                size = ((StreamProcessor)proc).getSize(
                        ((StreamResolver) resolver).getInputStream(identifier),
                        sourceFormat);
            }
        }
        return size;
    }

}
