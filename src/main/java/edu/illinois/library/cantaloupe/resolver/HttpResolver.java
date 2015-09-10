package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class HttpResolver implements Resolver {

    private static Logger logger = LoggerFactory.getLogger(HttpResolver.class);

    private static Client client = new Client(new Context(), Protocol.HTTP);

    /**
     * Exists to comply with Resolver. Returns null always. Use
     * <code>getInputStream()</code> instead.
     *
     * @param identifier IIIF identifier.
     * @return Null.
     */
    public File getFile(String identifier) {
        return null;
    }

    public InputStream getInputStream(String identifier) throws IOException {
        Configuration config = Application.getConfiguration();
        Reference url = getUrl(identifier);
        logger.info("Resolved {} to {}", identifier, url);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);

        // set up HTTP Basic authentication
        String username = config.getString("HttpResolver.username", "");
        String password = config.getString("HttpResolver.password", "");
        if (username.length() > 0 && password.length() > 0) {
            resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC,
                    username, password);
        }
        return resource.get().getStream();
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

}
