package edu.illinois.library.cantaloupe.resource.iiif.v3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Writes a JSON-serialized {@link ImageInfo} instance to the response.
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
                try {
                    // The logic here is somewhat convoluted. See the method
                    // documentation for more information.
                    return InformationResource.this.preAuthorize();
                } catch (ResourceException e) {
                    if (Status.FORBIDDEN.equals(e.getStatus())) {
                        throw e;
                    }
                }
                return false;
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
            Info info = handler.handle();
            addHeaders();
            newRepresentation(info, availableOutputFormats)
                    .write(getResponse().getOutputStream());
        }
    }

    private void addHeaders() {
        getResponse().setHeader("Content-Type", getNegotiatedContentType());
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
        final ImageInfoFactory factory = new ImageInfoFactory();
        factory.setDelegateProxy(getDelegateProxy());

        final ImageInfo<String, Object> imageInfo = factory.newImageInfo(
                availableOutputFormats,
                getImageURI(),
                info,
                getPageIndex(),
                getMetaIdentifier().getScaleConstraint());
        return new JacksonRepresentation(imageInfo);
    }

}
