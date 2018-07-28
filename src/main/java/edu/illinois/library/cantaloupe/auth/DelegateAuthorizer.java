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
            throw new NullPointerException(
                    DelegateProxy.class.getSimpleName() + " argument is required");
        }
        this.delegateProxy = (DelegateProxy) args[0];
    }

    @Override
    public AuthInfo authorize() throws IOException {
        try {
            final Object result = delegateProxy.authorize();
            if (result instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) result;
                Long status = (Long) map.get("status_code");
                String uri = (String) map.get("location");
                String challenge = (String) map.get("challenge");

                return new AuthInfo.RestrictiveBuilder()
                        .withResponseStatus(status.intValue())
                        .withChallengeValue(challenge)
                        .withRedirectURI(uri)
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
