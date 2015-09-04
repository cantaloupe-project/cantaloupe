package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.ConfigurationException;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

import java.io.InputStream;

public class HttpResolver extends AbstractResolver implements Resolver {

    private static Client client = new Client(new Context(), Protocol.HTTP);

    public InputStream resolve(String identifier) {
        try {
            Reference url = getUrl(identifier);
            ClientResource resource = new ClientResource(url);
            resource.setNext(client);

            // set up HTTP Basic authentication
            String username = getConfigurationString("HttpResolver.username");
            String password = getConfigurationString("HttpResolver.password");
            if (username.length() > 0 && password.length() > 0) {
                resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC,
                        username, password);
            }

            return resource.get().getStream();
        } catch (Exception e) {
            return null;
        }
    }

    private Reference getUrl(String identifier) {
        return new Reference(getConfigurationString("HttpResolver.url_prefix") +
                identifier + getConfigurationString("HttpResolver.url_suffix"));
    }

    private String getConfigurationString(String key) {
        String value = "";
        try {
            value = Application.getConfiguration().getString(key);
        } catch (ConfigurationException e) {
        }
        return value;
    }

}
