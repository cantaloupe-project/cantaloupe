package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.config.Key;

public class UnavailableException extends Exception {

    @Override
    public String getMessage() {
        return "A delegate is not available. Either a Java delegate must be " +
                "provided on the classpath, or the delegate script must be enabled (" +
                Key.DELEGATE_SCRIPT_ENABLED + " = true)";
    }

}
