package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.FileImageInputStream;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * <p>Provides access to source content located on a locally attached
 * filesystem. Identifiers are mapped to filesystem paths.</p>
 *
 * <h1>Format Determination</h1>
 *
 * <p>For images with extensions, the extension will be assumed to correctly
 * denote the image format, based on the return value of
 * {@link Format#inferFormat(Identifier)}. Images with extensions that are
 * missing or unrecognized will have their "magic number" checked to determine
 * their format, which will incur a small performance penalty. It is therefore
 * slightly more efficient to serve images with extensions.</p>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#FILESYSTEMRESOLVER_LOOKUP_STRATEGY}. BasicLookupStrategy locates
 * images by concatenating a pre-defined path prefix and/or suffix.
 * ScriptLookupStrategy invokes a delegate method to retrieve a pathname
 * dynamically.</p>
 */
class FilesystemResolver extends AbstractResolver
        implements StreamResolver, FileResolver {

    private static class FileStreamSource implements StreamSource {

        private final Path file;

        FileStreamSource(Path file) {
            this.file = file;
        }

        @Override
        public FileImageInputStream newImageInputStream() throws IOException {
            return new FileImageInputStream(file.toFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return Files.newInputStream(file);
        }

    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(FilesystemResolver.class);

    private static final String GET_PATHNAME_DELEGATE_METHOD =
            "FilesystemResolver::get_pathname";

    private static final String UNIX_PATH_SEPARATOR = "/";
    private static final String WINDOWS_PATH_SEPARATOR = "\\";

    /**
     * Lazy-loaded by {@link #getPath}.
     */
    private Path path;

    @Override
    public void checkAccess() throws IOException {
        final Path path = getPath();
        if (!Files.exists(path)) {
            throw new NoSuchFileException("Failed to resolve " +
                    identifier + " to " + path);
        } else if (!Files.isReadable(path)) {
            throw new AccessDeniedException("File is not readable: " + path);
        }
    }

    /**
     * @return Path corresponding to the given identifier according to the
     *         current lookup strategy
     *         ({@link Key#FILESYSTEMRESOLVER_LOOKUP_STRATEGY}). The result is
     *         cached.
     */
    @Override
    public Path getPath() throws IOException {
        if (path == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY);
            switch (strategy) {
                case DELEGATE_SCRIPT:
                    try {
                        path = getPathWithScriptStrategy();
                    } catch (DelegateScriptDisabledException e) {
                        LOGGER.error(e.getMessage());
                        throw new IOException(e);
                    } catch (ScriptException e) {
                        LOGGER.error(e.getMessage(), e);
                        throw new IOException(e);
                    }
                    break;
                default:
                    path = getPathWithBasicStrategy();
                    break;
            }
            LOGGER.info("Resolved {} to {}", identifier, path);
        }
        return path;
    }

    private Path getPathWithBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        final String prefix =
                config.getString(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "");
        final String suffix =
                config.getString(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");
        final Identifier sanitizedId = sanitizedIdentifier();
        return Paths.get(prefix + sanitizedId.toString() + suffix);
    }

    /**
     * @return Pathname of the file corresponding to the identifier passed to
     *         {@link #setIdentifier(Identifier)}.
     * @throws NoSuchFileException If the delegate method indicated that there
     *                               is no file corresponding to the given
     *                               identifier.
     * @throws IOException
     * @throws ScriptException If the method invocation failed.
     * @throws DelegateScriptDisabledException If the delegate script is
     *                                         disabled.
     */
    private Path getPathWithScriptStrategy() throws IOException,
            ScriptException, DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_PATHNAME_DELEGATE_METHOD,
                identifier.toString(), context.asMap());
        if (result == null) {
            throw new NoSuchFileException(GET_PATHNAME_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return Paths.get((String) result);
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            sourceFormat = Format.inferFormat(identifier);
            if (sourceFormat.equals(Format.UNKNOWN)) {
                sourceFormat = detectSourceFormat();
            }
        }
        return sourceFormat;
    }

    /**
     * Detects the source format of a file by reading its header.
     *
     * @return Detected source format, or {@link Format#UNKNOWN} if
     *         unknown.
     */
    private Format detectSourceFormat() throws IOException {
        Format format = Format.UNKNOWN;
        final Path path = getPath();
        List<MediaType> detectedTypes = MediaType.detectMediaTypes(path);
        if (detectedTypes.size() > 0) {
            format = detectedTypes.get(0).toFormat();
        }
        return format;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        return new FileStreamSource(getPath());
    }

    /**
     * Recursively filters out <code>fileseparator..</code> and
     * <code>..fileseparator</code> to prevent arbitrary directory
     * traversal.
     *
     * @return Sanitized identifier.
     */
    private Identifier sanitizedIdentifier() {
        final String sanitized = StringUtil.sanitize(
                identifier.toString(),
                UNIX_PATH_SEPARATOR + "..",
                ".." + UNIX_PATH_SEPARATOR,
                WINDOWS_PATH_SEPARATOR + "..",
                ".." + WINDOWS_PATH_SEPARATOR);
        return new Identifier(sanitized);
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        path = null;
        sourceFormat = null;
        this.identifier = identifier;
    }

}
