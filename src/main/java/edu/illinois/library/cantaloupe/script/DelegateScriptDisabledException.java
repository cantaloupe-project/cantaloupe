package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.config.Key;

public class DelegateScriptDisabledException extends Exception {

    @Override
    public String getMessage() {
        return "The delegate script is disabled (" +
                Key.DELEGATE_SCRIPT_ENABLED + " = false)";
    }

}
