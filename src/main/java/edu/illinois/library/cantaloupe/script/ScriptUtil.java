package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.Application;

import java.io.File;

public abstract class ScriptUtil {

    /**
     * @param scriptNameOrPathname
     * @return Canonical file representing a given script name or absolute
     * pathname. Existence of the underlying file is not checked.
     */
    public static File findScript(String scriptNameOrPathname) {
        File script = new File(scriptNameOrPathname);
        if (!script.isAbsolute()) {
            // Search for it in the same folder as the application
            // config (if available), or the current working
            // directory if not.
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
