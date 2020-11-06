package edu.illinois.library.cantaloupe.resource.iiif.v2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles IIIF Image API 2.x information requests.
 *
 * @see <a href="http://iiif.io/api/image/2.1/#information-request">Information
 *      Requests</a>
 */
public class InformationResource extends IIIF2Resource {

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
        final Set<Format> availableOutputFormats = new HashSet<>();

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
        getResponse().setHeader("Content-Type", getNegotiatedMediaType());
    }

    /**
     * @return Image URI corresponding to the given identifier, respecting the
     *         {@code X-Forwarded-*} and {@link #PUBLIC_IDENTIFIER_HEADER}
     *         reverse proxy headers.
     */
    private String getImageURI() {
        return getPublicRootReference() + Route.IIIF_2_PATH + "/" +
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
