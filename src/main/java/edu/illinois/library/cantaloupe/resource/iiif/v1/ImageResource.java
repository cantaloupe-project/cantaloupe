package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.CachedImageRepresentation;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.Disposition;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles IIIF Image API 1.1 image requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#url-syntax-image-request">Image
 * Request Operations</a>
 */
public class ImageResource extends IIIF1Resource {

    /**
     * Format to assume when no extension is present in the URI.
     */
    private static final Format DEFAULT_FORMAT = Format.JPG;

    /**
     * Responds to image requests.
     */
    @Get
    @SuppressWarnings("unused")
    public Representation doGet() throws Exception {
        final Configuration config = Configuration.getInstance();
        final Identifier identifier = getIdentifier();
        final Resolver resolver = new ResolverFactory().newResolver(identifier);

        // Setup the resolver context.
        final RequestContext requestContext = new RequestContext();
        requestContext.setRequestURI(getReference().toString());
        requestContext.setRequestHeaders(getRequest().getHeaders().getValuesMap());
        requestContext.setClientIP(getCanonicalClientIpAddress());
        requestContext.setCookies(getRequest().getCookies().getValuesMap());
        resolver.setContext(requestContext);

        // Determine the format of the source image.
        Format sourceFormat;
        try {
            sourceFormat = resolver.getSourceFormat();
        } catch (FileNotFoundException e) { // this needs to be rethrown
            if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                // If the image was not found, purge it from the cache.
                new CacheFacade().purgeAsync(identifier);
            }
            throw e;
        }

        // Obtain an instance of the processor assigned to that format.
        final Processor processor = new ProcessorFactory().
                newProcessor(sourceFormat);

        // Connect it to the resolver.
        new ProcessorConnector(resolver, processor, identifier).connect();

        final Set<Format> availableOutputFormats =
                processor.getAvailableOutputFormats();

        // Extract the quality and format from the URI.
        final Map<String,Object> attrs = getRequest().getAttributes();
        String[] qualityAndFormat = StringUtils.
                split((String) attrs.get("quality_format"), ".");
        // If a format is present, try to use that. Otherwise, guess it based
        // on the Accept header per Image API 1.1 spec section 4.5.
        String outputFormat;
        if (qualityAndFormat.length > 1) {
            outputFormat = qualityAndFormat[qualityAndFormat.length - 1];
        } else {
            outputFormat = getOutputFormat(availableOutputFormats).
                    getPreferredExtension();
        }

        // Assemble the URI parameters into an OperationList instance.
        final OperationList ops = new Parameters(
                identifier,
                (String) attrs.get("region"),
                (String) attrs.get("size"),
                (String) attrs.get("rotation"),
                qualityAndFormat[0],
                outputFormat).toOperationList();
        ops.getOptions().putAll(
                getReference().getQueryAsForm(true).getValuesMap());

        final Info info = getOrReadInfo(identifier, processor);
        final Dimension fullSize = info.getSize();

        StringRepresentation redirectingRep = checkAuthorization(ops, fullSize);
        if (redirectingRep != null) {
            return redirectingRep;
        }

        validateRequestedArea(ops, sourceFormat, info);

        processor.validate(ops, fullSize);

        final Disposition disposition = getRepresentationDisposition(
                getReference().getQueryAsForm()
                        .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG),
                ops.getIdentifier(), ops.getOutputFormat());

        // If we don't need to resolve first, and are using a cache, and the
        // cache contains an image matching the request, skip all the setup and
        // just return the cached image.
        final CacheFacade cacheFacade = new CacheFacade();

        if (!config.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true) &&
                cacheFacade.isDerivativeCacheAvailable()) {
            InputStream inputStream =
                    cacheFacade.newDerivativeImageInputStream(ops);
            if (inputStream != null) {
                return new CachedImageRepresentation(
                        ops.getOutputFormat().getPreferredMediaType(),
                        disposition, inputStream);
            }
        }

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                processor.getSupportedFeatures(),
                processor.getSupportedIIIF1Qualities(),
                processor.getAvailableOutputFormats());
        getBufferedResponseHeaders().add("Link",
                String.format("<%s>;rel=\"profile\";", complianceLevel.getUri()));

        ops.applyNonEndpointMutations(fullSize,
                info.getOrientation(),
                getCanonicalClientIpAddress(),
                getReference().toUri(),
                getRequest().getHeaders().getValuesMap(),
                getCookies().getValuesMap());

        // Find out whether the processor supports the source format by asking
        // it whether it offers any output formats for it.
        if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            String msg = String.format("%s does not support the \"%s\" output format",
                    processor.getClass().getSimpleName(),
                    ops.getOutputFormat().getPreferredExtension());
            getLogger().warning(msg + ": " + this.getReference());
            throw new UnsupportedSourceFormatException(msg);
        }

        commitCustomResponseHeaders();

        return new ImageRepresentation(info, processor, ops, disposition,
                isBypassingCache());
    }

    /**
     * @param limitToFormats Set of OutputFormats to limit the result to.
     * @return The best output format based on the URI extension, Accept
     *         header, or default, as outlined by the Image API 1.1 spec.
     */
    private Format getOutputFormat(Set<Format> limitToFormats) {
        // Check for a format extension in the URI.
        Format format = null;
        if (getReference().getExtensions() != null) {
            for (Format f : Format.values()) {
                if (f.getPreferredExtension().
                        equals(getReference().getExtensions())) {
                    format = f;
                    break;
                }
            }
        }
        if (format == null) { // if none, check the Accept header.
            format = getPreferredOutputFormat(limitToFormats);
            if (format == null) {
                format = DEFAULT_FORMAT;
            }
        }
        return format;
    }

    /**
     * @param limitToFormats Set of OutputFormats to limit the result to.
     * @return Best OutputFormat for the client preferences as specified in the
     *         Accept header.
     */
    private Format getPreferredOutputFormat(Set<Format> limitToFormats) {
        // Transform limitToFormats into a list in order of the application's
        // format preference, in case the client supplies something like */*.
        final List<Format> appPreferredFormats = new ArrayList<>();
        appPreferredFormats.addAll(limitToFormats);
        appPreferredFormats.sort((Format o1, Format o2) -> {
            // Default format goes first.
            if (DEFAULT_FORMAT.equals(o1)) {
                return -1;
            } else if (DEFAULT_FORMAT.equals(o2)) {
                return 1;
            }
            return 0;
        });

        // Transform the list of Formats into a list of Variants.
        final List<Variant> variants = appPreferredFormats.stream().
                map(f -> new Variant(new org.restlet.data.MediaType(
                        f.getPreferredMediaType().toString()))).
                collect(Collectors.toList());

        Variant preferred = getPreferredVariant(variants);
        if (preferred != null) {
            String mediaTypeStr = preferred.getMediaType().toString();
            return new MediaType(mediaTypeStr).toFormat();
        }
        return null;
    }

}
