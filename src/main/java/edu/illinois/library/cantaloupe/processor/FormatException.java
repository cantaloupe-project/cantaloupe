package edu.illinois.library.cantaloupe.processor;

import java.io.IOException;

public abstract class FormatException extends IOException {

    FormatException(String message) {
        super(message);
    }

}
