package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Spring Boot test for FormatRegistry.
 * Tests format loading and registry functionality with dependency injection.
 */
@SpringBootTest(classes = {FormatRegistry.class, FormatRegistryAccessor.class})
@TestPropertySource(properties = {
    "logging.level.root=WARN"
})
class FormatRegistryTest {

    @Autowired
    private FormatRegistry formatRegistry;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Mock the configuration to return the current working directory
        when(configuration.getFile()).thenReturn(Optional.empty());
    }

    /* allFormats() */

    @Test
    void testAllFormatsReadsBundledFormats() {
        Set<String> expected = Set.of("avi", "bmp", "flv", "gif", "jp2", "jpg",
                "mov", "mp4", "mpg", "pdf", "png", "tif", "webm", "webp",
                "xpm");
        Set<String> actual = formatRegistry.allFormats()
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
            formatRegistry.clear();

            // Get the registry size excepting any user formats.
            Set<Format> formats = formatRegistry.allFormats();
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

            formatRegistry.clear();

            // Check again.
            formats = formatRegistry.allFormats();
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
                formatRegistry.clear();
            }
        }
    }

    /* formatWithKey() */

    @Test
    void testFormatWithKeyWithRecognizedKey() {
        Format format = formatRegistry.formatWithKey("jpg");
        assertEquals("JPEG", format.getName());
    }

    @Test
    void testFormatWithKeyWithUnrecognizedKey() {
        Format format = formatRegistry.formatWithKey("bogus");
        assertNull(format);
    }

    /* Static accessor tests */

    @Test
    void testStaticAccessorAllFormats() {
        Set<String> expected = Set.of("avi", "bmp", "flv", "gif", "jp2", "jpg",
                "mov", "mp4", "mpg", "pdf", "png", "tif", "webm", "webp",
                "xpm");
        Set<String> actual = FormatRegistryAccessor.getAllFormats()
                .stream()
                .map(Format::getKey)
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    void testStaticAccessorFormatWithKey() {
        Format format = FormatRegistryAccessor.getFormatWithKey("jpg");
        assertEquals("JPEG", format.getName());
    }

    @Test
    void testStaticAccessorIsAvailable() {
        assertTrue(FormatRegistryAccessor.isAvailable());
    }
}
