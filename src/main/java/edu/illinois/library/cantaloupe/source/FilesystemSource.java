package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
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
 * <p>See {@link #getFormat()}.</p>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#FILESYSTEMSOURCE_LOOKUP_STRATEGY}. BasicLookupStrategy locates
 * images by concatenating a pre-defined path prefix and/or suffix.
 * ScriptLookupStrategy invokes a delegate method to retrieve a pathname
 * dynamically.</p>
 */
class FilesystemSource extends AbstractSource
        implements StreamSource, FileSource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FilesystemSource.class);

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
     *         ({@link Key#FILESYSTEMSOURCE_LOOKUP_STRATEGY}). The result is
     *         cached.
     */
    @Override
    public Path getPath() throws IOException {
        if (path == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY);
            switch (strategy) {
                case DELEGATE_SCRIPT:
                    try {
                        path = getPathWithScriptStrategy();
                    } catch (ScriptException e) {
                        LOGGER.error(e.getMessage(), e);
                        throw new IOException(e);
                    }
                    break;
                default:
                    path = getPathWithBasicStrategy();
                    break;
            }
            LOGGER.debug("Resolved {} to {}", identifier, path);
        }
        return path;
    }

    private Path getPathWithBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        final String prefix =
                config.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "");
        final String suffix =
                config.getString(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "");
        final Identifier sanitizedId = sanitizedIdentifier();
        return Paths.get(prefix + sanitizedId.toString() + suffix);
    }

    /**
     * @return Pathname of the file corresponding to the identifier passed to
     *         {@link #setIdentifier(Identifier)}.
     * @throws NoSuchFileException if the delegate method indicated that there
     *                             is no file corresponding to the given
     *                             identifier.
     * @throws ScriptException     if the method invocation failed.
     */
    private Path getPathWithScriptStrategy() throws NoSuchFileException,
            ScriptException {
        String pathname = getDelegateProxy().getFilesystemSourcePathname();

        if (pathname == null) {
            throw new NoSuchFileException(
                    DelegateMethod.FILESYSTEMSOURCE_PATHMAME +
                    " returned nil for " + identifier);
        }
        return Paths.get(pathname);
    }

    /**
     * <ol>
     *     <li>If the file's filename contains an extension, the format is
     *     inferred from that.</li>
     *     <li>If unsuccessful, and the identifier contains an extension, the
     *     format is inferred from that.</li>
     *     <li>If unsuccessful, the format is inferred from the file's magic
     *     bytes.</li>
     * </ol>
     *
     * @return Best attempt at determining the file format.
     * @throws IOException if the magic byte check fails.
     */
    @Override
    public Format getFormat() throws IOException {
        if (format == null) {
            // Try to infer a format from the filename.
            format = Format.inferFormat(getPath().getFileName().toString());

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the identifier.
                format = Format.inferFormat(identifier);
            }

            if (Format.UNKNOWN.equals(format)) {
                // Fall back to reading the magic bytes.
                format = detectFormat();
            }
        }
        return format;
    }

    /**
     * Detects the format of a file by reading its header.
     *
     * @return Detected format, or {@link Format#UNKNOWN}.
     */
    private Format detectFormat() throws IOException {
        Format format = Format.UNKNOWN;
        final Path path = getPath();
        List<MediaType> detectedTypes = MediaType.detectMediaTypes(path);
        if (!detectedTypes.isEmpty()) {
            format = detectedTypes.get(0).toFormat();
        }
        return format;
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        return new PathStreamFactory(getPath());
    }

    /**
     * Recursively filters out {@literal fileseparator..} and
     * {@literal ..fileseparator} to prevent moving up a directory tree.
     *
     * @return Sanitized identifier.
     */
    private Identifier sanitizedIdentifier() {
        final String sanitized = StringUtils.sanitize(
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
        format = null;
        this.identifier = identifier;
    }

}
