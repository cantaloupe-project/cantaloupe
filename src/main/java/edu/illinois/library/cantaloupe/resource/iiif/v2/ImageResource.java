package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandler;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles image requests.
 *
 * @see <a href="http://iiif.io/api/image/2.1/#image-request-parameters">Image
 * Request Operations</a>
 */
public class ImageResource extends IIIF2Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageResource.class);

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
     * Responds to image requests.
     */
    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }

        // Assemble the URI path segments into a Parameters object.
        final List<String> args = getPathArguments();
        final Parameters params = new Parameters(
                getIdentifier().toString(), args.get(1), args.get(2),
                args.get(3), args.get(4), args.get(5));
        // Convert it into an OperationList.
        final OperationList ops = params.toOperationList(getDelegateProxy());
        ops.setPageIndex(getPageIndex());
        ops.getOptions().putAll(getRequest().getReference().getQuery().toMap());
        final int pageIndex = getPageIndex();
        final String disposition = getRepresentationDisposition(
                ops.getMetaIdentifier().toString(), ops.getOutputFormat());

        class CustomCallback implements ImageRequestHandler.Callback {
            @Override
            public void willStreamImageFromDerivativeCache() {
                addHeaders(disposition,
                        params.getOutputFormat().toFormat()
                                .getPreferredMediaType().toString());
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
            public void infoAvailable(Info info) {
                if (Size.ScaleMode.MAX.equals(params.getSize().getScaleMode())) {
                    constrainSizeToMaxPixels(info.getSize(), ops);
                }
            }

            @Override
            public void willProcessImage(Processor processor,
                                         Info info) throws Exception {
                final Metadata metadata       = info.getMetadata();
                final Orientation orientation = (metadata != null) ?
                        metadata.getOrientation() : Orientation.ROTATE_0;
                final Dimension virtualSize   = orientation.adjustedSize(info.getSize(pageIndex));
                final Dimension resultingSize = ops.getResultingSize(info.getSize());
                validateScale(
                        virtualSize,
                        (Scale) ops.getFirst(Scale.class),
                        Status.FORBIDDEN);
                validateSize(resultingSize, virtualSize);

                addHeaders(params,
                        info.getSize(pageIndex),
                        disposition,
                        params.getOutputFormat().toFormat().getPreferredMediaType().toString());
            }
        }

        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withOperationList(ops)
                .withBypassingCache(isBypassingCache())
                .withBypassingCacheRead(isBypassingCacheRead())
                .optionallyWithDelegateProxy(getDelegateProxy(), getRequestContext())
                .withCallback(new CustomCallback())
                .build()) {
            handler.handle(getResponse().getOutputStream());
        }
    }

    /**
     * Adds {@code Content-Disposition} and {@code Content-Type} response
     * headers.
     */
    private void addHeaders(String disposition,
                            String contentType) {
        // Content-Disposition
        if (disposition != null) {
            getResponse().setHeader("Content-Disposition", disposition);
        }
        // Content-Type
        getResponse().setHeader("Content-Type", contentType);
    }

    /**
     * Invokes {@link #addHeaders(String, String)} and also adds a {@code Link}
     * header.
     */
    private void addHeaders(Parameters params,
                            Dimension fullSize,
                            String disposition,
                            String contentType) {
        addHeaders(disposition, contentType);

        Parameters paramsCopy = new Parameters(params);
        paramsCopy.setIdentifier(getPublicIdentifier());
        String paramsStr = paramsCopy.toCanonicalString(fullSize);
        getResponse().setHeader("Link",
                String.format("<%s%s/%s>;rel=\"canonical\"",
                        getPublicRootReference(),
                        Route.IIIF_2_PATH,
                        paramsStr));
    }

    private void validateSize(Dimension resultingSize,
                              Dimension virtualSize) throws SizeRestrictedException {
        final var config = Configuration.getInstance();
        if (config.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)) {
            new InformationFactory().getSizes(virtualSize)
                    .stream()
                    .filter(s -> s.width == resultingSize.intWidth() &&
                            s.height == resultingSize.intHeight())
                    .findAny()
                    .orElseThrow(() -> new SizeRestrictedException(
                            "Available sizes are limited to those in the information response."));
        }
    }

}
