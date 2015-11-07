package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.File;
import java.io.InputStream;
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

    private static Logger logger = LoggerFactory.
            getLogger(ImageResource.class);

    /**
     * Format to assume when no extension is present in the URI.
     */
    private static final OutputFormat DEFAULT_FORMAT = OutputFormat.JPG;

    /**
     * Responds to image requests.
     *
     * @return ImageRepresentation
     * @throws Exception
     */
    @Get
    public ImageRepresentation doGet() throws Exception {
        Map<String,Object> attrs = this.getRequest().getAttributes();

        Resolver resolver = ResolverFactory.getResolver();
        // Determine the format of the source image
        SourceFormat sourceFormat = resolver.getSourceFormat(
                new Identifier((String) attrs.get("identifier")));
        if (sourceFormat.equals(SourceFormat.UNKNOWN)) {
            throw new UnsupportedSourceFormatException();
        }
        // Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);

        Set<OutputFormat> availableOutputFormats =
                proc.getAvailableOutputFormats(sourceFormat);

        // Assemble the URI parameters into a Parameters object
        Parameters params = new Parameters(
                (String) attrs.get("identifier"),
                (String) attrs.get("region"),
                (String) attrs.get("size"),
                (String) attrs.get("rotation"),
                StringUtils.split((String) attrs.get("quality"), ".")[0],
                getOutputFormat(availableOutputFormats).getExtension());
        Operations ops = params.toOperations();

        ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                proc.getSupportedFeatures(sourceFormat),
                proc.getSupportedQualities(sourceFormat),
                proc.getAvailableOutputFormats(sourceFormat));
        this.addHeader("Link", String.format("<%s>;rel=\"profile\";",
                complianceLevel.getUri()));

        // Find out whether the processor supports that source format by
        // asking it whether it offers any output formats for it
        if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            String msg = String.format("%s does not support the \"%s\" output format",
                    proc.getClass().getSimpleName(),
                    ops.getOutputFormat().getExtension());
            logger.warn(msg + ": " + this.getReference());
            throw new UnsupportedSourceFormatException(msg);
        }

        MediaType mediaType = new MediaType(
                ops.getOutputFormat().getMediaType());

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
            File inputFile = ((FileResolver) resolver).
                    getFile(ops.getIdentifier());
            return new ImageRepresentation(mediaType, sourceFormat, ops,
                    inputFile);
        } else if (resolver instanceof StreamResolver) {
            logger.debug("Using {} as a StreamProcessor",
                    proc.getClass().getSimpleName());
            StreamResolver sres = (StreamResolver) resolver;
            if (proc instanceof StreamProcessor) {
                StreamProcessor sproc = (StreamProcessor) proc;
                InputStream inputStream = sres.
                        getInputStream(ops.getIdentifier());
                Dimension fullSize = sproc.getSize(inputStream, sourceFormat);
                // avoid reusing the stream
                inputStream = sres.getInputStream(ops.getIdentifier());
                return new ImageRepresentation(mediaType, sourceFormat, fullSize,
                        ops, inputStream);
            }
        }
        return null; // this should never happen
    }

    /**
     * @param limitToFormats Set of OutputFormats to limit the
     * result to
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
     * @param limitToFormats Set of OutputFormats to limit the
     * result to
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
