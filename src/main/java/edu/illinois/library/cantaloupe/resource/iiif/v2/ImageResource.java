package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.AccessDeniedException;
import edu.illinois.library.cantaloupe.resource.CachedImageRepresentation;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.iiif.ResourceUtils;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.representation.WritableRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.Set;

/**
 * Handles IIIF image requests.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-request-parameters">Image
 * Request Operations</a>
 */
public class ImageResource extends AbstractResource {

    private static Logger logger = LoggerFactory.getLogger(ImageResource.class);

    public static final String CONTENT_DISPOSITION_CONFIG_KEY =
            "endpoint.iiif.content_disposition";
    public static final String ENDPOINT_ENABLED_CONFIG_KEY =
            "endpoint.iiif.2.enabled";

    @Override
    protected void doInit() throws ResourceException {
        if (!Application.getConfiguration().
                getBoolean(ENDPOINT_ENABLED_CONFIG_KEY, true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
        getResponseCacheDirectives().addAll(getCacheDirectives());
    }

    /**
     * Responds to IIIF Image requests.
     *
     * @return OutputRepresentation
     * @throws Exception
     */
    @Get
    public WritableRepresentation doGet() throws Exception {
        final Map<String,Object> attrs = this.getRequest().getAttributes();
        // Assemble the URI parameters into a Parameters object
        final Parameters params = new Parameters(
                (String) attrs.get("identifier"),
                (String) attrs.get("region"),
                (String) attrs.get("size"),
                (String) attrs.get("rotation"),
                (String) attrs.get("quality"),
                (String) attrs.get("format"));
        final OperationList ops = params.toOperationList();
        ops.setIdentifier(decodeSlashes(ops.getIdentifier()));
        ops.getOptions().putAll(
                this.getReference().getQueryAsForm(true).getValuesMap());

        final Disposition disposition = ResourceUtils.getRepresentationDisposition(
                ops.getIdentifier(), ops.getOutputFormat());

        // If we don't need to resolve first, and are using a cache, and the
        // cache contains an image matching the request, skip all the setup and
        // just return the cached image.
        if (!Application.getConfiguration().
                getBoolean(RESOLVE_FIRST_CONFIG_KEY, true)) {
            Cache cache = CacheFactory.getInstance();
            if (cache != null) {
                ReadableByteChannel readableChannel =
                        cache.getImageReadableChannel(ops);
                if (readableChannel != null) {
                    this.addLinkHeader(params);
                    return new CachedImageRepresentation(
                            new MediaType(params.getOutputFormat().getMediaType()),
                            disposition, readableChannel);
                }
            }
        }

        Resolver resolver = ResolverFactory.getResolver(ops.getIdentifier());
        // Determine the format of the source image
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        try {
            sourceFormat = resolver.getSourceFormat(ops.getIdentifier());
        } catch (FileNotFoundException e) {
            if (Application.getConfiguration().
                    getBoolean(PURGE_MISSING_CONFIG_KEY, false)) {
                // if the image was not found, purge it from the cache
                final Cache cache = CacheFactory.getInstance();
                if (cache != null) {
                    cache.purge(ops.getIdentifier());
                }
            }
            throw e;
        }
        if (sourceFormat.equals(SourceFormat.UNKNOWN)) {
            throw new UnsupportedSourceFormatException();
        }
        // Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);

        checkProcessorResolverCompatibility(resolver, proc);

        if (!isAuthorized(ops,
                getSize(ops.getIdentifier(), proc, resolver, sourceFormat))) {
            throw new AccessDeniedException();
        }

        // Find out whether the processor supports that source format by
        // asking it whether it offers any output formats for it
        Set<OutputFormat> availableOutputFormats =
                proc.getAvailableOutputFormats(sourceFormat);
        if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            String msg = String.format("%s does not support the \"%s\" output format",
                    proc.getClass().getSimpleName(),
                    ops.getOutputFormat().getExtension());
            logger.warn(msg + ": " + this.getReference());
            throw new UnsupportedSourceFormatException(msg);
        }

        this.addLinkHeader(params);

        return getRepresentation(ops, sourceFormat, disposition, resolver,
                proc);
    }

    private void addLinkHeader(Parameters params) {
        this.addHeader("Link", String.format("<%s%s/%s>;rel=\"canonical\"",
                getPublicRootRef(getRequest()).toString(),
                WebApplication.IIIF_2_PATH, params.toString()));
    }

}
