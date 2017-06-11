package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.lang3.StringUtils;
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
 * <h3>Format Determination</h3>
 *
 * <p>For images with extensions, the extension will be assumed to correctly
 * denote the image format, based on the return value of
 * {@link Format#inferFormat(Identifier)}. Images with extensions that are
 * missing or unrecognized will have their "magic number" checked to determine
 * their format, which will incur a small performance penalty. It is therefore
 * slightly more efficient to serve images with extensions.</p>
 *
 * <h3>Lookup Strategies</h3>
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

    private static Logger logger = LoggerFactory.
            getLogger(FilesystemResolver.class);

    static final String GET_PATHNAME_DELEGATE_METHOD =
            "FilesystemResolver::get_pathname";

    @Override
    public StreamSource newStreamSource() throws IOException {
        return new FilesystemStreamSource(getFile());
    }

    @Override
    public File getFile() throws IOException {
        File file = new File(getPathname(File.separator));
        try {
            checkAccess(file);
            logger.info("Resolved {} to {}", identifier,
                    file.getAbsolutePath());
        } catch (FileNotFoundException | AccessDeniedException e) {
            logger.info(e.getMessage());
            throw e;
        }
        return file;
    }

    /**
     * Gets the pathname corresponding to the given identifier according to the
     * current lookup strategy
     * ({@link edu.illinois.library.cantaloupe.config.Key#FILESYSTEMRESOLVER_LOOKUP_STRATEGY})
     * in the application configuration.
     *
     * @param fileSeparator Return value of {@link File#separator}
     * @return
     * @throws IOException
     */
    String getPathname(String fileSeparator) throws IOException {
        final Configuration config = ConfigurationFactory.getInstance();
        switch (config.getString(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY)) {
            case "BasicLookupStrategy":
                return getPathnameWithBasicStrategy(fileSeparator);
            case "ScriptLookupStrategy":
                try {
                    return getPathnameWithScriptStrategy();
                } catch (DelegateScriptDisabledException e) {
                    logger.error(e.getMessage());
                    throw new IOException(e);
                } catch (ScriptException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                throw new IOException(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY +
                        " is invalid or not set");
        }
    }

    private String getPathnameWithBasicStrategy(final String fileSeparator) {
        final Configuration config = ConfigurationFactory.getInstance();
        final String prefix =
                config.getString(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "");
        final String suffix =
                config.getString(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");
        final Identifier sanitizedId = sanitizedIdentifier(fileSeparator);
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
                identifier.toString());
        if (result == null) {
            throw new FileNotFoundException(GET_PATHNAME_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return (String) result;
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            final File file = new File(getPathname(File.separator));
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
     * @throws IOException
     */
    private Format detectSourceFormat() throws IOException {
        Format format = Format.UNKNOWN;
        final File file = new File(getPathname(File.separator));
        List<MediaType> detectedTypes = MediaType.detectMediaTypes(file);
        if (detectedTypes.size() > 0) {
            format = detectedTypes.get(0).toFormat();
        }
        return format;
    }

    /**
     * Filters out <code>fileseparator..</code> and
     * <code>..fileseparator</code> to prevent arbitrary directory traversal.
     *
     * @param fileSeparator Return value of {@link File#separator}
     * @return Sanitized identifier.
     */
    private Identifier sanitizedIdentifier(final String fileSeparator) {
        String idStr = identifier.toString();
        idStr = StringUtils.replace(idStr, fileSeparator + "..", "");
        idStr = StringUtils.replace(idStr, ".." + fileSeparator, "");
        return new Identifier(idStr);
    }

}
