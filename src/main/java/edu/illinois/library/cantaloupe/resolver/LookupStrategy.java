package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

enum LookupStrategy {

    BASIC, DELEGATE_SCRIPT;

    static LookupStrategy from(Key key) {
        final Configuration config = Configuration.getInstance();

        switch (config.getString(key)) {
            case "BasicLookupStrategy":
                return BASIC;
            case "ScriptLookupStrategy":
                return DELEGATE_SCRIPT;
            default:
                throw new IllegalArgumentException(
                        key + " is invalid or not set");
        }
    }

}
