package edu.illinois.library.cantaloupe.operation;

public class IllegalSizeException extends ValidationException {

    IllegalSizeException() {
        super("The requested pixel area exceeds the maximum threshold set " +
                "in the configuration.");
    }

}
