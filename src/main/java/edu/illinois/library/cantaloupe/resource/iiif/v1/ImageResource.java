package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.AccessDeniedException;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles IIIF Image API 1.1 image requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#url-syntax-image-request">Image
 * Request Operations</a>
 */
public class ImageResource extends AbstractResource {

    private static final Logger logger = LoggerFactory.
            getLogger(ImageResource.class);

    public static final String ENDPOINT_ENABLED_CONFIG_KEY =
            "endpoint.iiif.1.enabled";

    /**
     * Format to assume when no extension is present in the URI.
     */
    private static final OutputFormat DEFAULT_FORMAT = OutputFormat.JPG;

    @Override
    protected void doInit() throws ResourceException {
        if (!Application.getConfiguration().
                getBoolean(ENDPOINT_ENABLED_CONFIG_KEY, true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

    /**
     * Responds to image requests.
     *
     * @return ImageRepresentation
     * @throws Exception
     */
    @Get
    public ImageRepresentation doGet() throws Exception {
        final Map<String,Object> attrs = this.getRequest().getAttributes();
        Identifier identifier =
                new Identifier(Reference.decode((String) attrs.get("identifier")));
        identifier = decodeSlashes(identifier);

        final Resolver resolver = ResolverFactory.getResolver(identifier);
        // Determine the format of the source image
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        try {
            sourceFormat = resolver.getSourceFormat(identifier);
        } catch (FileNotFoundException e) {
            if (Application.getConfiguration().
                    getBoolean(PURGE_MISSING_CONFIG_KEY, false)) {
                // if the image was not found, purge it from the cache
                final Cache cache = CacheFactory.getInstance();
                if (cache != null) {
                    cache.purge(identifier);
                }
            }
            throw e;
        }

        // Obtain an instance of the processor assigned to that format in
        // the config file. This will throw a variety of exceptions if there
        // are any issues.
        final Processor proc = ProcessorFactory.getProcessor(
                resolver, identifier, sourceFormat);

        final Set<OutputFormat> availableOutputFormats =
                proc.getAvailableOutputFormats();

        // Extract the quality and format from the URI
        String[] qualityAndFormat = StringUtils.split((String) attrs.get("quality_format"), ".");
        // If a format is present, try to use that. Otherwise, guess it based
        // on the Accept header per Image API 1.1 spec section 4.5.
        String outputFormat;
        if (qualityAndFormat.length > 1) {
            outputFormat = qualityAndFormat[qualityAndFormat.length - 1];
        } else {
            outputFormat = getOutputFormat(availableOutputFormats).getExtension();
        }

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                proc.getSupportedFeatures(),
                proc.getSupportedIiif1_1Qualities(),
                proc.getAvailableOutputFormats());
        this.addHeader("Link", String.format("<%s>;rel=\"profile\";",
                complianceLevel.getUri()));

        // Assemble the URI parameters into an OperationList objects
        final OperationList ops = new Parameters(
                (String) attrs.get("identifier"),
                (String) attrs.get("region"),
                (String) attrs.get("size"),
                (String) attrs.get("rotation"),
                qualityAndFormat[0],
                outputFormat).toOperationList();
        ops.setIdentifier(identifier);
        ops.getOptions().putAll(
                this.getReference().getQueryAsForm(true).getValuesMap());

        final Dimension fullSize = getSize(identifier, proc);

        if (!isAuthorized(ops, fullSize)) {
            throw new AccessDeniedException();
        }

        addNonEndpointOperations(ops, fullSize);

        // Find out whether the processor supports that source format by
        // asking it whether it offers any output formats for it
        if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            String msg = String.format("%s does not support the \"%s\" output format",
                    proc.getClass().getSimpleName(),
                    ops.getOutputFormat().getExtension());
            logger.warn(msg + ": " + this.getReference());
            throw new UnsupportedSourceFormatException(msg);
        }

        Disposition disposition = getRepresentationDisposition(
                ops.getIdentifier(), ops.getOutputFormat());
        return getRepresentation(ops, sourceFormat, disposition, proc);
    }

    /**
     * @param limitToFormats Set of OutputFormats to limit the result to.
     * @return The best output format based on the URI extension, Accept
     * header, or default, as outlined by the Image API 1.1 spec.
     */
    private OutputFormat getOutputFormat(Set<OutputFormat> limitToFormats) {
        // check the URI for a format in the extension
        OutputFormat format = null;
        for (OutputFormat f : OutputFormat.values()) {
            if (f.getExtension().equals(this.getReference().getExtensions())) {
                format = f;
                break;
            }
        }
        if (format == null) { // if none, check the Accept header
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
     * Accept header.
     */
    private OutputFormat getPreferredOutputFormat(Set<OutputFormat> limitToFormats) {
        List<Variant> variants = new ArrayList<>();
        for (OutputFormat format : limitToFormats) {
            variants.add(new Variant(new MediaType(format.getMediaType())));
        }
        Variant preferred = getPreferredVariant(variants);
        if (preferred != null) {
            String mediaTypeStr = preferred.getMediaType().toString();
            OutputFormat format = OutputFormat.getOutputFormat(mediaTypeStr);
            return format;
        }
        return null;
    }

}
