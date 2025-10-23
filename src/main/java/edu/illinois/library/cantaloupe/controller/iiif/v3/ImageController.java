package edu.illinois.library.cantaloupe.controller.iiif.v3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandler;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandlerFactory;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.ScaleRestrictedException;
import edu.illinois.library.cantaloupe.resource.iiif.IIIFAuth;
import edu.illinois.library.cantaloupe.resource.iiif.ImageDisposition;
import edu.illinois.library.cantaloupe.resource.iiif.ScaleValidator;
import edu.illinois.library.cantaloupe.resource.iiif.SizeConstrainer;
import edu.illinois.library.cantaloupe.resource.iiif.SizeRestrictedException;
import edu.illinois.library.cantaloupe.resource.iiif.v3.InformationFactory;
import edu.illinois.library.cantaloupe.resource.iiif.v3.Parameters;
import edu.illinois.library.cantaloupe.resource.iiif.v3.Size;
import edu.illinois.library.cantaloupe.source.StatResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for IIIF Image API 3.x image requests.
 * This implementation uses real IIIF classes and integrates with the complete
 * image processing pipeline, similar to ImageResource.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#4-image-requests">Image Requests</a>
 */
@RestController
@RequestMapping("/iiif/3")
public class ImageController extends AbstractIIIFController {

    private final ImageRequestHandlerFactory handlerFactory;

    /**
     * Map of response headers to be added to the response upon success.
     */
    private final Map<String, String> queuedHeaders = new HashMap<>();

    @Autowired
    public ImageController(Configuration configuration, ImageRequestHandlerFactory handlerFactory) {
        super(configuration);
        this.handlerFactory = handlerFactory;
    }

    @GetMapping("/{identifier}/{region}/{size}/{rotation}/{quality}.{format}")
    public void getImage(
            @PathVariable String identifier,
            @PathVariable String region,
            @PathVariable String size,
            @PathVariable String rotation,
            @PathVariable String quality,
            @PathVariable String format,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        checkEndpointEnabled();
        addCorsHeaders(response);

        // Assemble the URI path segments into a Parameters object using fromURI
        String paramsStr = String.join("/", identifier, region, size, rotation, quality + "." + format);
        final Parameters params = Parameters.fromURI(paramsStr);

        // Create an IIIFRequest from the HttpServletRequest
        List<String> pathArguments = Arrays.asList(identifier, region, size, rotation, quality + "." + format);
        IIIFRequest iiifrequest = new IIIFRequest(request, pathArguments);

        // Convert parameters into an OperationList
        final OperationList ops = params.toOperationList(
                iiifrequest.getDelegateProxy(), getMaxScale(), configuration);
        final int pageIndex = getPageIndex(iiifrequest);
        ops.setPageIndex(pageIndex);
        ops.getOptions().putAll(getQueryParams(request));

        final String disposition = ImageDisposition.getRepresentationDisposition(
                iiifrequest, ops.getMetaIdentifier().toString(), ops.getOutputFormat());

        class CustomCallback implements ImageRequestHandler.Callback {
            @Override
            public boolean preAuthorize() throws Exception {
                return IIIFAuth.preAuthorize(iiifrequest, response);
            }

            @Override
            public boolean authorize() throws Exception {
                return IIIFAuth.authorize(iiifrequest, response);
            }

            @Override
            public void sourceAccessed(StatResult result) {
                if (result.getLastModified() != null) {
                    setLastModifiedHeader(response, result.getLastModified());
                }
            }

            @Override
            public void infoAvailable(Info info) throws Exception {
                if (Size.Type.MAX.equals(params.getSize().getType())) {
                    try {
                        SizeConstrainer.constrainSizeToMaxPixels(info.getSize(), ops, configuration);
                    } catch (ValidationException e) {
                        throw new IllegalClientArgumentException(e.getMessage(), e);
                    }
                }
                try {
                    enqueueHeaders(params, info.getSize(pageIndex), disposition, request, iiifrequest);
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalClientArgumentException(e.getMessage(), e);
                }
            }

            @Override
            public void willStreamImageFromDerivativeCache() throws Exception {
                sendHeaders(response);
            }

            @Override
            public void willProcessImage(Processor processor, Info info) throws Exception {
                final Metadata metadata = info.getMetadata();
                final Orientation orientation = (metadata != null) ?
                        metadata.getOrientation() : Orientation.ROTATE_0;
                final Scale scale = (Scale) ops.getFirst(Scale.class);
                final Dimension virtualSize = orientation.adjustedSize(info.getSize(pageIndex));
                final Dimension resultingSize = ops.getResultingSize(info.getSize(pageIndex));
                validateScale(virtualSize, scale, params.getSize().isUpscalingAllowed(), iiifrequest);
                ScaleValidator.validateScale(virtualSize, scale, Status.BAD_REQUEST, iiifrequest.getMetaIdentifier());
                validateSize(virtualSize, resultingSize);

                sendHeaders(response);
            }
        }

        try (ImageRequestHandler handler = handlerFactory.create(ops, iiifrequest, new CustomCallback())) {
            handler.handle(response.getOutputStream());
        } catch (ResourceException e) {
            if (e.getStatus().getCode() < 500) {
                response.setStatus(e.getStatus().getCode());
                response.getWriter().write(createErrorResponse(e, identifier, request, iiifrequest));
            } else {
                throw e;
            }
        }
    }

