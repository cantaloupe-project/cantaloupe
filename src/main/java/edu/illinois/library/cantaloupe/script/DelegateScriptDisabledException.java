package edu.illinois.library.cantaloupe.script;

public class DelegateScriptDisabledException extends Exception {

    @Override
    public String getMessage() {
        return "The delegate script is disabled (" +
                ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY +
                " = false)";
    }

}
