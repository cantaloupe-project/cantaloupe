package edu.illinois.library.cantaloupe.auth;

public class AuthInfo {

    private boolean isAuthorized;

    /**
     * Creates an instance that is either authorized or not.
     */
    public AuthInfo(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

}
