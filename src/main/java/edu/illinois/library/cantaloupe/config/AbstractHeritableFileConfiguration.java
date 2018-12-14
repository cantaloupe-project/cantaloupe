package edu.illinois.library.cantaloupe.config;

import java.io.File;
import java.util.Set;

/**
 * Configuration that allows file-based inheritance.
 */
abstract class AbstractHeritableFileConfiguration {

    abstract Set<File> getFiles();

}
