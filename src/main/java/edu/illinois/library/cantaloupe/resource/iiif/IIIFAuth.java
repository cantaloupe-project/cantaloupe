package edu.illinois.library.cantaloupe.resource.iiif;

import java.io.IOException;

import edu.illinois.library.cantaloupe.auth.AuthInfo;
import edu.illinois.library.cantaloupe.auth.Authorizer;
import edu.illinois.library.cantaloupe.auth.AuthorizerFactory;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.StringRepresentation;
import jakarta.servlet.http.HttpServletResponse;

public class IIIFAuth {
    
    /**
     * <p>Uses an {@link Authorizer} to determine how to respond to the
     * request. The response is modified if necessary.</p>
     *
     * <p>The authorization system (rooted in the {@link
     * edu.illinois.library.cantaloupe.delegate.DelegateMethod#AUTHORIZE
     * authorization delegate method} supports simple boolean authorization
     * which maps to the HTTP 200 and 403 statuses.</p>
     *
     * <p>Authorization can simultaneously be used in the context of the
     * <a href="https://iiif.io/api/auth/1.0/">IIIF Authentication API, where
     * it works a little differently. Here, HTTP 401 is returned instead of
     * 403, and the response body <strong>does</strong> include image
     * information. (See
     * <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">
     * Interaction with Access-Controlled Resources</a>. This means that IIIF
     * information endpoints should swallow any {@link ResourceException}s with
     * HTTP 401 status.</p>
     *
     * @return Whether authorization was successful. {@code false} indicates a
     *         redirect, and client code should abort.
     * @throws IOException if there was an I/O error while checking
     *         authorization.
     * @throws ResourceException if authorization resulted in an HTTP 400-level
     *         response.
     */
    public final static boolean authorize(IIIFRequest request, HttpServletResponse response) throws IOException, ResourceException {
        final Authorizer authorizer =
                new AuthorizerFactory().newAuthorizer(request.getDelegateProxy());
        final AuthInfo info = authorizer.authorize();
        if (info != null) {
            return processAuthInfo(info, request, response);
        }
        return true;
    }

    /**
     * <p>Uses an {@link Authorizer} to determine how to respond to the
     * request. The response is modified if necessary.</p>
     *
     * <p>The authorization system (rooted in the {@link
     * edu.illinois.library.cantaloupe.delegate.DelegateMethod#AUTHORIZE
     * authorization delegate method} supports simple boolean authorization
     * which maps to the HTTP 200 and 403 statuses. In the event of a 403,
     * IIIF image information should not be included in the response body.</p>
     *
     * <p>Authorization can simultaneously be used in the context of the
     * <a href="https://iiif.io/api/auth/1.0/">IIIF Authentication API, where
     * it works a little differently. Here, HTTP 401 is returned instead of
     * 403, and the response body <strong>does</strong> include image
     * information. (See
     * <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">
     * Interaction with Access-Controlled Resources</a>. This means that IIIF
     * information endpoints should swallow any {@link ResourceException}s with
     * HTTP 401 status.</p>
     *
     * @return Whether authorization was successful. {@code false} indicates a
     *         redirect, and client code should abort.
     * @throws IOException if there was an I/O error while checking
     *         authorization.
     * @throws ResourceException if authorization resulted in an HTTP 400-level
     *         response.
     */
    public static final boolean preAuthorize(IIIFRequest request, HttpServletResponse response) throws IOException, ResourceException {
        final Authorizer authorizer =
                new AuthorizerFactory().newAuthorizer(request.getDelegateProxy());
        final AuthInfo info = authorizer.preAuthorize();
        if (info != null) {
            return processAuthInfo(info, request, response);
        }
        return true;
    }

    private static boolean processAuthInfo(AuthInfo info, IIIFRequest request, HttpServletResponse response)
            throws IOException, ResourceException {
        final int code                      = info.getResponseStatus();
        final String location               = info.getRedirectURI();
        if (location != null) {
            response.setStatus(code);
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Location", location);
            new StringRepresentation("Redirect: " + location)
                    .write(response.getOutputStream());
            return false;
        }

        final MetaIdentifier metaIdentifier = new MetaIdentifier(request.getMetaIdentifier());
        metaIdentifier.setScaleConstraint(info.getScaleConstraint());

        if (metaIdentifier.getScaleConstraint() != null) {
            Reference publicRef = request.getPublicReference(metaIdentifier,
                                                             request.getIdentifierPathComponent(),
                                                             request.getDelegateProxy());
            response.setStatus(code);
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Location", publicRef.toString());
            new StringRepresentation("Redirect: " + publicRef)
                    .write(response.getOutputStream());
            return false;
        } else if (code >= 400) {
            response.setStatus(code);
            response.setHeader("Cache-Control", "no-cache");
            if (code == 401) {
                response.setHeader("WWW-Authenticate",
                        info.getChallengeValue());
            }
            throw new ResourceException(new Status(code));
        }
        return true;
    }
}
