package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FormatRegistryTest extends BaseTest {

    /* allFormats() */

    @Test
    void testAllFormatsReadsBundledFormats() {
        Set<String> expected = Set.of("avi", "bmp", "flv", "gif", "jp2", "jpg",
                "mov", "mp4", "mpg", "pdf", "png", "tif", "webm", "webp",
                "xpm");
        Set<String> actual = FormatRegistry.allFormats()
                .stream()
                .map(Format::getKey)
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    void testAllFormatsReadsUserFormats() throws Exception {
        // If there is already a ./formats.yml file, move it out of the way.
        Path pathname    = Paths.get(".", "formats.yml");
        Path tmpPathname = Paths.get(".", "formats-" + UUID.randomUUID() + ".yml" );
        if (Files.exists(pathname)) {
            Files.move(pathname, tmpPathname);
        }

        try {
            FormatRegistry.clear();

            // Get the registry size excepting any user formats.
            Set<Format> formats = FormatRegistry.allFormats();
            final int initialSize = formats.size();

            // Write a new formats.yml file.
            String yaml = "test:\n" +
                    "  key: test\n" +
                    "  name: Test Format\n" +
                    "  extensions:\n" +
                    "    - test\n" +
                    "  mediaTypes:\n" +
                    "    - test/test\n" +
                    "  raster: true\n" +
                    "  video: false\n" +
                    "  supportsTransparency: false";
            Files.writeString(pathname, yaml);

            FormatRegistry.clear();

            // Check again.
            formats = FormatRegistry.allFormats();
            assertTrue(formats.size() > initialSize);
        } finally {
            // Delete the temporary formats.yml.
            try {
                if (Files.exists(pathname)) {
                    Files.delete(pathname);
                    // Move the initial one back into place.
                    if (Files.exists(tmpPathname)) {
                        Files.move(tmpPathname, pathname);
                    }
                }
            } finally {
                FormatRegistry.clear();
            }
        }
    }

    /* formatWithKey() */

    @Test
    void testFormatWithKeyWithRecognizedKey() {
        Format format = FormatRegistry.formatWithKey("jpg");
        assertEquals("JPEG", format.getName());
    }

    @Test
    void testFormatWithKeyWithUnrecognizedKey() {
        Format format = FormatRegistry.formatWithKey("bogus");
        assertNull(format);
    }

}