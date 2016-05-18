package edu.illinois.library.cantaloupe.processor.io;

import java.awt.color.ICC_Profile;

/**
 * Wraps an {@link ICC_Profile} instance, adding a name accessor.
 */
class IccProfile {

    private String name;
    private ICC_Profile profile;

    IccProfile(String name, ICC_Profile profile) {
        this.name = name;
        this.profile = profile;
    }

    String getName() {
        return name;
    }

    ICC_Profile getProfile() {
        return profile;
    }

}
