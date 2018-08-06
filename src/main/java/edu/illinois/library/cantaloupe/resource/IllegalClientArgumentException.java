package edu.illinois.library.cantaloupe.resource;

/**
 * Distinguishes illegal arguments supplied by an end user from all the rest.
 */
public class IllegalClientArgumentException extends IllegalArgumentException {

    public IllegalClientArgumentException() {
        super();
    }

    public IllegalClientArgumentException(String message) {
        super(message);
    }

    public IllegalClientArgumentException(Throwable t) {
        super(t);
    }

    public IllegalClientArgumentException(String message, Throwable t) {
        super(message, t);
    }

}
