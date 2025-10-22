package edu.illinois.library.cantaloupe.image;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import edu.illinois.library.cantaloupe.config.Configuration;
import jakarta.annotation.PostConstruct;

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
@Component
public class FormatRegistry {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FormatRegistry.class);

    private static final String FILENAME = "formats.yml";

    private final Configuration config;
    private Map<String, Format> formats;

    // Static fallback for non-Spring usage
    private static Map<String, Format> STATIC_FORMATS;

    @Autowired
    public FormatRegistry(Configuration config) {
        this.config = config;
    }

    @PostConstruct
    private void initialize() {
        readFormats();
    }

    /**
     * @return Unmodifiable union of all formats in every known {@literal
     *         formats.yml} file.
     */
    public Set<Format> allFormats() {
        if (formats == null) {
            readFormats();
        }
        return Set.copyOf(formats.values());
    }

    /**
     * For testing only!
     */
    public void clear() {
        formats = null;
    }

    /**
     * @param key Format {@link Format#getKey() key}.
     * @return    Format with the given key, or {@code null} if no such format
     *            is {@link #allFormats() registered}.
     */
    public Format formatWithKey(String key) {
        if (formats == null) {
            readFormats();
        }
        return formats.get(key);
    }

    /**
     * <p>Reads the available formats from various files according to the class
     * documentation.</p>
     */
    private synchronized void readFormats() {
        try {
            formats = readBundledFormats();
            formats.putAll(readUserFormats());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private Map<String, Format> readBundledFormats() throws IOException {
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

    private Map<String, Format> readUserFormats() throws IOException {
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

    private Path getUserFormatsFilePath() {
        Optional<Path> configFile = config.getFile();
        Path dir = configFile.isPresent() ?
                configFile.get().getParent() : Paths.get(".");
        return dir.resolve(FILENAME);
    }

    // Static fallback methods for non-Spring usage

    /**
     * Static fallback for allFormats() when Spring context is not available.
     * @return Unmodifiable union of all formats in every known formats.yml file.
     */
    public static synchronized Set<Format> allFormatsStatic() {
        if (STATIC_FORMATS == null) {
            readStaticFormats();
        }
        return Set.copyOf(STATIC_FORMATS.values());
    }

    /**
     * Static fallback for formatWithKey() when Spring context is not available.
     * @param key Format key.
     * @return Format with the given key, or null if no such format is registered.
     */
    public static synchronized Format formatWithKeyStatic(String key) {
        if (STATIC_FORMATS == null) {
            readStaticFormats();
        }
        return STATIC_FORMATS.get(key);
    }

    /**
     * Static fallback for clear() when Spring context is not available.
     * For testing only!
     */
    public static synchronized void clearStatic() {
        STATIC_FORMATS = null;
    }

    private static synchronized void readStaticFormats() {
        try {
            STATIC_FORMATS = readBundledFormatsStatic();
            STATIC_FORMATS.putAll(readUserFormatsStatic());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static Map<String, Format> readBundledFormatsStatic() throws IOException {
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

    private static Map<String, Format> readUserFormatsStatic() throws IOException {
        Path pathname = getUserFormatsFilePathStatic();
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

    private static Path getUserFormatsFilePathStatic() {
        Configuration config = Configuration.getInstance();
        Optional<Path> configFile = config.getFile();
        Path dir = configFile.isPresent() ?
                configFile.get().getParent() : Paths.get(".");
        return dir.resolve(FILENAME);
    }
}
