package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import eu.medsea.mimeutil.MimeUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.AccessDeniedException;
import java.util.Collection;

class FilesystemResolver extends AbstractResolver
        implements ChannelResolver, FileResolver {

    private static Logger logger = LoggerFactory.
            getLogger(FilesystemResolver.class);

    public static final String LOOKUP_STRATEGY_CONFIG_KEY =
            "FilesystemResolver.lookup_strategy";
    public static final String PATH_PREFIX_CONFIG_KEY =
            "FilesystemResolver.BasicLookupStrategy.path_prefix";
    public static final String PATH_SUFFIX_CONFIG_KEY =
            "FilesystemResolver.BasicLookupStrategy.path_suffix";

    static {
        MimeUtil.registerMimeDetector(
                "eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
    }

    @Override
    public ReadableByteChannel getChannel(Identifier identifier)
            throws IOException {
        return new FileInputStream(getFile(identifier)).getChannel();
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
     * @throws FileNotFoundException If the delegate script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     */
    private String getPathnameWithScriptStrategy(Identifier identifier)
            throws IOException, ScriptException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String[] args = { identifier.toString() };
        final String method = "Cantaloupe::get_pathname";
        final long msec = System.currentTimeMillis();
        final Object result = engine.invoke(method, args);
        logger.debug("{} load+exec time: {} msec", method,
                System.currentTimeMillis() - msec);
        if (result == null) {
            throw new FileNotFoundException(method + " returned nil for " +
                    identifier);
        }
        return (String) result;
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
     * Filters out "fileseparator.." and "..fileseparator" to prevent arbitrary
     * directory traversal.
     *
     * @param identifier Identifier to sanitize.
     * @param fileSeparator Return value of {@link File#separator}
     * @return Sanitized identifier.
     */
    private Identifier sanitize(final Identifier identifier,
                                final String fileSeparator) {
        String idStr = identifier.toString();
        idStr = StringUtils.replace(idStr, fileSeparator + "..", "");
        idStr = StringUtils.replace(idStr, ".." + fileSeparator, "");
        return new Identifier(idStr);
    }

}
