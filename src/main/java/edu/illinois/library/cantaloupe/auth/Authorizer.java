package edu.illinois.library.cantaloupe.auth;

import java.io.IOException;

/**
 * Authorizes a request.
 */
public interface Authorizer {

    /**
     * Authorizes a request early in the request cycle, before any information
     * is available about the source image.
     *
     * @return Instance reflecting the authorization status of the request.
     */
    AuthInfo preAuthorize() throws IOException;

    /**
     * Authorizes a request after full information is available about the
     * source image.
     *
     * @return Instance reflecting the authorization status of the request.
     */
    AuthInfo authorize() throws IOException;

}
