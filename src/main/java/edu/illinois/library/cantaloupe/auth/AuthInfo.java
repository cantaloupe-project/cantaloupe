package edu.illinois.library.cantaloupe.auth;

/**
 * <p>Authorization info in HTTP context. Supports boolean authorization as
 * well as challenges and redirects.</p>
 *
 * <p>Create instances using one of the inner builder classes.</p>
 */
public final class AuthInfo {

    private static abstract class Builder {

        final AuthInfo authInfo = new AuthInfo();

    }

    /**
     * Creates an instance that allows or denies a request.
     */
    public static final class BooleanBuilder extends Builder {

        /**
         * N.B.: The
         * <a href="https://iiif.io/api/auth/1.0/#all-or-nothing-access">IIIF
         * Authentication API 1.0</a> calls for 401, but that status must be
         * accompanied by a {@literal WWW-Authenticate} header which this
         * builder cannot provide.
         */
        private static final int DEFAULT_UNAUTHORIZED_STATUS = 403;

        public BooleanBuilder(boolean authorized) {
            if (!authorized) {
                authInfo.responseStatus = DEFAULT_UNAUTHORIZED_STATUS;
            }
        }

        public AuthInfo build() {
            return authInfo;
        }

    }

    /**
     * Creates an instance that is restrictive in some way.
     */
    public static final class RestrictiveBuilder extends Builder {

        public RestrictiveBuilder withChallengeValue(String value) {
            authInfo.challenge = value;
            return this;
        }

        public RestrictiveBuilder withRedirectURI(String uri) {
            authInfo.redirectURI = uri;
            return this;
        }

        public RestrictiveBuilder withResponseStatus(int status) {
            authInfo.responseStatus = status;
            return this;
        }

        public AuthInfo build() {
            final int status = authInfo.getResponseStatus();
            final String wwwAuthValue = authInfo.getChallengeValue();

            if (status >= 300 && status < 400 &&
                    authInfo.getRedirectURI() == null) {
                throw new IllegalStateException("Redirect URI is not set");
            } else if (authInfo.getRedirectURI() != null &&
                    (status < 300 || status > 399)) {
                throw new IllegalStateException("Status must be between 300 " +
                        "and 399 when redirect URI is set");
            } else if (status == 401 && (wwwAuthValue == null || wwwAuthValue.isEmpty())) {
                throw new IllegalStateException("WWW-Authenticate header " +
                        "value is required when the status is 401");
            } else if (status != 401 && wwwAuthValue != null && !wwwAuthValue.isEmpty()) {
                throw new IllegalStateException("WWW-Authenticate header is " +
                        "only allowed when the status is 401");
            }
            return authInfo;
        }

    }

    private int responseStatus = 200;
    private String challenge, redirectURI;

    private AuthInfo() {}

    /**
     * @return Value of the {@literal WWW-Authenticate} header. Will be
     *         non-{@literal null} when {@link #getResponseStatus()} returns
     *         {@literal 401}, and {@literal null} otherwise.
     */
    public String getChallengeValue() {
        return challenge;
    }

    /**
     * @return URI to redirect to. Will be {@literal null} unless {@link
     *         #getResponseStatus()} returns a 3xx value.
     */
    public String getRedirectURI() {
        return redirectURI;
    }

    /**
     * @return HTTP 3xx- or 4xx-level status code. May be {@literal null}.
     */
    public int getResponseStatus() {
        return responseStatus;
    }

    /**
     * @return Whether {@link #getResponseStatus()} returns a value less than
     *         {@literal 300}.
     */
    public boolean isAuthorized() {
        return responseStatus < 300;
    }

}
