package edu.illinois.library.cantaloupe.resource.iiif.v1;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandler;
import edu.illinois.library.cantaloupe.source.StatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles IIIF Image API 1.x information requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">Information
 * Requests</a>
 */
public class InformationResource extends IIIF1Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InformationResource.class);

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

    @Override
    protected final void doOPTIONS() {
        HttpServletResponse response = getResponse();
        Method[] methods = getSupportedMethods();
        if (methods.length > 0) {
            response.setStatus(Status.NO_CONTENT.getCode());
            response.setHeader("Access-Control-Allow-Headers", "Authorization");
            response.setHeader("Allow", Arrays.stream(methods)
                    .map(Method::toString)
                    .collect(Collectors.joining(",")));
        } else {
            response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
        }
    }

    /**
     * Writes a JSON-serialized {@link Information} instance to the response.
     */
    @Override
    public void doGET() throws Exception {
        if (redirectToNormalizedScaleConstraint()) {
            return;
        }
        final Set<Format> availableOutputFormats = new HashSet<>();

        class CustomCallback implements InformationRequestHandler.Callback {
            @Override
            public boolean authorize() throws Exception {
                return InformationResource.this.preAuthorize();
            }

            @Override
            public void sourceAccessed(StatResult result) {
                if (result.getLastModified() != null) {
                    setLastModifiedHeader(result.getLastModified());
                }
            }

            @Override
            public void knowAvailableOutputFormats(Set<Format> formats) {
                availableOutputFormats.addAll(formats);
            }
        }

        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withIdentifier(getMetaIdentifier().getIdentifier())
                .withBypassingCache(isBypassingCache())
                .withBypassingCacheRead(isBypassingCacheRead())
                .withDelegateProxy(getDelegateProxy())
                .withRequestContext(getRequestContext())
                .withCallback(new CustomCallback())
                .build()) {
            try {
                Info info = handler.handle();
                Information iiifInfo = new InformationFactory().newImageInfo(
                        getImageURI(),
                        availableOutputFormats,
                        info,
                        getPageIndex(),
                        getMetaIdentifier().getScaleConstraint());
                addHeaders(info, iiifInfo);
                new JacksonRepresentation(iiifInfo)
                        .write(getResponse().getOutputStream());
            } catch (ResourceException e) {
                if (e.getStatus().getCode() < 500) {
                    newHTTP4xxRepresentation(e.getStatus(), e.getMessage())
                            .write(getResponse().getOutputStream());
                } else {
                    throw e;
                }
            }
        }
    }

    private void addHeaders(Info info, Information iiifInfo) {
        // Content-Type
        getResponse().setHeader("Content-Type", getNegotiatedMediaType());
        // Link
        getResponse().setHeader("Link",
                String.format("<%s>;rel=\"profile\";", iiifInfo.profile));
        // Last-Modified
        if (info.getSerializationTimestamp() != null) {
            setLastModifiedHeader(info.getSerializationTimestamp());
        }
    }

    /**
     * @return Image URI corresponding to the given identifier, respecting the
     *         {@code X-Forwarded-*} and {@link #PUBLIC_IDENTIFIER_HEADER}
     *         reverse proxy headers.
     */
    private String getImageURI() {
        return getPublicRootReference() + Route.IIIF_1_PATH + "/" +
                getPublicIdentifier();
    }

    private String getNegotiatedMediaType() {
        String mediaType;
        // If the client has requested JSON-LD, set the content type to
        // that; otherwise set it to JSON.
        final List<String> preferences = getPreferredMediaTypes();
        if (!preferences.isEmpty() && preferences.get(0)
                .startsWith("application/ld+json")) {
            mediaType = "application/ld+json";
        } else {
            mediaType = "application/json";
        }
        return mediaType + ";charset=UTF-8";
    }

    private JacksonRepresentation newHTTP4xxRepresentation(Status status,
                                                           String message) {
        final Map<String, Object> map = new LinkedHashMap<>(); // preserves key order
        map.put("@context", "http://library.stanford.edu/iiif/image-api/1.1/context.json");
        map.put("@id", getImageURI());
        map.put("status", status.getCode());
        map.put("message", message);
        return new JacksonRepresentation(map);
    }

}
