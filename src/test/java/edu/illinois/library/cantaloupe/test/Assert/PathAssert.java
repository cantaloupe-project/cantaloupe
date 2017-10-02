package edu.illinois.library.cantaloupe.test.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public final class PathAssert {

    public static void assertRecursiveFileCount(Path dir, long expected) {
        try {
            long count = Files.walk(dir).filter(Files::isRegularFile)
                    .mapToLong(f -> 1).sum();
            assertEquals(expected, count);
        } catch (IOException e) {
            fail(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private PathAssert() {}

}
