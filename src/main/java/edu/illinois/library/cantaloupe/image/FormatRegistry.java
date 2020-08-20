package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Provides access to the master registry of {@link Format}s, which is
 * composed of the union of the sets of formats in:</p>
 *
 * <ol>
 *     <li>The bundled {@literal formats.yml} resource;</li>
 *     <li>Any {@literal formats.yml} that happens to be present in either the
 *     same directory as the configuration file, or, if no such file exists,
 *     the current working directory.</li>
 * </ol>
 *
 * @since 5.0
 */
final class FormatRegistry {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FormatRegistry.class);

    private static final String FILENAME = "formats.yml";
    private static Map<String, Format> FORMATS;

    /**
     * @return Unmodifiable union of all formats in every known {@literal
     *         formats.yml} file.
     */
    static synchronized Set<Format> allFormats() {
        if (FORMATS == null) {
            readFormats();
        }
        return Set.copyOf(FORMATS.values());
    }

    /**
     * For testing only!
     */
    static synchronized void clear() {
        FORMATS = null;
    }

    /**
     * @param key Format {@link Format#getKey() key}.
     * @return    Format with the given key, or {@code null} if no such format
     *            is {@link #allFormats() registered}.
     */
    static synchronized Format formatWithKey(String key) {
        if (FORMATS == null) {
            readFormats();
        }
        return FORMATS.get(key);
    }

    /**
     * <p>Reads the available formats from various files according to the class
     * documentation.</p>
     */
    private static synchronized void readFormats() {
        try {
            FORMATS = readBundledFormats();
            FORMATS.putAll(readUserFormats());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static Map<String, Format> readBundledFormats() throws IOException {
        try (InputStream is = FormatRegistry.class.getClassLoader().getResourceAsStream(FILENAME)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();

            TypeReference<Map<String, Format>> ref = new TypeReference<>() {};
            Map<String, Format> formats = mapper.readValue(is, ref);
            LOGGER.debug("Read {} bundled formats: {}",
                    formats.size(),
                    formats.values().stream().map(Format::getKey).collect(Collectors.joining(", ")));
            return formats;
        }
    }

    private static Map<String, Format> readUserFormats() throws IOException {
        Path pathname = getUserFormatsFilePath();
        if (Files.exists(pathname)) {
            LOGGER.trace("Reading user formats from {}", pathname);
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();

            TypeReference<Map<String, Format>> ref = new TypeReference<>() {};
            Map<String, Format> formats = mapper.readValue(pathname.toFile(), ref);
            LOGGER.debug("Read {} user formats: {}",
                    formats.size(),
                    formats.values().stream().map(Format::getKey).collect(Collectors.joining(", ")));
            return formats;
        }
        return Collections.emptyMap();
    }

    private static Path getUserFormatsFilePath() {
        Configuration config = Configuration.getInstance();
        Optional<Path> configFile = config.getFile();
        Path dir = configFile.isPresent() ?
                configFile.get().getParent() : Paths.get(".");
        return dir.resolve(FILENAME);
    }

}
