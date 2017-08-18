package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Authorizes a request using the delegate script mechanism.
 */
public class Authorizer {

    private static final Logger logger =
            LoggerFactory.getLogger(Authorizer.class);

    private static final String AUTHORIZATION_DELEGATE_METHOD = "authorized?";

    private String clientIP;
    private Map<String,String> cookies;
    private Map<String,String> requestHeaders;
    private String requestURI;

    public Authorizer(String requestURI,
                      String clientIP,
                      Map<String,String> requestHeaders,
                      Map<String,String> cookies) {
        this.requestURI = requestURI;
        this.clientIP = clientIP;
        this.requestHeaders = requestHeaders;
        this.cookies = cookies;
    }

    /**
     * <p>Invokes the {@link #AUTHORIZATION_DELEGATE_METHOD} delegate method to
     * determine whether the request is authorized.</p>
     *
     * <p>The delegate method may return a boolean or a hash. If it returns a
     * hash, it must contain <code>location</code>, <code>status_code</code>,
     * and <code>status_line</code> keys.</p>
     *
     * <p>If the delegate script is disabled, the returned {@link AuthInfo}'s
     * {@link AuthInfo#isAuthorized()} method will return <code>true</code>.</p>
     *
     * <p>N.B. The reason there aren't separate delegate methods to perform
     * authorization and redirecting is because these will often require
     * similar or identical service requests on the part of the client. Having
     * one method handle both scenarios simplifies implementation and reduces
     * cost.</p>
     *
     * @param opList Operations requested on the image.
     * @param fullSize Full size of the requested image.
     * @return AuthInfo reflecting the return value of the
     *         {@link #AUTHORIZATION_DELEGATE_METHOD} delegate method.
     * @throws IOException
     * @throws ScriptException
     */
    public AuthInfo authorize(final OperationList opList,
                              final Dimension fullSize)
            throws IOException, ScriptException {
        final Map<String,Integer> fullSizeArg = new HashMap<>();
        fullSizeArg.put("width", fullSize.width);
        fullSizeArg.put("height", fullSize.height);

        final Dimension resultingSize = opList.getResultingSize(fullSize);
        final Map<String,Integer> resultingSizeArg = new HashMap<>();
        resultingSizeArg.put("width", resultingSize.width);
        resultingSizeArg.put("height", resultingSize.height);

        final Map<String, Object> opListMap = opList.toMap(fullSize);

        try {
            final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
            Object result = engine.invoke(AUTHORIZATION_DELEGATE_METHOD,
                    opList.getIdentifier().toString(),  // identifier
                    fullSizeArg,                        // full_size
                    opListMap.get("operations"),        // operations
                    resultingSizeArg,                   // resulting_size
                    opListMap.get("output_format"),     // output_format
                    requestURI,                         // request_uri
                    requestHeaders,                     // request_headers
                    clientIP,                           // client_ip
                    cookies);                           // cookies
            if (result instanceof Boolean) {
                if (!((boolean) result)) {
                    return new AuthInfo(false);
                }
            } else {
                final Map<?, ?> redirectInfo = (Map<?, ?>) result;
                final String location = redirectInfo.get("location").toString();
                // Prevent circular redirects
                if (!requestURI.equals(location)) {
                    final int statusCode =
                            Integer.parseInt(redirectInfo.get("status_code").toString());
                    if (statusCode < 300 || statusCode > 399) {
                        throw new IllegalArgumentException(
                                "Status code must be in the range of 300-399.");
                    }
                    return new AuthInfo(false, new URL(location), statusCode);
                }
            }
        } catch (DelegateScriptDisabledException e) {
            logger.debug("Delegate script is disabled; allowing.");
            return new AuthInfo(true);
        }
        return new AuthInfo(true);
    }

}
