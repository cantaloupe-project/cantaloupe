package edu.illinois.library.cantaloupe.controller.iiif.v1;

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
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandler;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandlerFactory;
import edu.illinois.library.cantaloupe.resource.RequestContextDecorator;
import edu.illinois.library.cantaloupe.resource.iiif.IIIFAuth;
import edu.illinois.library.cantaloupe.resource.iiif.ImageDisposition;
import edu.illinois.library.cantaloupe.resource.iiif.ScaleValidator;
import edu.illinois.library.cantaloupe.resource.iiif.v1.ComplianceLevel;
import edu.illinois.library.cantaloupe.resource.iiif.v1.Parameters;
import edu.illinois.library.cantaloupe.source.StatResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for IIIF Image API 1.x image requests.
 * This implementation uses real IIIF classes and integrates with the complete
 * image processing pipeline, similar to ImageResource.
 *
 * @see <a href="https://iiif.io/api/image/1.0/#21-image-request-url-syntax">Image Requests</a>
 */
@RestController
@RequestMapping("/iiif/1")
public class ImageController extends AbstractIIIFController {

    private final ImageRequestHandlerFactory handlerFactory;

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

        // Create an IIIFRequest from the HttpServletRequest
        List<String> pathArguments = Arrays.asList(identifier, region, size, rotation, quality, format);
        IIIFRequest iiifrequest = new IIIFRequest(request, pathArguments, configuration);
        if (redirectToNormalizedScaleConstraint(iiifrequest, response)) {
            return;
        }
        RequestContextDecorator.decorateRequestContext(iiifrequest);
        addHeaders(response, iiifrequest);

        final Parameters params = new Parameters(
                iiifrequest.getIdentifier().toString(), region, size, rotation, quality, format);


        // Convert parameters into an OperationList
        final OperationList ops = params.toOperationList(iiifrequest.getDelegateProxy());
        final int pageIndex = getPageIndex(iiifrequest);
        ops.setPageIndex(pageIndex);
        ops.getOptions().putAll(getQueryParams(request));

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
            public void infoAvailable(Info info) {
            }

            @Override
            public void willStreamImageFromDerivativeCache() throws Exception {
                throw new RuntimeException("This method is not supposed to get called");
            }

            @Override
            public void willProcessImage(Processor processor, Info info) throws Exception {
                final Metadata metadata = info.getMetadata();

                final Dimension fullSize = info.getSize(iiifrequest.getPageIndex());
                ScaleValidator.validateScale(configuration, metadata.getOrientation().adjustedSize(fullSize),
                                             (Scale) ops.getFirst(Scale.class),
                                             Status.FORBIDDEN,
                                             iiifrequest.getMetaIdentifier());
                final String disposition = ImageDisposition.getRepresentationDisposition(
                        iiifrequest, ops.getMetaIdentifier().toString(), ops.getOutputFormat());

                if (disposition != null) {
                    response.setHeader("Content-Disposition", disposition);
                }
                response.setHeader("Content-Type", ops.getOutputFormat().getPreferredMediaType().toString());

                final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(processor.getAvailableOutputFormats());
                response.setHeader("Link", String.format("<%s>;rel=\"profile\";", complianceLevel.getUri()));
            }
        }

        try (ImageRequestHandler handler = handlerFactory.create(ops, iiifrequest, new CustomCallback())) {
            handler.handle(response.getOutputStream());
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

    private void setLastModifiedHeader(HttpServletResponse response, java.time.Instant timestamp) {
        response.setDateHeader("Last-Modified", timestamp.toEpochMilli());
    }
}
