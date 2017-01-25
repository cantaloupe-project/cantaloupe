package edu.illinois.library.cantaloupe.operation.redaction;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;

import javax.script.ScriptException;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides information about redactions.
 */
public abstract class RedactionService {

    public static final String REDACTION_ENABLED_CONFIG_KEY =
            "redaction.enabled";

    /**
     * @return Whether {@link #REDACTION_ENABLED_CONFIG_KEY} is true.
     */
    public static boolean isEnabled() {
        return ConfigurationFactory.getInstance().
                getBoolean(REDACTION_ENABLED_CONFIG_KEY, false);
    }

    /**
     * Factory method that returns a list of {@link Redaction redactions}
     * based on the given parameters.
     *
     * @param identifier Image identifier.
     * @param requestHeaders
     * @param clientIp
     * @param cookies
     * @return Redactions applicable to the given parameters, or an empty list
     *         if none.
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     * @throws ConfigurationException
     */
    public static List<Redaction> redactionsFor(Identifier identifier,
                                                Map<String,String> requestHeaders,
                                                String clientIp,
                                                Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException, ConfigurationException {
        final List<Redaction> redactions = new ArrayList<>();

        final List<Map<String,Long>> defs = getRedactionDefsFromScript(
                identifier, requestHeaders, clientIp, cookies);
        if (defs != null) {
            for (Map<String,Long> def : defs) {
                redactions.add(new Redaction(
                        new Rectangle(def.get("x").intValue(),
                                def.get("y").intValue(),
                                def.get("width").intValue(),
                                def.get("height").intValue())));
            }
        }
        return redactions;
    }

    /**
     * @param identifier
     * @param requestHeaders
     * @param clientIp
     * @param cookies
     * @return Map with "file", "inset", and "position" keys; or null
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String,Long>> getRedactionDefsFromScript(
            final Identifier identifier,
            final Map<String,String> requestHeaders,
            final String clientIp,
            final Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String method = "redactions";
        final Object result = engine.invoke(method,
                identifier.toString(), // identifier
                requestHeaders,        // request_headers
                clientIp,              // client_ip
                cookies);              // cookies
        if (result == null || (result instanceof Boolean && !((Boolean) result))) {
            return null;
        }
        return (List<Map<String,Long>>) result;
    }

}
