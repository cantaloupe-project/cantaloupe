package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.processor.ChannelProcessor;
import edu.illinois.library.cantaloupe.resolver.ChannelResolver;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.CacheDirective;
import org.restlet.data.Disposition;
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
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
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
    public static final String SLASH_SUBSTITUTE_CONFIG_KEY =
            "slash_substitute";

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

    /**
     * Should be called by all relevant resource implementations.
     *
     * @throws ConfigurationException If the given resolver and processor are
     * incompatible.
     */
    protected void checkProcessorResolverCompatibility(Resolver resolver,
                                                       Processor processor)
            throws ConfigurationException {
        if (!resolver.isCompatible(processor)) {
            throw new ConfigurationException(
                    String.format("%s is not compatible with %s",
                            processor.getClass().getSimpleName(),
                            resolver.getClass().getSimpleName()));
        }
    }

    /**
     * Some web servers have issues dealing with encoded slashes (%2F) in URLs.
     * This method enables the use of an alternate string to represent a slash
     * via {@link #SLASH_SUBSTITUTE_CONFIG_KEY}.
     *
     * @param uriPathComponent
     * @return
     */
    protected String decodeSlashes(final String uriPathComponent) {
        final String substitute = Application.getConfiguration().
                getString(SLASH_SUBSTITUTE_CONFIG_KEY, "");
        if (substitute.length() > 0) {
            return StringUtils.replace(uriPathComponent, substitute, "/");
        }
        return uriPathComponent;
    }

    protected Identifier decodeSlashes(final Identifier identifier) {
        return new Identifier(decodeSlashes(identifier.toString()));
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
    protected Reference getPublicRootRef() {
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
                                                    Disposition disposition,
                                                    Resolver resolver,
                                                    Processor proc)
            throws IOException, ProcessorException {
        final MediaType mediaType = new MediaType(
                ops.getOutputFormat().getMediaType());
        // Max allowed size is ignored when the processing is a no-op.
        final long maxAllowedSize = (ops.isNoOp(sourceFormat)) ?
                0 : Application.getConfiguration().getLong(MAX_PIXELS_CONFIG_KEY, 0);

        if (resolver instanceof FileResolver &&
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
                    ops, disposition, inputFile);
        } else if (resolver instanceof ChannelResolver) {
            logger.debug("Using {} as a ChannelProcessor",
                    proc.getClass().getSimpleName());
            final ChannelResolver chRes = (ChannelResolver) resolver;
            if (proc instanceof ChannelProcessor) {
                final ChannelProcessor sproc = (ChannelProcessor) proc;
                ReadableByteChannel readableChannel = chRes.
                        getChannel(ops.getIdentifier());
                final Dimension fullSize = sproc.getSize(readableChannel,
                        sourceFormat);
                final Dimension effectiveSize = ops.getResultingSize(fullSize);
                if (maxAllowedSize > 0 &&
                        effectiveSize.width * effectiveSize.height > maxAllowedSize) {
                    throw new PayloadTooLargeException();
                }
                // avoid reusing the channel
                readableChannel = chRes.getChannel(ops.getIdentifier());
                return new ImageRepresentation(mediaType, sourceFormat,
                        fullSize, ops, disposition, readableChannel);
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
                size = ((FileProcessor) proc).getSize(
                        ((FileResolver) resolver).getFile(identifier),
                        sourceFormat);
            } else if (proc instanceof ChannelProcessor) {
                size = ((ChannelProcessor) proc).getSize(
                        ((ChannelResolver) resolver).getChannel(identifier),
                        sourceFormat);
            }
        } else if (resolver instanceof ChannelResolver) {
            if (!(proc instanceof ChannelProcessor)) {
                // ChannelResolvers and FileProcessors are incompatible
            } else {
                size = ((ChannelProcessor) proc).getSize(
                        ((ChannelResolver) resolver).getChannel(identifier),
                        sourceFormat);
            }
        }
        return size;
    }

}
