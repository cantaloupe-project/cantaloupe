package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Identifier;
import eu.medsea.mimeutil.MimeUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

class FilesystemResolver implements FileResolver, StreamResolver {

    static {
        MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
    }

    private static Logger logger = LoggerFactory.
            getLogger(FilesystemResolver.class);

    public File getFile(Identifier identifier) throws IOException {
        File file = new File(getPathname(identifier));
        if (!file.exists()) {
            String message = "Failed to resolve " + identifier + " to " +
                    file.getAbsolutePath();
            logger.warn(message);
            throw new FileNotFoundException(message);
        }
        logger.debug("Resolved {} to {}", identifier, file.getAbsolutePath());
        return file;
    }

    public FileImageInputStream getInputStream(Identifier identifier)
            throws IOException {
        return new FileImageInputStream(getFile(identifier));
    }

    public String getPathname(Identifier identifier) {
        Configuration config = Application.getConfiguration();
        String prefix = config.getString("FilesystemResolver.path_prefix");
        if (prefix == null) {
            prefix = "";
        }
        String suffix = config.getString("FilesystemResolver.path_suffix");
        if (suffix == null) {
            suffix = "";
        }

        String idStr = identifier.toString();

        // The Image API 2.0 spec mandates the use of percent-encoded
        // identifiers. But some web servers have issues dealing with the
        // encoded slash (%2F). FilesystemResolver.path_separator enables the
        // use of an alternate string as a path separator.
        String separator = config.getString("FilesystemResolver.path_separator",
                File.separator);
        if (!separator.equals(File.separator)) {
            idStr = StringUtils.replace(idStr, separator, File.separator);
        }
        return prefix + idStr + suffix;
    }

    /**
     * Returns the format of the image corresponding to the given identifier.
     *
     * @param identifier IIIF identifier.
     * @return A source format, or <code>SourceFormat.UNKNOWN</code> if unknown.
     */
    public SourceFormat getSourceFormat(Identifier identifier) {
        SourceFormat sourceFormat = getSourceFormatFromIdentifier(identifier);
        if (sourceFormat == SourceFormat.UNKNOWN) {
            sourceFormat = getDetectedSourceFormat(identifier);
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

    private SourceFormat getDetectedSourceFormat(Identifier identifier) {
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

}
