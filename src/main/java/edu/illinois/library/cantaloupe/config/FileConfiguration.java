package edu.illinois.library.cantaloupe.config;

import java.io.File;

public interface FileConfiguration extends Configuration {

    /**
     * @return File corresponding to the persistent configuration file.
     */
    File getFile();

}
