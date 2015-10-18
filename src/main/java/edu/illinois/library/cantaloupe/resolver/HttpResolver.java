package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Identifier;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;

class HttpResolver implements StreamResolver {

    private static Logger logger = LoggerFactory.getLogger(HttpResolver.class);

    private static Client client = new Client(new Context(), Protocol.HTTP);

    @Override
    public InputStream getInputStream(Identifier identifier)
            throws IOException {
        Configuration config = Application.getConfiguration();
        Reference url = getUrl(identifier);
        logger.debug("Resolved {} to {}", identifier, url);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);

        // set up HTTP Basic authentication
        String username = config.getString("HttpResolver.auth.basic.username", "");
        String secret = config.getString("HttpResolver.auth.basic.secret", "");
        if (username.length() > 0 && secret.length() > 0) {
            resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC,
                    username, secret);
        }
        try {
            return resource.get().getStream();
        } catch (ResourceException e) {
            if (e.getStatus().equals(Status.CLIENT_ERROR_NOT_FOUND) ||
                    e.getStatus().equals(Status.CLIENT_ERROR_GONE)) {
                throw new FileNotFoundException(e.getMessage());
            } else if (e.getStatus().equals(Status.CLIENT_ERROR_FORBIDDEN)) {
                throw new AccessDeniedException(e.getMessage());
            } else {
                throw new IOException(e.getMessage());
            }
        }
    }

    public SourceFormat getSourceFormat(Identifier identifier)
            throws IOException {
        SourceFormat format = getSourceFormatFromIdentifier(identifier);
        if (format == SourceFormat.UNKNOWN) {
            format = getSourceFormatFromServer(identifier);
        }
        getInputStream(identifier); // throws IOException if not found etc.
        return format;
    }

    public Reference getUrl(Identifier identifier) {
        Configuration config = Application.getConfiguration();
        String prefix = config.getString("HttpResolver.url_prefix");
        if (prefix == null) {
            prefix = "";
        }
        String suffix = config.getString("HttpResolver.url_suffix");
        if (suffix == null) {
            suffix = "";
        }

        String idStr = identifier.toString();

        // The Image API 2.0 spec mandates the use of percent-encoded
        // identifiers. But some web servers have issues dealing with the
        // encoded slash (%2F). FilesystemResolver.path_separator enables the
        // use of an alternate string as a path separator.
        String separator = config.getString("HttpResolver.path_separator", "/");
        if (!separator.equals("/")) {
            idStr = StringUtils.replace(idStr, separator, "/");
        }
        return new Reference(prefix + idStr + suffix);
    }

    /**
     * @param identifier
     * @return A source format, or <code>SourceFormat.UNKNOWN</code> if unknown.
     */
    private SourceFormat getSourceFormatFromIdentifier(Identifier identifier) {
        // try to get the source format based on a filename extension in the
        // identifier
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

    /**
     * Issues an HTTP HEAD request and checks the Content-Type header in the
     * response to determine the source format.
     *
     * @param identifier
     * @return A source format, or <code>SourceFormat.UNKNOWN</code> if unknown.
     */
    private SourceFormat getSourceFormatFromServer(Identifier identifier) {
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        String contentType = "";
        Reference url = getUrl(identifier);
        try {
            Client client = new Client(new Context(), Protocol.HTTP);
            ClientResource resource = new ClientResource(url);
            resource.setNext(client);
            resource.head();

            contentType = resource.getResponse().getHeaders().
                    getFirstValue("Content-Type", true);
            if (contentType != null) {
                sourceFormat = SourceFormat.
                        getSourceFormat(new MediaType(contentType));
            }
        } catch (ResourceException e) {
            // nothing we can do but log it
            if (contentType.length() > 0) {
                logger.debug("Failed to determine source format based on a Content-Type of {}",
                        contentType);
            } else {
                logger.debug("Failed to determine source format (missing Content-Type at {})", url);
            }
        }
        return sourceFormat;
    }

}
