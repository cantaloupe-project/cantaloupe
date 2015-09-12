package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.apache.commons.configuration.Configuration;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class HttpResolver implements Resolver {

    private static Logger logger = LoggerFactory.getLogger(HttpResolver.class);

    private static Client client = new Client(new Context(), Protocol.HTTP);

    public ImageInputStream getInputStream(String identifier)
            throws IOException {
        Configuration config = Application.getConfiguration();
        Reference url = getUrl(identifier);
        logger.debug("Resolved {} to {}", identifier, url);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);

        // set up HTTP Basic authentication
        String username = config.getString("HttpResolver.username", "");
        String password = config.getString("HttpResolver.password", "");
        if (username.length() > 0 && password.length() > 0) {
            resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC,
                    username, password);
        }
        try {
            return ImageIO.createImageInputStream(resource.get().getStream());
        } catch (ResourceException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    /**
     * Returns the format of the image corresponding to the given identifier.
     *
     * @param identifier IIIF identifier.
     * @return A source format, or <code>SourceFormat.UNKNOWN</code> if unknown.
     */
    public SourceFormat getSourceFormat(String identifier) {
        SourceFormat format = getSourceFormatFromIdentifier(identifier);
        if (format == SourceFormat.UNKNOWN) {
            format = getSourceFormatFromServer(identifier);
        }
        return format;
    }

    public Reference getUrl(String identifier) {
        Configuration config = Application.getConfiguration();
        String prefix = config.getString("HttpResolver.url_prefix");
        if (prefix == null) {
            prefix = "";
        }
        String suffix = config.getString("HttpResolver.url_suffix");
        if (suffix == null) {
            suffix = "";
        }
        return new Reference(prefix + identifier + suffix);
    }

    /**
     * @param identifier
     * @return A source format, or <code>SourceFormat.UNKNOWN</code> if unknown.
     */
    private SourceFormat getSourceFormatFromIdentifier(String identifier) {
        // try to get the source format based on a filename extension in the
        // identifier
        identifier = identifier.toLowerCase();
        String extension = null;
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        int i = identifier.lastIndexOf('.');
        if (i > 0) {
            extension = identifier.substring(i + 1);
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
    private SourceFormat getSourceFormatFromServer(String identifier) {
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