    @RequestMapping(value = "/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                   method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsImage(
            @PathVariable String identifier,
            @PathVariable String region,
            @PathVariable String size,
            @PathVariable String rotation,
            @PathVariable String quality,
            @PathVariable String format,
            HttpServletResponse response) throws EndpointDisabledException {

        checkEndpointEnabled();
        addCorsHeaders(response);

        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }

    /**
     * Adds Content-Disposition, Content-Type, and Link response headers to a queue
     * which will be sent upon a success response.
     */
    private void enqueueHeaders(Parameters params,
                               Dimension fullSize,
                               String disposition,
                               HttpServletRequest request,
                               IIIFRequest iiifrequest) {
        // Content-Disposition
        if (disposition != null) {
            queuedHeaders.put("Content-Disposition", disposition);
        }

        // Content-Type
        queuedHeaders.put("Content-Type",
                params.getOutputFormat().toFormat().getPreferredMediaType().toString());

        // Link
        Parameters paramsCopy = new Parameters(params);
        paramsCopy.setIdentifier(getPublicIdentifier(iiifrequest));
        String paramsStr = paramsCopy.toCanonicalString(fullSize);
        queuedHeaders.put("Link",
                String.format("<%s%s/%s>;rel=\"canonical\"",
                        getPublicRootReference(request),
                        Route.IIIF_3_PATH,
                        paramsStr));
    }

    private void sendHeaders(HttpServletResponse response) {
        queuedHeaders.forEach(response::setHeader);
    }

    private double getMaxScale() {
        return configuration.getDouble(Key.MAX_SCALE, 1);
    }

    /**
     * Gets the page index from the IIIF request, defaulting to 0.
     */
    private int getPageIndex(IIIFRequest iiifrequest) {
        try {
            return iiifrequest.getPageIndex();
        } catch (Exception e) {
            return 0; // Default to first page
        }
    }

    /**
     * Gets query parameters from the request as a Map.
     */
    private Map<String, Object> getQueryParams(HttpServletRequest request) {
        Map<String, Object> queryParams = new HashMap<>();
        if (request.getQueryString() != null) {
            String[] pairs = request.getQueryString().split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return queryParams;
    }

    /**
     * Gets the public identifier from the request.
     */
    private String getPublicIdentifier(IIIFRequest iiifrequest) {
        try {
            return iiifrequest.getPublicIdentifier();
        } catch (Exception e) {
            return iiifrequest.getIdentifier().toString();
        }
    }

    /**
     * Gets the public root reference from the request.
     */
    private String getPublicRootReference(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder uri = new StringBuilder();
        uri.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            uri.append(":").append(serverPort);
        }
        uri.append(contextPath);
        return uri.toString();
    }

    /**
     * Ensures that the resulting scale is less than or equal to 1 if the
     * size URI path component does not begin with ^.
     */
    private void validateScale(Dimension virtualSize,
                              Scale scale,
                              boolean isUpscalingAllowed,
                              IIIFRequest iiifrequest) throws ScaleRestrictedException {
        if (!isUpscalingAllowed && scale != null) {
            final edu.illinois.library.cantaloupe.image.ScaleConstraint constraint =
                    (iiifrequest.getMetaIdentifier().getScaleConstraint() != null) ?
                            iiifrequest.getMetaIdentifier().getScaleConstraint() :
                            new edu.illinois.library.cantaloupe.image.ScaleConstraint(1, 1);
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
     * Ensures that resultingSize is valid if IIIF_RESTRICT_TO_SIZES is set to true.
     */
    private void validateSize(Dimension virtualSize,
                             Dimension resultingSize) throws SizeRestrictedException {
        if (configuration.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)) {
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

    /**
     * Creates an error response for 4xx status codes.
     */
    private String createErrorResponse(ResourceException exception,
                                     String identifier,
                                     HttpServletRequest request,
                                     IIIFRequest iiifrequest) {
        try {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("@context", "http://iiif.io/api/image/3/context.json");
            errorMap.put("id", getImageURI(identifier, request));
            errorMap.put("type", "ImageService3");
            errorMap.put("protocol", "http://iiif.io/api/image");
            errorMap.put("profile", "level2");
            errorMap.put("status", exception.getStatus().getCode());
            errorMap.put("message", exception.getMessage());

            // Add any extra keys from delegate
            try {
                errorMap.putAll(iiifrequest.getDelegateProxy().getExtraIIIF3InformationResponseKeys());
            } catch (Exception e) {
                // Log but don't fail the request
            }

            // Simple JSON serialization
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : errorMap.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof String) {
                    json.append("\"").append(entry.getValue()).append("\"");
                } else {
                    json.append(entry.getValue());
                }
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            return "{\"error\":\"Internal server error\"}";
        }
    }

    /**
     * Builds the image URI from the request.
     */
    private String getImageURI(String identifier, HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder uri = new StringBuilder();
        uri.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            uri.append(":").append(serverPort);
        }
        uri.append(contextPath).append("/iiif/3/").append(identifier);
        return uri.toString();
    }

    private void setLastModifiedHeader(HttpServletResponse response, java.time.Instant timestamp) {
        response.setDateHeader("Last-Modified", timestamp.toEpochMilli());
    }
}
