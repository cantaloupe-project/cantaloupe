package edu.illinois.library.cantaloupe.controller.iiif.v1;

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
import edu.illinois.library.cantaloupe.http.ContentTypeNegotiator;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
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
import edu.illinois.library.cantaloupe.resource.iiif.v1.Information;
import edu.illinois.library.cantaloupe.resource.iiif.v1.InformationFactory;
import edu.illinois.library.cantaloupe.source.StatResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for IIIF Image API 1.x information requests.
 * This implementation uses real IIIF classes and integrates with the complete
 * image processing pipeline, similar to InformationResource.
 *
 * @see <a href="https://iiif.io/api/image/1.0/#22-image-information-request-url-syntax">
 *     Image Information Requests</a>
 */
@RestController
@RequestMapping("/iiif/1")
public class InformationController extends AbstractIIIFController {
    private final InformationRequestHandlerFactory handlerFactory;

    @Autowired
    public InformationController(Configuration configuration,
                                InformationRequestHandlerFactory handlerFactory) {
        super(configuration);
        this.handlerFactory = handlerFactory;
    }

    @GetMapping("/{identifier}/info.json")
    public ResponseEntity<?> getInformation(@PathVariable String identifier,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) throws Exception {

        checkEndpointEnabled();
        List<String> pathArguments = Arrays.asList(identifier);
        IIIFRequest iiifrequest = new IIIFRequest(request, pathArguments, configuration);

        // 6.2: http://iiif.io/api/image/1.1/#server-responses-error
        if (iiifrequest.getReference().toString().length() > 1024) {
            throw new ResourceException(Status.URI_TOO_LONG);
        }

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
 
                setContentTypeAndLastModified(request, headers, info);

                // Create the IIIF Information response
                Information iiifInfo = createInformation(info, availableOutputFormats,  iiifrequest);
                headers.add("Link", String.format("<%s>;rel=\"profile\";", iiifInfo.profile));
                return new ResponseEntity<Information>(iiifInfo, headers, HttpStatus.OK);
            } catch (ResourceException e) {
                if (e.getStatus().getCode() < 500) {
                    Map<String,Object> errorInfo = createErrorInformation(e, iiifrequest);
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

    private String getNegotiatedContentType(HttpServletRequest request) {
        ContentTypeNegotiator negotiator = new ContentTypeNegotiator(getHeaders(request));
        List<String> preferences =  negotiator.getPreferredMediaTypes();
        String mediaType = "";
        if (!preferences.isEmpty() && preferences.get(0)
                .startsWith("application/ld+json")) {
            mediaType = "application/ld+json";
        } else {
            mediaType = "application/json";
        }
        return mediaType + ";charset=UTF-8";
    }


    /**
     * Creates a real IIIF Information object using the InformationFactory.
     */
    private Information createInformation(Info info,
                                          Set<Format> availableOutputFormats,
                                          IIIFRequest iiifrequest) throws Exception {

        final InformationFactory factory = new InformationFactory();

        // TODO which one should we use in every info controller: iiifrequest.getPageIndex vs getPageIndex(iiifrequest)

        return factory.newImageInfo(
                        getImageURI(iiifrequest),
                        availableOutputFormats,
                        info,
                        iiifrequest.getPageIndex(),
                        iiifrequest.getMetaIdentifier().getScaleConstraint());
    }


    /**
     * Creates an error Information object for 4xx responses.
     */
    private Map<String,Object> createErrorInformation(ResourceException exception,
                                                      IIIFRequest iiifrequest) throws Exception {
        final Map<String,Object> map = new LinkedHashMap<>(); // preserves key order

        map.put("@context", "http://library.stanford.edu/iiif/image-api/1.1/context.json");
        map.put("@id", getImageURI(iiifrequest));
        map.put("status", exception.getStatus().getCode());
        map.put("message", exception.getMessage());
        return map;
    }

    /**
     * Builds the image URI from the request.
     */
    private String getImageURI(IIIFRequest iiifRequest) {
        return iiifRequest.getPublicRootReference() + Route.IIIF_2_PATH + "/" +
                iiifRequest.getPublicIdentifier();
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

    private void setContentTypeAndLastModified(HttpServletRequest request, HttpHeaders headers, Info info) {
        // Content-Type
        headers.add("Content-Type", getNegotiatedContentType(request));
        // Last-Modified
        if (info.getSerializationTimestamp() != null) {
            setLastModifiedHeader(headers, info.getSerializationTimestamp());
        }
    }
}
