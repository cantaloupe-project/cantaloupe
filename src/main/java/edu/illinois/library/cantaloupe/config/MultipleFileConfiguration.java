package edu.illinois.library.cantaloupe.config;

import java.nio.file.Path;
import java.util.Set;

interface MultipleFileConfiguration extends FileConfiguration {

    Set<Path> getFiles();

}
