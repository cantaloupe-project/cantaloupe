package edu.illinois.library.cantaloupe.controller.iiif.v3;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandler;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandlerFactory;
import edu.illinois.library.cantaloupe.resource.RequestContextDecorator;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.iiif.IIIFAuth;
import edu.illinois.library.cantaloupe.resource.iiif.v3.Information;
import edu.illinois.library.cantaloupe.resource.iiif.v3.InformationFactory;
import edu.illinois.library.cantaloupe.source.StatResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for IIIF Image API 3.x information requests.
 * This implementation uses real IIIF classes and integrates with the complete
 * image processing pipeline, similar to InformationResource.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#51-image-information-request">
 *     Image Information Requests</a>
 */
@RestController("v3InformationController")
@RequestMapping("/iiif/3")
public class InformationController extends AbstractIIIFController {
    private final InformationRequestHandlerFactory handlerFactory;

    @Autowired
    public InformationController(Configuration configuration,
                                InformationRequestHandlerFactory handlerFactory) {
        super(configuration);
        this.handlerFactory = handlerFactory;
    }

    @GetMapping("/{identifier}/info.json")
    public ResponseEntity<Information<String, Object>> getInformation(@PathVariable String identifier,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) throws Exception {

        checkEndpointEnabled();
        List<String> pathArguments = Arrays.asList(identifier);
        IIIFRequest iiifrequest = new IIIFRequest(request, pathArguments, configuration);

        MetaIdentifier newMetaId = iiifrequest.getMetaIdentifier().getNormalizedScaleConstraintMetaIdentifier();
        if (newMetaId != null) { // We need to redirect to the normalized scale constraint
            Reference newRef = iiifrequest.getPublicReference(newMetaId,
                                                              iiifrequest.getIdentifierPathComponent(),
                                                              iiifrequest.getDelegateProxy());
            response.sendRedirect(newRef.toString());
            return null;
        }

        HttpHeaders headers = new HttpHeaders();

        addCorsHeaders(response);

        RequestContextDecorator.decorateRequestContext(iiifrequest);

        // Get the available output formats from the processor
        final Set<Format> availableOutputFormats =
                new HashSet<>(ImageWriterFactory.supportedFormats());

        class CustomCallback implements InformationRequestHandler.Callback {
            @Override
            public boolean authorize() throws Exception {
                return IIIFAuth.preAuthorize(iiifrequest, response);
            }

            @Override
            public void sourceAccessed(StatResult result) {
                if (result.getLastModified() != null) {
                    setLastModifiedHeader(headers, result.getLastModified());
                }
            }

            @Override
            public void knowAvailableOutputFormats(Set<Format> formats) {
                availableOutputFormats.addAll(formats);
            }
        }


        try (InformationRequestHandler handler = handlerFactory.create(
                iiifrequest,
                new CustomCallback())) {
            try {
                Info info = handler.handle();
                if (info == null) {
                    return null; // Auth failure.
                }
                addHeaders(response, iiifrequest);

                setContentTypeAndLastModified(headers, info);

                // Create the IIIF Information response
                Information<String, Object> iiifInfo = createInformation(info, availableOutputFormats,  iiifrequest);

                return new ResponseEntity<Information<String, Object>>(iiifInfo, headers, HttpStatus.OK);
            } catch (ResourceException e) {
                if (e.getStatus().getCode() < 500) {
                    Information<String, Object> errorInfo = createErrorInformation(
                            e, identifier, request, iiifrequest);
                    return ResponseEntity.status(e.getStatus().getCode()).body(errorInfo);
                } else {
                    throw e;
                }
            }
        }
    }

    @RequestMapping(value = "/{identifier}/info.json", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsInformation(@PathVariable String identifier,
                                                   HttpServletResponse response) throws EndpointDisabledException {
        checkEndpointEnabled();
        addCorsHeaders(response);

        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }

    private String getNegotiatedContentType() {
        String contentType = "application/ld+json"; // Default to JSON-LD
        contentType += ";charset=UTF-8";
        contentType += ";profile=\"http://iiif.io/api/image/3/context.json\"";
        return contentType;
    }

    /**
     * Creates a real IIIF Information object using the InformationFactory.
     */
    private Information<String, Object> createInformation(Info info,
                                                          Set<Format> availableOutputFormats,
                                                          IIIFRequest iiifrequest) throws Exception {
        final InformationFactory factory = new InformationFactory();
        factory.setDelegateProxy(iiifrequest.getDelegateProxy());

        final String imageURI = getImageURI(iiifrequest);
        final int pageIndex = getPageIndex(iiifrequest);

        return factory.newImageInfo(
                availableOutputFormats,
                imageURI,
                info,
                pageIndex,
                iiifrequest.getMetaIdentifier().getScaleConstraint());
    }


    /**
     * Creates an error Information object for 4xx responses.
     */
    private Information<String, Object> createErrorInformation(ResourceException exception,
                                                               String identifier,
                                                               HttpServletRequest request,
                                                               IIIFRequest iiifrequest) throws Exception {
        final Map<String,Object> map = new LinkedHashMap<>(); // preserves key order
        map.put("@context", "http://iiif.io/api/image/3/context.json");
        map.put("id", getImageURI(iiifrequest));
        map.put("type", "ImageService3");
        map.put("protocol", "http://iiif.io/api/image");
        map.put("profile", "level2");
        map.put("status", exception.getStatus().getCode());
        map.put("message", exception.getMessage());

        // Add any extra keys from delegate
        try {
            map.putAll(iiifrequest.getDelegateProxy().getExtraIIIF3InformationResponseKeys());
        } catch (Exception e) {
            // Log but don't fail the request
        }

        Information<String, Object> errorInfo = new Information<>();
        errorInfo.putAll(map);
        return errorInfo;
    }

    /**
     * Builds the image URI from the request.
     */
    private String getImageURI(IIIFRequest iiifRequest) {
        return iiifRequest.getPublicRootReference() + Route.IIIF_3_PATH + "/" +
                iiifRequest.getPublicIdentifier();

        // String scheme = request.getScheme();
        // String serverName = request.getServerName();
        // int serverPort = request.getServerPort();
        // String contextPath = request.getContextPath();

        // StringBuilder uri = new StringBuilder();
        // uri.append(scheme).append("://").append(serverName);
        // if (serverPort != 80 && serverPort != 443) {
        //     uri.append(":").append(serverPort);
        // }
        // uri.append(contextPath).append("/iiif/3/").append(identifier);
        // return uri.toString();
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

    private void setLastModifiedHeader(HttpHeaders headers, java.time.Instant timestamp) {
                // Format the instant to RFC 1123 date-time format
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                .withLocale(Locale.UK)
                .withZone(ZoneOffset.UTC);

        String formattedDate = formatter.format(timestamp);
        headers.add("Last-Modified", formattedDate);
    }

    private void setContentTypeAndLastModified(HttpHeaders headers, Info info) {
        // Content-Type
        headers.add("Content-Type", getNegotiatedContentType());
        // Last-Modified
        if (info.getSerializationTimestamp() != null) {
            setLastModifiedHeader(headers, info.getSerializationTimestamp());
        }
    }
}
