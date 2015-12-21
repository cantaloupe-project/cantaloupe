package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.script.ScriptUtil;
import eu.medsea.mimeutil.MimeUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class FilesystemResolver extends AbstractResolver
        implements FileResolver, StreamResolver {

    private static Logger logger = LoggerFactory.
            getLogger(FilesystemResolver.class);

    public static final String LOOKUP_SCRIPT_CONFIG_KEY =
            "FilesystemResolver.ScriptLookupStrategy.script";
    public static final String LOOKUP_STRATEGY_CONFIG_KEY =
            "FilesystemResolver.lookup_strategy";
    public static final String PATH_PREFIX_CONFIG_KEY =
            "FilesystemResolver.BasicLookupStrategy.path_prefix";
    public static final String PATH_SEPARATOR_CONFIG_KEY =
            "FilesystemResolver.path_separator";
    public static final String PATH_SUFFIX_CONFIG_KEY =
            "FilesystemResolver.BasicLookupStrategy.path_suffix";

    private static final Set<String> SUPPORTED_SCRIPT_EXTENSIONS =
            new HashSet<>();

    static {
        MimeUtil.registerMimeDetector(
                "eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
        SUPPORTED_SCRIPT_EXTENSIONS.add("rb");
    }

    /**
     * Passes the given identifier to a function in the given script.
     *
     * @param identifier
     * @param script
     * @return Pathname of the image file corresponding to the given identifier,
     * as reported by the lookup script, or null.
     * @throws IOException If the lookup script configuration key is undefined
     * @throws ScriptException If the script failed to execute
     * @throws ScriptException If the script is of an unsupported type
     * @throws FileNotFoundException If the script returned null
     */
    public Object executeLookupScript(Identifier identifier, File script)
            throws IOException, ScriptException {
        final String extension = FilenameUtils.getExtension(script.getName());

        if (SUPPORTED_SCRIPT_EXTENSIONS.contains(extension)) {
            logger.debug("Using lookup script: {}", script);
            switch (extension) {
                case "rb":
                    final ScriptEngine engine = ScriptEngineFactory.
                            getScriptEngine("jruby");
                    final long msec = System.currentTimeMillis();
                    engine.load(FileUtils.readFileToString(script));
                    final String[] args = { identifier.toString() };
                    final Object result = engine.invoke("get_pathname", args);
                    logger.debug("Lookup function load+exec time: {} msec",
                            System.currentTimeMillis() - msec);
                    if (result == null) {
                        throw new FileNotFoundException(
                                "Lookup script returned nil for " + identifier);
                    }
                    return result;
            }
        }
        throw new ScriptException("Unsupported script type: " + extension);
    }

    @Override
    public File getFile(Identifier identifier) throws IOException {
        File file = new File(getPathname(identifier, File.separator));
        try {
            checkAccess(file, identifier);
            logger.debug("Resolved {} to {}", identifier,
                    file.getAbsolutePath());
        } catch (FileNotFoundException | AccessDeniedException e) {
            logger.info(e.getMessage());
            throw e;
        }
        return file;
    }

    @Override
    public InputStream getInputStream(Identifier identifier)
            throws IOException {
        return new BufferedInputStream(new FileInputStream(getFile(identifier)));
    }

    /**
     * Gets the pathname corresponding to the given identifier according to the
     * current lookup strategy ({@link #LOOKUP_STRATEGY_CONFIG_KEY}) in the
     * application configuration.
     *
     * @param identifier
     * @param fileSeparator Return value of {@link File#separator}
     * @return
     * @throws IOException
     */
    public String getPathname(Identifier identifier, String fileSeparator)
            throws IOException {
        identifier = replacePathSeparators(identifier, fileSeparator);

        final Configuration config = Application.getConfiguration();
        switch (config.getString(LOOKUP_STRATEGY_CONFIG_KEY)) {
            case "BasicLookupStrategy":
                return getPathnameWithBasicStrategy(identifier, fileSeparator);
            case "ScriptLookupStrategy":
                try {
                    return getPathnameWithScriptStrategy(identifier);
                } catch (ScriptException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                throw new IOException(LOOKUP_STRATEGY_CONFIG_KEY +
                        " is invalid or not set");
        }
    }

    private String getPathnameWithBasicStrategy(final Identifier identifier,
                                                final String fileSeparator) {
        final Configuration config = Application.getConfiguration();
        final String prefix = config.getString(PATH_PREFIX_CONFIG_KEY, "");
        final String suffix = config.getString(PATH_SUFFIX_CONFIG_KEY, "");
        final Identifier sanitizedId = sanitize(identifier, fileSeparator);
        return prefix + sanitizedId.toString() + suffix;
    }

    /**
     * @param identifier
     * @return
     * @throws FileNotFoundException If a script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     * @throws ScriptException If the script is of an unsupported type
     */
    private String getPathnameWithScriptStrategy(Identifier identifier)
            throws IOException, ScriptException {
        final Configuration config = Application.getConfiguration();
        // The script name may be an absolute path or a filename.
        final String scriptValue = config.
                getString(LOOKUP_SCRIPT_CONFIG_KEY);
        File script = ScriptUtil.findScript(scriptValue);
        if (!script.exists()) {
            throw new FileNotFoundException("Does not exist: " +
                    script.getAbsolutePath());
        }
        return (String) executeLookupScript(identifier, script);
    }

    @Override
    public SourceFormat getSourceFormat(Identifier identifier)
            throws IOException {
        SourceFormat sourceFormat = ResolverUtil.inferSourceFormat(identifier);
        if (sourceFormat.equals(SourceFormat.UNKNOWN)) {
            File file = new File(getPathname(identifier, File.separator));
            checkAccess(file, identifier);
            sourceFormat = detectSourceFormat(identifier);
        }
        return sourceFormat;
    }

    private void checkAccess(File file, Identifier identifier)
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
     * @param identifier
     * @return Inferred source format, or {@link SourceFormat#UNKNOWN} if
     * unknown.
     * @throws IOException
     */
    private SourceFormat detectSourceFormat(Identifier identifier)
            throws IOException {
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        String pathname = getPathname(identifier, File.separator);
        Collection<?> detectedTypes = MimeUtil.getMimeTypes(pathname);
        if (detectedTypes.size() > 0) {
            String detectedType = detectedTypes.toArray()[0].toString();
            sourceFormat = SourceFormat.
                    getSourceFormat(new MediaType(detectedType));
        }
        return sourceFormat;
    }

    /**
     * Some web servers have issues dealing with encoded slashes (%2F) in URL
     * identifiers. This method enables the use of an alternate string as a
     * path separator via {@link #PATH_SEPARATOR_CONFIG_KEY}.
     * #
     * @param identifier
     * @param fileSeparator
     * @return
     */
    private Identifier replacePathSeparators(final Identifier identifier,
                                             final String fileSeparator) {
        final String separator = Application.getConfiguration().
                getString(PATH_SEPARATOR_CONFIG_KEY, "");
        if (separator.length() > 0) {
            return ResolverUtil.replacePathSeparators(identifier, separator,
                    fileSeparator);
        }
        return identifier;
    }

    /**
     * Filters out "fileseparator.." and "..fileseparator" to prevent arbitrary
     * directory traversal.
     *
     * @param identifier
     * @param fileSeparator Return value of {@link File#separator}
     * @return
     */
    private Identifier sanitize(final Identifier identifier,
                                final String fileSeparator) {
        String idStr = identifier.toString();
        idStr = StringUtils.replace(idStr, fileSeparator + "..", "");
        idStr = StringUtils.replace(idStr, ".." + fileSeparator, "");
        return new Identifier(idStr);
    }

}
