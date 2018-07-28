package edu.illinois.library.cantaloupe.auth;

import java.io.IOException;

/**
 * Authorizes a request.
 */
public interface Authorizer {

    /**
     * @return Instance reflecting the authorization status of the request.
     */
    AuthInfo authorize() throws IOException;

}
