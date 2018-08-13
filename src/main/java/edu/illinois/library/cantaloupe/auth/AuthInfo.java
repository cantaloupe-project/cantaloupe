package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;

/**
 * <p>Authorization info in HTTP context. Supports simple boolean authorization
 * as well as challenges and redirects to external URIs or {@link
 * ScaleConstraint scale-constrained} versions.</p>
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
     * Creates an instance that is restrictive in some way&mdash;it may
     * redirect to a scale-constrained version of an image, for example.
     */
    public static final class RestrictiveBuilder extends Builder {

        public RestrictiveBuilder withChallengeValue(String value) {
            authInfo.challenge = value;
            return this;
        }

        public RestrictiveBuilder withRedirectScaleConstraint(Long numerator,
                                                              Long denominator) {
            authInfo.scaleConstraintNumerator = (numerator != null) ?
                    numerator : 0L;
            authInfo.scaleConstraintDenominator = (denominator != null) ?
                    denominator : 0L;
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

        /**
         * @throws IllegalStateException if the instance being built will be
         *         invalid.
         */
        public AuthInfo build() {
            final int status = authInfo.getResponseStatus();
            final String wwwAuthValue = authInfo.getChallengeValue();

            if (status >= 300 && status < 400 &&
                    authInfo.getRedirectURI() == null &&
                    authInfo.scaleConstraintDenominator == 0) {
                throw new IllegalStateException("Status set to redirect, but " +
                        "neither redirect URI nor scale constraint are set");
            } else if ((authInfo.getRedirectURI() != null ||
                    authInfo.scaleConstraintDenominator > 0) &&
                    (status < 300 || status > 399)) {
                throw new IllegalStateException("Status must be between 300 " +
                        "and 399 when redirect URI or scale constraint are set");
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
    private long scaleConstraintNumerator, scaleConstraintDenominator;

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
     * @return Virtual scale-constrained image to redirect to. May be
     *         {@literal null}.
     */
    public ScaleConstraint getScaleConstraint() {
        return (scaleConstraintDenominator > 0) ?
                new ScaleConstraint(scaleConstraintNumerator,
                        scaleConstraintDenominator) : null;
    }

    /**
     * @return Whether {@link #getResponseStatus()} returns a value less than
     *         {@literal 300}.
     */
    public boolean isAuthorized() {
        return responseStatus < 300;
    }

}
