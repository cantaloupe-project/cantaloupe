package edu.illinois.library.cantaloupe.image.icc;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IccProfileService {

    private static Logger logger = LoggerFactory.
            getLogger(IccProfileService.class);

    public static final String ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY =
            "icc.BasicStrategy.profile";
    public static final String ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY =
            "icc.BasicStrategy.profile_name";
    public static final String ICC_ENABLED_CONFIG_KEY = "icc.enabled";
    public static final String ICC_STRATEGY_CONFIG_KEY = "icc.strategy";

    /**
     * @return Whether ICC profiles should be embedded in derivative images
     *         under any circumstances according to the
     *         {@link #ICC_ENABLED_CONFIG_KEY} key in the application
     *         configuration.
     */
    public static boolean isEnabled() {
        return Configuration.getInstance().
                getBoolean(ICC_ENABLED_CONFIG_KEY, false);
    }

    /**
     * Returns a profile corresponding to the application configuration and
     * the given arguments. The arguments will only be used if
     * {@link #ICC_STRATEGY_CONFIG_KEY} is set to <var>ScriptStrategy</var>.
     *
     * @param identifier Image identifier to pass to the delegate method.
     * @param outputFormat Format of the image into which the profile is to
     *                     be embedded.
     * @param requestHeaders Request headers to pass to the delegate method.
     * @param clientIp Client IP address to pass to the delegate method.
     * @return Profile corresponding to the given parameters as returned by
     *         the delegate method, or null if none was returned.
     * @throws IOException
     */
    public IccProfile getProfile(Identifier identifier,
                                 Format outputFormat,
                                 Map<String,String> requestHeaders,
                                 String clientIp) throws IOException {
        final Configuration config = Configuration.getInstance();
        switch (config.getString(ICC_STRATEGY_CONFIG_KEY, "")) {
            case "BasicStrategy":
                return getProfileUsingBasicStrategy();
            case "ScriptStrategy":
                try {
                    return getProfileUsingScriptStrategy(identifier,
                            outputFormat, requestHeaders, clientIp);
                } catch (ScriptException e) {
                    throw new IOException(e.getMessage(), e);
                }
        }
        return null;
    }

    /**
     * @return Profile corresponding to the
     *         {@link #ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY} and
     *         {@link #ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY} keys in the
     *         application configuration.
     */
    private IccProfile getProfileUsingBasicStrategy() {
        final String profileFilename = Configuration.getInstance().
                getString(ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY, "");
        if (profileFilename.length() > 0) {
            final String profileName = Configuration.getInstance().
                    getString(ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY, "");
            if (profileName != null) {
                return new IccProfile(profileName, new File(profileFilename));
            } else {
                logger.warn("{} is not set; skipping profile embed.",
                        ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY);
            }
        } else {
            logger.warn("{} is not set; skipping profile embed.",
                    ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY);
        }
        return null;
    }

    /**
     * @param identifier Image identifier to pass to the delegate method.
     * @param outputFormat Format of the image into which the profile is to
     *                     be embedded.
     * @param requestHeaders Request headers to pass to the delegate method.
     * @param clientIp Client IP address to pass to the delegate method.
     * @return Profile corresponding to the given parameters as returned by
     *         the <code>icc_profile()</code>delegate method, or
     *         <code>null</code> if none was returned.
     * @throws IOException
     * @throws ScriptException
     */
    private IccProfile getProfileUsingScriptStrategy(Identifier identifier,
                                                     Format outputFormat,
                                                     Map<String,String> requestHeaders,
                                                     String clientIp)
            throws IOException, ScriptException {
        // Assemble delegate method parameters
        final Map<String,String> formatArg = new HashMap<>();
        formatArg.put("media_type",
                outputFormat.getPreferredMediaType().toString());
        formatArg.put("extension", outputFormat.getPreferredExtension());

        final Object args[] = new Object[] {
                identifier.toString(), formatArg, requestHeaders, clientIp };
        try {
            // The delegate method is expected to return a hash (Map)
            // containing `name` and `pathname` keys, or an empty hash if
            // there is no profile to embed.
            final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
            final String method = "icc_profile";
            final Map result = (Map) engine.invoke(method, args);
            if (result != null && result.size() > 0) {
                return new IccProfile((String) result.get("name"),
                        new File((String) result.get("pathname")));
            }
        } catch (DelegateScriptDisabledException e) {
            logger.info("addMetadataUsingScriptStrategy(): delegate script " +
                    "disabled; aborting.");
        }
        return null;
    }

}
