package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.apache.commons.io.FileUtils;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Provides access to a shared {@link ScriptEngine} instance.
 */
public abstract class ScriptEngineFactory {

    public static final String DELEGATE_SCRIPT_ENABLED_CONFIG_KEY =
            "delegate_script.enabled";
    public static final String DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY =
            "delegate_script.pathname";

    private static final Object lock = new Object();

    private static ScriptEngine scriptEngine;

    /**
     * @return Shared ScriptEngine instance, ready for use.
     * @throws FileNotFoundException If the delegate script specified in the
     *                               application configuration was not found.
     * @throws DelegateScriptDisabledException If the delegate script is
     *                                         disabled in the application
     *                                         configuration.
     * @throws IOException
     * @throws ScriptException
     */
    public static ScriptEngine getScriptEngine() throws IOException,
            DelegateScriptDisabledException, ScriptException {
        if (scriptEngine == null) {
            synchronized (lock) {
                final Configuration config = ConfigurationFactory.getInstance();
                if (config.getBoolean(DELEGATE_SCRIPT_ENABLED_CONFIG_KEY, false)) {
                    scriptEngine = new RubyScriptEngine();
                    scriptEngine.load(FileUtils.readFileToString(getScriptFile()));
                } else {
                    throw new DelegateScriptDisabledException();
                }
            }
        }
        return scriptEngine;
    }

    /**
     * @return File representing the delegate script, whether or not the
     *         delegate script system is enabled.
     * @throws FileNotFoundException If the script specified in
     *         {@link #DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY} does not exist, or
     *         if no script is specified.
     */
    static File getScriptFile() throws FileNotFoundException {
        final Configuration config = ConfigurationFactory.getInstance();
        // The script name may be an absolute path or a filename.
        final String scriptValue =
                config.getString(DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY, "");
        if (scriptValue != null && scriptValue.length() > 0) {
            File script = findScript(scriptValue);
            if (!script.exists()) {
                throw new FileNotFoundException(script.getAbsolutePath());
            }
            return script;
        }
        throw new FileNotFoundException();
    }

    /**
     * @param scriptNameOrPathname
     * @return Canonical file representing a given script name or absolute
     *         pathname. Existence of the underlying file is not checked.
     */
    private static File findScript(String scriptNameOrPathname) {
        File script = new File(scriptNameOrPathname);
        if (!script.isAbsolute()) {
            // Search for it in the same folder as the application config
            // (if available), or the current working directory if not.
            final File configFile =
                    ConfigurationFactory.getInstance().getFile();
            if (configFile != null) {
                script = new File(configFile.getParent() + "/" +
                        script.getName());
            } else {
                script = new File("./" + script.getName());
            }
        }
        return script;
    }

}
