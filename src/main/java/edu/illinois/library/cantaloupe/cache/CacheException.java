package edu.illinois.library.cantaloupe.cache;

public class CacheException extends Exception {

    public CacheException(String message) {
        super(message);
    }

    public CacheException(String message, Throwable t) {
        super(message, t);
    }

}
