package edu.illinois.library.cantaloupe.resource.iiif.v3;

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
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandler;
import edu.illinois.library.cantaloupe.source.StatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles IIIF Image API 3.x information requests.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#51-image-information-request">
 *     Image Information Requests</a>
 */
public class InformationResource extends IIIF3Resource {

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
        // TODO: we are supposed to get the available output formats from the
        // processor, but the control flow may not lead to a processor ever
        // being obtained.
        final Set<Format> availableOutputFormats =
                new HashSet<>(ImageWriterFactory.supportedFormats());

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
                addHeaders(info);
                newRepresentation(info, availableOutputFormats)
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

    private void addHeaders(Info info) {
        // Content-Type
        getResponse().setHeader("Content-Type", getNegotiatedContentType());
        // Last-Modified
        if (info.getSerializationTimestamp() != null) {
            setLastModifiedHeader(info.getSerializationTimestamp());
        }
    }

    /**
     * @return Full image URI corresponding to the given identifier, respecting
     *         the {@literal X-Forwarded-*} and
     *         {@link #PUBLIC_IDENTIFIER_HEADER} reverse proxy headers.
     */
    private String getImageURI() {
        return getPublicRootReference() + Route.IIIF_3_PATH + "/" +
                getPublicIdentifier();
    }

    private String getNegotiatedContentType() {
        String contentType;
        // If the client has requested JSON, set the content type to
        // that; otherwise set it to JSON-LD.
        final List<String> preferences = getPreferredMediaTypes();
        if (!preferences.isEmpty() && preferences.get(0)
                .startsWith("application/json")) {
            contentType = "application/json";
        } else {
            contentType = "application/ld+json";
        }
        contentType += ";charset=UTF-8";
        contentType += ";profile=\"http://iiif.io/api/image/3/context.json\"";
        return contentType;
    }

    private JacksonRepresentation newRepresentation(Info info,
                                                    Set<Format> availableOutputFormats) {
        final InformationFactory factory = new InformationFactory();
        factory.setDelegateProxy(getDelegateProxy());

        final Information<String, Object> iiifInfo = factory.newImageInfo(
                availableOutputFormats,
                getImageURI(),
                info,
                getPageIndex(),
                getMetaIdentifier().getScaleConstraint());
        return new JacksonRepresentation(iiifInfo);
    }

    private JacksonRepresentation newHTTP4xxRepresentation(
            Status status,
            String message) throws ScriptException {
        final Map<String,Object> map = new LinkedHashMap<>(); // preserves key order
        map.put("@context", "http://iiif.io/api/image/3/context.json");
        map.put("id", getImageURI());
        map.put("type", "ImageService3");
        map.put("protocol", "http://iiif.io/api/image");
        map.put("profile", "level2");
        map.put("status", status.getCode());
        map.put("message", message);
        map.putAll(getDelegateProxy().getExtraIIIF3InformationResponseKeys());
        return new JacksonRepresentation(map);
    }

}
