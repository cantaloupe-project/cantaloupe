package edu.illinois.library.cantaloupe.operation;

public class IllegalScaleException extends ValidationException {

    IllegalScaleException() {
        super("Access denied for the requested scale.");
    }

}
