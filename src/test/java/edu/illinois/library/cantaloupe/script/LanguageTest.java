package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LanguageTest extends BaseTest {

    @Test
    void forPathWithJavaScript() {
        assertEquals(Language.JAVASCRIPT, Language.forPath("file.js"));
    }

    @Test
    void forPathWithRuby() {
        assertEquals(Language.RUBY, Language.forPath("file.rb"));
    }

    @Test
    void forPathWithUnsupportedPath() {
        assertThrows(IllegalArgumentException.class,
                () -> Language.forPath("file"));
        assertThrows(IllegalArgumentException.class,
                () -> Language.forPath("file.txt"));
    }

}
