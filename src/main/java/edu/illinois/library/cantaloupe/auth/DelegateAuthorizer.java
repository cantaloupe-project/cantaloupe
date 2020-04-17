package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.script.DelegateProxy;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.Map;

/**
 * Authorizes a request using a {@link DelegateProxy}.
 */
final class DelegateAuthorizer implements Authorizer {

    private DelegateProxy delegateProxy;

    /**
     * @param args One-element array containing a {@link DelegateProxy}.
     */
    DelegateAuthorizer(Object... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    DelegateProxy.class.getSimpleName() + " argument is required");
        }
        this.delegateProxy = (DelegateProxy) args[0];
    }

    @Override
    public AuthInfo authorize() throws IOException {
        try {
            final Object result = delegateProxy.authorize();
            if (result instanceof Map) {
                final Map<?, ?> map = (Map<?, ?>) result;
                Long status = (Long) map.get("status_code");
                // Used when returning HTTP 401. Value will be inserted into a
                // WWW-Authorization header.
                String challenge = (String) map.get("challenge");
                // Used when redirecting.
                String uri = (String) map.get("location");
                // Used when redirecting to a virtual scale-reduced version.
                Long scaleNumerator = (Long) map.get("scale_numerator");
                Long scaleDenominator = (Long) map.get("scale_denominator");

                return new AuthInfo.RestrictiveBuilder()
                        .withResponseStatus(status.intValue())
                        .withChallengeValue(challenge)
                        .withRedirectURI(uri)
                        .withRedirectScaleConstraint(scaleNumerator, scaleDenominator)
                        .build();
            } else if (result instanceof Boolean) {
                return new AuthInfo.BooleanBuilder((Boolean) result).build();
            } else {
                throw new IllegalArgumentException(
                        "Illegal return type from delegate method");
            }
        } catch (ScriptException e) {
            throw new IOException(e);
        }
    }

}
