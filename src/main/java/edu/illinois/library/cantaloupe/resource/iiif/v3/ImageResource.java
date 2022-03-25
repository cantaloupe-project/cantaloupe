package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.ScaleRestrictedException;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandler;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import edu.illinois.library.cantaloupe.source.StatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles image requests.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#4-image-requests">Image
 * Requests</a>
 */
public class ImageResource extends IIIF3Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    /**
     * Map of response headers to be added to the response upon success.
     */
    private final Map<String,String> queuedHeaders = new HashMap<>();

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
        final OperationList ops = params.toOperationList(
                getDelegateProxy(), getMaxScale());
        ops.setPageIndex(getPageIndex());
        ops.getOptions().putAll(getRequest().getReference().getQuery().toMap());
        final int pageIndex = getPageIndex();
        final String disposition = getRepresentationDisposition(
                ops.getMetaIdentifier().toString(), ops.getOutputFormat());

        class CustomCallback implements ImageRequestHandler.Callback {
            @Override
            public boolean preAuthorize() throws Exception {
                return ImageResource.this.preAuthorize();
            }

            @Override
            public boolean authorize() throws Exception {
                return ImageResource.this.authorize();
            }

            @Override
            public void sourceAccessed(StatResult result) {
                if (result.getLastModified() != null) {
                    setLastModifiedHeader(result.getLastModified());
                }
            }

            @Override
            public void infoAvailable(Info info) {
                if (Size.Type.MAX.equals(params.getSize().getType())) {
                    constrainSizeToMaxPixels(info.getSize(), ops);
                }
                try {
                    enqueueHeaders(params, info.getSize(pageIndex), disposition);
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalClientArgumentException(e);
                }
            }

            @Override
            public void willStreamImageFromDerivativeCache() {
                sendHeaders();
            }

            @Override
            public void willProcessImage(Processor processor,
                                         Info info) throws Exception {
                final Metadata metadata       = info.getMetadata();
                final Orientation orientation = (metadata != null) ?
                        metadata.getOrientation() : Orientation.ROTATE_0;
                final Scale scale             = (Scale) ops.getFirst(Scale.class);
                final Dimension virtualSize   = orientation.adjustedSize(info.getSize(pageIndex));
                final Dimension resultingSize = ops.getResultingSize(info.getSize());
                validateScale(virtualSize, scale, params.getSize().isUpscalingAllowed());
                validateScale(virtualSize, scale, Status.BAD_REQUEST);
                validateSize(virtualSize, resultingSize);
                sendHeaders();
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
     * Adds {@code Content-Disposition}, {@code Content-Type}, and {@code Link}
     * response headers to a queue which will be sent upon a success response.
     */
    private void enqueueHeaders(Parameters params,
                                Dimension fullSize,
                                String disposition) {
        // Content-Disposition
        if (disposition != null) {
            queuedHeaders.put("Content-Disposition", disposition);
        }
        // Content-Type
        queuedHeaders.put("Content-Type",
                params.getOutputFormat().toFormat().getPreferredMediaType().toString());
        // Link
        Parameters paramsCopy = new Parameters(params);
        paramsCopy.setIdentifier(getPublicIdentifier());
        String paramsStr = paramsCopy.toCanonicalString(fullSize);
        queuedHeaders.put("Link",
                String.format("<%s%s/%s>;rel=\"canonical\"",
                        getPublicRootReference(),
                        Route.IIIF_3_PATH,
                        paramsStr));
    }

    private static double getMaxScale() {
        return Configuration.getInstance().getDouble(Key.MAX_SCALE, 1);
    }

    private void sendHeaders() {
        queuedHeaders.forEach((k, v) -> getResponse().setHeader(k, v));
    }

    /**
     * Ensures that the resulting scale is less than or equal to 1 if the
     * {@code size} URI path component does not begin with {@code ^}.
     *
     * @param virtualSize        Source image size post-rotation and post-scale
     *                           constraint.
     * @param scale              May be {@code null}.
     * @param isUpscalingAllowed Whether the {@code size} URI path component
     *                           begins with {@code ^}.
     */
    private void validateScale(Dimension virtualSize,
                               Scale scale,
                               boolean isUpscalingAllowed) throws ScaleRestrictedException {
        if (!isUpscalingAllowed && scale != null) {
            final ScaleConstraint constraint =
                    (getMetaIdentifier().getScaleConstraint() != null) ?
                            getMetaIdentifier().getScaleConstraint() :
                            new ScaleConstraint(1, 1);
            if (scale.isWidthUp(virtualSize, constraint) ||
                    scale.isHeightUp(virtualSize, constraint)) {
                throw new ScaleRestrictedException("Requests for scales in " +
                        "excess of 100% must prefix the size path component " +
                        "with a ^ character.",
                        Status.BAD_REQUEST);
            }
        }
    }

    /**
     * Ensures that {@code resultingSize} is valid if {@link
     * Key#IIIF_RESTRICT_TO_SIZES} is set to {@code true}.
     *
     * @param virtualSize   Source image size post-rotation and post-scale
     *                      constraint.
     * @param resultingSize Requested size.
     */
    private void validateSize(Dimension virtualSize,
                              Dimension resultingSize) throws SizeRestrictedException {
        final Configuration config = Configuration.getInstance();
        if (config.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)) {
            new InformationFactory().getSizes(virtualSize)
                    .stream()
                    .filter(s -> s.width == resultingSize.intWidth() &&
                            s.height == resultingSize.intHeight())
                    .findAny()
                    .orElseThrow(() -> new SizeRestrictedException(
                            "Available sizes are limited to those listed in " +
                                    "the information response."));
        }
    }

}
