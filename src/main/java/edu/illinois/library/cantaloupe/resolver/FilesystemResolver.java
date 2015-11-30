package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import eu.medsea.mimeutil.MimeUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.Collection;

class FilesystemResolver implements FileResolver, StreamResolver {

    private static Logger logger = LoggerFactory.
            getLogger(FilesystemResolver.class);

    private static final String PATH_PREFIX_CONFIG_KEY =
            "FilesystemResolver.path_prefix";
    private static final String PATH_SEPARATOR_CONFIG_KEY =
            "FilesystemResolver.path_separator";
    private static final String PATH_SUFFIX_CONFIG_KEY =
            "FilesystemResolver.path_suffix";

    static {
        MimeUtil.registerMimeDetector(
                "eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
    }

    @Override
    public File getFile(Identifier identifier) throws IOException {
        File file = new File(getPathname(identifier));
        try {
            checkAccess(file, identifier);
            logger.debug("Resolved {} to {}", identifier,
                    file.getAbsolutePath());
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            throw e;
        }
        return file;
    }

    @Override
    public InputStream getInputStream(Identifier identifier)
            throws IOException {
        return new BufferedInputStream(new FileInputStream(getFile(identifier)));
    }

    public String getPathname(Identifier identifier) {
        Configuration config = Application.getConfiguration();
        String prefix = config.getString(PATH_PREFIX_CONFIG_KEY);
        if (prefix == null) {
            prefix = "";
        }
        String suffix = config.getString(PATH_SUFFIX_CONFIG_KEY);
        if (suffix == null) {
            suffix = "";
        }

        String idStr = identifier.toString();

        // Some web servers have issues dealing with encoded slashes (%2F)
        // in URL identifiers. FilesystemResolver.path_separator enables the
        // use of an alternate string as a path separator.
        String separator = config.getString(PATH_SEPARATOR_CONFIG_KEY);
        if (separator != null && separator.length() > 0) {
            idStr = StringUtils.replace(idStr, separator, File.separator);
        }

        idStr = getSanitizedIdentifier(idStr, File.separator);

        return prefix + idStr + suffix;
    }

    /**
     * Filters out "fileseparator.." and "..fileseparator" to prevent arbitrary
     * directory traversal.
     *
     * @param identifier
     * @param fileSeparator The return value of {@link File#separator}
     * @return Sanitized identifier
     */
    public String getSanitizedIdentifier(String identifier,
                                         String fileSeparator) {
        identifier = StringUtils.replace(identifier, fileSeparator + "..", "");
        identifier = StringUtils.replace(identifier, ".." + fileSeparator, "");
        return identifier;
    }

    public SourceFormat getSourceFormat(Identifier identifier)
            throws IOException {
        SourceFormat sourceFormat = getSourceFormatFromIdentifier(identifier);
        if (sourceFormat.equals(SourceFormat.UNKNOWN)) {
            File file = new File(getPathname(identifier));
            checkAccess(file, identifier);
            sourceFormat = detectSourceFormat(identifier);
        }
        return sourceFormat;
    }

    private SourceFormat getSourceFormatFromIdentifier(Identifier identifier) {
        String idStr = identifier.toString().toLowerCase();
        String extension = null;
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        int i = idStr.lastIndexOf('.');
        if (i > 0) {
            extension = idStr.substring(i + 1);
        }
        if (extension != null) {
            for (SourceFormat enumValue : SourceFormat.values()) {
                if (enumValue.getExtensions().contains(extension)) {
                    sourceFormat = enumValue;
                    break;
                }
            }
        }
        return sourceFormat;
    }

    private SourceFormat detectSourceFormat(Identifier identifier) {
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        String pathname = getPathname(identifier);
        Collection<?> detectedTypes = MimeUtil.getMimeTypes(pathname);
        if (detectedTypes.size() > 0) {
            String detectedType = detectedTypes.toArray()[0].toString();
            sourceFormat = SourceFormat.
                    getSourceFormat(new MediaType(detectedType));
        }
        return sourceFormat;
    }

    private void checkAccess(File file, Identifier identifier)
            throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("Failed to resolve " +
                    identifier + " to " + file.getAbsolutePath());
        } else if (!file.canRead()) {
            throw new AccessDeniedException("File is not readable: " +
                    file.getAbsolutePath());
        }
    }

}
