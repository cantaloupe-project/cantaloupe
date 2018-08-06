package edu.illinois.library.cantaloupe.operation;

public class ExcessiveSizeException extends ValidationException {

    ExcessiveSizeException() {
        super("The requested pixel area exceeds the maximum threshold set " +
                "in the configuration.");
    }

}
