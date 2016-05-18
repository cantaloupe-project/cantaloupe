package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

class IccProfileService {

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
     * <p>Returns an instance corresponding to the filename or pathname of an
     * ICC profile. If a filename is given, it will be searched for in the
     * same folder as the application config (if available), or the current
     * working directory if not.</p>
     *
     * @param profileFilenameOrPathname Profile filename or pathname
     * @return Instance reflecting a given profile.
     * @throws IOException
     */
    ICC_Profile getProfile(String profileFilenameOrPathname)
            throws IOException {
        final FileInputStream in =
                new FileInputStream(findProfile(profileFilenameOrPathname));
        try {
            return ICC_Profile.getInstance(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
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
    ICC_Profile getProfileFromDelegateMethod(Identifier identifier,
                                             Map<String,String> requestHeaders,
                                             String clientIp)
            throws IOException, ScriptException {
        // delegate method parameters
        final Object args[] = new Object[] {
                identifier, requestHeaders, clientIp };
        try {
            final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
            final String method = "icc_profile";
            final String result = (String) engine.invoke(method, args);
            if (result != null) {
                return getProfile(result);
            }
        } catch (DelegateScriptDisabledException e) {
            logger.info("addMetadataUsingScriptStrategy(): delegate script " +
                    "disabled; aborting.");
        }
        return null;
    }

}
