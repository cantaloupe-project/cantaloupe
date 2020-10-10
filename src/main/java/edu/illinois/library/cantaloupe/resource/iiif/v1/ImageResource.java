package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Handles IIIF Image API 1.1 image requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#url-syntax-image-request">Image
 * Request Operations</a>
 */
public class ImageResource extends IIIF1Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageResource.class);

    private static final List<String> AVAILABLE_OUTPUT_MEDIA_TYPES =
            List.of("image/jpeg", "image/tiff", "image/png", "image/gif");

    /**
     * Format to assume when no extension is present in the URI.
     */
    private static final Format DEFAULT_FORMAT = Format.get("jpg");

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    /**
     * <p>Responds to image requests.</p>
     *
     * <p>N.B.: This method only respects {@link
     * Key#CACHE_SERVER_RESOLVE_FIRST} for infos, as doing so with images is
     * not really possible using current API.</p>
     */
    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }

        final OperationList opList = getOperationList();

        class CustomCallback implements ImageRequestHandler.Callback {
            @Override
            public void willStreamImageFromDerivativeCache() {
                throw new RuntimeException("This method is not supposed to get called");
            }

            @Override
            public boolean preAuthorize() throws Exception {
                return ImageResource.this.preAuthorize();
            }

            @Override
            public boolean authorize() throws Exception {
                return ImageResource.this.authorize();
            }

            @Override
            public void willProcessImage(Processor processor,
                                         Info info) throws Exception {
                final Dimension fullSize = info.getSize(getPageIndex());
                validateScale(info.getMetadata().getOrientation().adjustedSize(fullSize),
                        (Scale) opList.getFirst(Scale.class),
                        Status.FORBIDDEN);

                final String disposition = getRepresentationDisposition(
                        getIdentifier(), opList.getOutputFormat());
                addHeaders(processor.getAvailableOutputFormats(),
                        opList.getOutputFormat(), disposition);
            }
        }

        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withOperationList(opList)
                .withBypassingCache(isBypassingCache())
                .withBypassingCacheRead(isBypassingCacheRead())
                .optionallyWithDelegateProxy(getDelegateProxy(), getRequestContext())
                .withCallback(new CustomCallback())
                .build()) {
            handler.handle(getResponse().getOutputStream());
        }
    }

    private void addHeaders(Set<Format> availableOutputFormats,
                            Format outputFormat,
                            String disposition) {
        if (disposition != null) {
            getResponse().setHeader("Content-Disposition", disposition);
        }
        getResponse().setHeader("Content-Type",
                outputFormat.getPreferredMediaType().toString());

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                availableOutputFormats);
        getResponse().setHeader("Link",
                String.format("<%s>;rel=\"profile\";", complianceLevel.getUri()));
    }

    private OperationList getOperationList() {
        final List<String> args = getPathArguments();

        // If the URI path contains a format extension, try to use that.
        // Otherwise, negotiate it based on the Accept header per Image API 1.1
        // spec section 4.5.
        String outputFormat;
        try {
            outputFormat = args.get(5);
        } catch (IndexOutOfBoundsException e) {
            outputFormat = getEffectiveOutputFormat().getPreferredExtension();
        }

        final Identifier identifier = getIdentifier();
        final Parameters params = new Parameters(
                identifier.toString(), args.get(1), args.get(2),
                args.get(3), args.get(4), outputFormat);

        final OperationList ops = params.toOperationList();
        ops.setPageIndex(getPageIndex());
        ops.setScaleConstraint(getScaleConstraint());
        ops.getOptions().putAll(getRequest().getReference().getQuery().toMap());
        return ops;
    }

    /**
     * Negotiates an output format.
     *
     * @return The best output format based on the URI extension, {@code
     *         Accept} header, or default.
     */
    private Format getEffectiveOutputFormat() {
        // Check for a format extension in the URI.
        final String extension = getRequest().getReference().getPathExtension();

        Format format = null;
        if (extension != null) {
            format = Format.all().stream()
                    .filter(f -> f.getPreferredExtension().equals(extension))
                    .findFirst()
                    .orElse(null);
        }

        if (format == null) { // if none, check the Accept header.
            String contentType = negotiateContentType(AVAILABLE_OUTPUT_MEDIA_TYPES);
            if (contentType != null) {
                format = new MediaType(contentType).toFormat();
            } else {
                format = DEFAULT_FORMAT;
            }
        }

        if (format == null) {
            format = DEFAULT_FORMAT;
        }
        return format;
    }

}
