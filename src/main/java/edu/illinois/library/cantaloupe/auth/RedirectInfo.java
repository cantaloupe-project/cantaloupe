package edu.illinois.library.cantaloupe.auth;

import java.net.URI;

public class RedirectInfo {

    private int redirectStatus;
    private URI redirectURI;

    public RedirectInfo(URI redirectURI, int redirectStatus) {
        this.redirectURI = redirectURI;
        this.redirectStatus = redirectStatus;
    }

    /**
     * @return HTTP 3xx-level status code. May be {@literal null}.
     */
    public int getRedirectStatus() {
        return redirectStatus;
    }

    /**
     * @return URI to redirect to. May be {@literal null}.
     */
    public URI getRedirectURI() {
        return redirectURI;
    }

}
