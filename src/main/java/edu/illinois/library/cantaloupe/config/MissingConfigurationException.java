package edu.illinois.library.cantaloupe.config;

public class MissingConfigurationException extends RuntimeException {

    MissingConfigurationException(String message) {
        super(message);
    }

}
