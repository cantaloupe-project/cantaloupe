package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.FileImageInputStream;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
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

    private static class FilesystemStreamSource implements StreamSource {

        private final File file;

        FilesystemStreamSource(File file) {
            this.file = file;
        }

        @Override
        public FileImageInputStream newImageInputStream() throws IOException {
            return new FileImageInputStream(file);
        }

        @Override
        public FileInputStream newInputStream() throws IOException {
            return new FileInputStream(file);
        }

    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(FilesystemResolver.class);

    private static final String UNIX_PATH_SEPARATOR = "/";
    private static final String WINDOWS_PATH_SEPARATOR = "\\";

    private static final String GET_PATHNAME_DELEGATE_METHOD =
            "FilesystemResolver::get_pathname";

    /**
     * Lazy-loaded by {@link #getPathname()}.
     */
    private String pathname;

    @Override
    public StreamSource newStreamSource() throws IOException {
        return new FilesystemStreamSource(getFile());
    }

    @Override
    public File getFile() throws IOException {
        final File file = new File(getPathname());
        try {
            checkAccess(file);
            LOGGER.info("Resolved {} to {}", identifier,
                    file.getAbsolutePath());
        } catch (FileNotFoundException | AccessDeniedException e) {
            LOGGER.info(e.getMessage());
            throw e;
        }
        return file;
    }

    /**
     * @return Pathname corresponding to the given identifier according to the
     *         current lookup strategy
     *         ({@link Key#FILESYSTEMRESOLVER_LOOKUP_STRATEGY}). The result is
     *         cached.
     */
    String getPathname() throws IOException {
        if (pathname == null) {
            final Configuration config = Configuration.getInstance();
            switch (config.getString(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY)) {
                case "BasicLookupStrategy":
                    pathname = getPathnameWithBasicStrategy();
                    break;
                case "ScriptLookupStrategy":
                    try {
                        pathname = getPathnameWithScriptStrategy();
                        break;
                    } catch (DelegateScriptDisabledException e) {
                        LOGGER.error(e.getMessage());
                        throw new IOException(e);
                    } catch (ScriptException e) {
                        LOGGER.error(e.getMessage(), e);
                        throw new IOException(e);
                    }
                default:
                    throw new IOException(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY +
                            " is invalid or not set");
            }
        }
        return pathname;
    }

    private String getPathnameWithBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        final String prefix =
                config.getString(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "");
        final String suffix =
                config.getString(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");
        final Identifier sanitizedId = sanitizedIdentifier();
        return prefix + sanitizedId.toString() + suffix;
    }

    /**
     * @return Pathname of the file corresponding to the identifier passed to
     *         {@link #setIdentifier(Identifier)}.
     * @throws FileNotFoundException If the delegate method indicated that there
     *                               is no file corresponding to the given
     *                               identifier.
     * @throws IOException
     * @throws ScriptException If the method invocation failed.
     * @throws DelegateScriptDisabledException If the delegate script is
     *                                         disabled.
     */
    private String getPathnameWithScriptStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_PATHNAME_DELEGATE_METHOD,
                identifier.toString(), context.asMap());
        if (result == null) {
            throw new FileNotFoundException(GET_PATHNAME_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return (String) result;
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            final File file = new File(getPathname());
            checkAccess(file);
            sourceFormat = Format.inferFormat(identifier);
            if (sourceFormat.equals(Format.UNKNOWN)) {
                sourceFormat = detectSourceFormat();
            }
        }
        return sourceFormat;
    }

    private void checkAccess(File file)
            throws FileNotFoundException, AccessDeniedException {
        if (!file.exists()) {
            throw new FileNotFoundException("Failed to resolve " +
                    identifier + " to " + file.getAbsolutePath());
        } else if (!file.canRead()) {
            throw new AccessDeniedException("File is not readable: " +
                    file.getAbsolutePath());
        }
    }

    /**
     * Detects the source format of a file by reading its header.
     *
     * @return Detected source format, or {@link Format#UNKNOWN} if
     *         unknown.
     */
    private Format detectSourceFormat() throws IOException {
        Format format = Format.UNKNOWN;
        final File file = new File(getPathname());
        List<MediaType> detectedTypes = MediaType.detectMediaTypes(file);
        if (detectedTypes.size() > 0) {
            format = detectedTypes.get(0).toFormat();
        }
        return format;
    }

    /**
     * Recursively filters out <code>fileseparator..</code> and
     * <code>..fileseparator</code> to prevent arbitrary directory
     * traversal.
     *
     * @return Sanitized identifier.
     */
    private Identifier sanitizedIdentifier() {
        String idStr = identifier.toString();

        final String pattern1 = UNIX_PATH_SEPARATOR + "..";
        final String pattern2 = ".." + UNIX_PATH_SEPARATOR;
        final String pattern3 = WINDOWS_PATH_SEPARATOR + "..";
        final String pattern4 = ".." + WINDOWS_PATH_SEPARATOR;

        while (idStr.contains(pattern1) || idStr.contains(pattern2) ||
                idStr.contains(pattern3) || idStr.contains(pattern4)) {
            idStr = idStr.replace(pattern1, "");
            idStr = idStr.replace(pattern2, "");
            idStr = idStr.replace(pattern3, "");
            idStr = idStr.replace(pattern4, "");
        }
        return new Identifier(idStr);
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        pathname = null;
        sourceFormat = null;
        this.identifier = identifier;
    }

}
