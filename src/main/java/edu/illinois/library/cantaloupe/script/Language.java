package edu.illinois.library.cantaloupe.script;

public enum Language {

    JAVASCRIPT, RUBY;

    /**
     * @param path Pathname of a script file.
     * @return     Language of the file at the given path.
     * @throws IllegalArgumentException if the pathname extension is
     *         unrecognized.
     */
    static Language forPath(String path) {
        if (path.endsWith(".js")) {
            return JAVASCRIPT;
        } else if (path.endsWith(".rb")) {
            return RUBY;
        }
        throw new IllegalArgumentException("Unrecognized filename extension");
    }

}
