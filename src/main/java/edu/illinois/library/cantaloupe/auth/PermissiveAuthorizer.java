package edu.illinois.library.cantaloupe.auth;

/**
 * Authorizes all requests.
 */
final class PermissiveAuthorizer implements Authorizer {

    @Override
    public AuthInfo authorize() {
        return new AuthInfo.BooleanBuilder(true).build();
    }

    @Override
    public AuthInfo preAuthorize() {
        return new AuthInfo.BooleanBuilder(true).build();
    }

}
