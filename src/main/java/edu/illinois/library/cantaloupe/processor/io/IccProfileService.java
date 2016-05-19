package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class IccProfileService {

    private static Logger logger = LoggerFactory.
            getLogger(IccProfileService.class);

    static final String ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY =
            "icc.BasicStrategy.profile";
    static final String ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY =
            "icc.BasicStrategy.profile_name";
    static final String ICC_ENABLED_CONFIG_KEY = "icc.enabled";
    static final String ICC_STRATEGY_CONFIG_KEY = "icc.strategy";

    /**
     * Finds the given profile whether it is given in the form of an absolute
     * path or a filename. If the latter, it will be searched for in the same
     * folder as the application config (if available), or the current working
     * directory if not.
     *
     * @param filenameOrPathname Filename or absolute pathname
     * @return File corresponding to <var>profileFilename</var>
     */
    private File findProfile(String filenameOrPathname) {
        File profileFile = new File(filenameOrPathname);
        if (!profileFile.isAbsolute()) {
            final File configFile =
                    Configuration.getInstance().getConfigurationFile();
            if (configFile != null) {
                profileFile = new File(configFile.getParent() + "/" +
                        profileFile.getName());
            } else {
                profileFile = new File("./" + profileFile.getName());
            }
        }
        return profileFile;
    }

    /**
     * Returns a profile corresponding to the application configuration and
     * the given arguments. The arguments will only be used if
     * {@link #ICC_STRATEGY_CONFIG_KEY} is set to <var>ScriptStrategy</var>.
     *
     * @param identifier Image identifier to pass to the delegate method.
     * @param requestHeaders Request headers to pass to the delegate method.
     * @param clientIp Client IP address to pass to the delegate method.
     * @return Profile corresponding to the given parameters as returned by
     *         the delegate method, or null if none was returned.
     * @throws IOException
     */
    public IccProfile getProfile(Identifier identifier,
                                 Map<String,String> requestHeaders,
                                 String clientIp) throws IOException {
        Configuration config = Configuration.getInstance();
        switch (config.getString(ICC_STRATEGY_CONFIG_KEY, "")) {
            case "BasicStrategy":
                final String profileFilename = Configuration.getInstance().
                        getString(ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY);
                if (profileFilename != null) {
                    final String profileName = Configuration.getInstance().
                            getString(ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY);
                    return new IccProfile(profileName,
                            findProfile(profileFilename));
                }
            case "ScriptStrategy":
                try {
                    return getProfileFromDelegateMethod(identifier,
                            requestHeaders, clientIp);
                } catch (ScriptException e) {
                    throw new IOException(e.getMessage(), e);
                }
        }
        return null;
    }

    /**
     * Returns a profile corresponding to the given parameters from the
     * <code>icc_profile()</code> delegate method.
     *
     * @param identifier Image identifier to pass to the delegate method.
     * @param requestHeaders Request headers to pass to the delegate method.
     * @param clientIp Client IP address to pass to the delegate method.
     * @return Profile corresponding to the given parameters as returned by
     *         the delegate method, or null if none was returned.
     * @throws IOException
     * @throws ScriptException
     */
    private IccProfile getProfileFromDelegateMethod(Identifier identifier,
                                                    Map<String,String> requestHeaders,
                                                    String clientIp)
            throws IOException, ScriptException {
        // Delegate method parameters
        final Object args[] = new Object[] {
                identifier, requestHeaders, clientIp };
        try {
            // The delegate method is expected to return a hash (Map)
            // containing `name` and `pathname` keys, or an empty hash if
            // there is no profile to embed.
            final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
            final String method = "icc_profile";
            final Map result = (Map) engine.invoke(method, args);
            if (result != null && result.size() > 0) {
                return new IccProfile((String) result.get("name"),
                        findProfile((String) result.get("pathname")));
            }
        } catch (DelegateScriptDisabledException e) {
            logger.info("addMetadataUsingScriptStrategy(): delegate script " +
                    "disabled; aborting.");
        }
        return null;
    }

    /**
     * @return Whether ICC profiles should be embedded in derivative images
     *         under any circumstances according to the
     *         {@link #ICC_ENABLED_CONFIG_KEY} key in the application
     *         configuration.
     */
    public boolean isEnabled() {
        return Configuration.getInstance().
                getBoolean(ICC_ENABLED_CONFIG_KEY, false);
    }

}
