package edu.illinois.library.cantaloupe.auth;

import java.net.URL;

public class AuthInfo {

    private boolean isAuthorized = false;
    private Integer redirectStatus;
    private URL redirectURI;

    AuthInfo(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
    }

    AuthInfo(boolean isAuthorized, URL redirectURI, int redirectStatus) {
        this(isAuthorized);
        this.redirectURI = redirectURI;
        this.redirectStatus = redirectStatus;
    }

    /**
     * @return HTTP 3xx-level status code.
     */
    public Integer getRedirectStatus() {
        return redirectStatus;
    }

    public URL getRedirectURI() {
        return redirectURI;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

}
