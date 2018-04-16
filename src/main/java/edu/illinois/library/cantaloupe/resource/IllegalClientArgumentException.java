package edu.illinois.library.cantaloupe.resource;

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
