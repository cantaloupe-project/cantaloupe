package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.config.Key;

public class IllegalSizeException extends ValidationException {

    IllegalSizeException() {
        super("The requested pixel area exceeds the maximum threshold (" +
                Key.MAX_PIXELS + ") set " + "in the configuration.");
    }

}
