package edu.illinois.library.cantaloupe.cache;

public class CacheDisabledException extends Exception {

    public CacheDisabledException() {
        super();
    }

    public CacheDisabledException(String message) {
        super(message);
    }

}
