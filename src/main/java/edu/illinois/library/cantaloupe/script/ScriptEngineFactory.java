package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class ScriptEngineFactory {

    /**
     * @return New ScriptEngine instance with the delegate script code loaded;
     * or, if there is no delegate script, an empty module.
     * @throws IOException
     * @throws ScriptException
     */
    public static ScriptEngine getScriptEngine()
            throws IOException, ScriptException {
        final ScriptEngine engine = new RubyScriptEngine();
        final File script = getScript();
        if (script != null) {
            engine.load(FileUtils.readFileToString(script));
        } else {
            engine.load("module Cantaloupe\nend");
        }
        return engine;
    }

    private static File getScript() throws FileNotFoundException {
        final Configuration config = Application.getConfiguration();
        // The script name may be an absolute path or a filename.
        final String scriptValue = config.getString("delegate_script");
        if (scriptValue != null) {
            File script = findScript(scriptValue);
            if (!script.exists()) {
                throw new FileNotFoundException("Does not exist: " +
                        script.getAbsolutePath());
            }
            return script;
        }
        return null;
    }

    /**
     * @param scriptNameOrPathname
     * @return Canonical file representing a given script name or absolute
     * pathname. Existence of the underlying file is not checked.
     */
    private static File findScript(String scriptNameOrPathname) {
        File script = new File(scriptNameOrPathname);
        if (!script.isAbsolute()) {
            // Search for it in the same folder as the application config
            // (if available), or the current working directory if not.
            final File configFile = Application.getConfigurationFile();
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
