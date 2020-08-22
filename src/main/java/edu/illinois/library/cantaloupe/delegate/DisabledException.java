package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.config.Key;

public class DisabledException extends Exception {

    @Override
    public String getMessage() {
        return "The delegate script is disabled (" +
                Key.DELEGATE_SCRIPT_ENABLED + " = false)";
    }

}
