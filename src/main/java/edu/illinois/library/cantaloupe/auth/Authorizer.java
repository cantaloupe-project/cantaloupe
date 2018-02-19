package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.script.DelegateProxy;

import javax.script.ScriptException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Authorizes a request.
 */
public final class Authorizer {

    private DelegateProxy delegateProxy;

    /**
     * No-op constructor for an instance that authorizes everything.
     */
    public Authorizer() {}

    /**
     * Constructor for an instance that acquires authorization info from the
     * given {@link DelegateProxy}.
     */
    public Authorizer(DelegateProxy proxy) {
        this();
        this.delegateProxy = proxy;
    }

    /**
     * Determines whether the request is authorized.
     */
    public AuthInfo authorize() throws ScriptException {
        boolean result = true;

        if (delegateProxy != null) {
            result = delegateProxy.isAuthorized();
        }
        return new AuthInfo(result);
    }

    /**
     * Determines whether the request should trigger a redirect.
     *
     * @return New instance, or {@literal null} to indicate no redirect.
     */
    public RedirectInfo redirect() throws ScriptException {
        if (delegateProxy != null) {
            final Map<String,Object> map = delegateProxy.getRedirect();
            if (map != null && !map.isEmpty()) {
                final String location = (String) map.get("location");
                final Long status = (Long) map.get("status_code");
                try {
                    return new RedirectInfo(new URI(location), status.intValue());
                } catch (URISyntaxException e) {
                    throw new ScriptException(e);
                }
            }
        }
        return null;
    }

}
