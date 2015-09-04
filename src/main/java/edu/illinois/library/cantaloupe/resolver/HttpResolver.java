package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.ConfigurationException;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.resource.ClientResource;

import java.io.InputStream;

public class HttpResolver implements Resolver {

    public InputStream resolve(String identifier) {
        try {
            String url = getUrl(identifier);
            Client client = new Client(new Context(), Protocol.HTTP);
            ClientResource resource = new ClientResource(url);
            resource.setNext(client);
            return resource.get().getStream();
        } catch (Exception e) {
            return null;
        }
    }

    private String getUrl(String identifier) {
        return getUrlPrefix() + identifier + getUrlSuffix();
    }

    /**
     * @return URL prefix, never with a trailing slash.
     */
    private String getUrlPrefix() {
        String prefix;
        try {
            prefix = Application.getConfiguration().
                    getString("HttpResolver.url_prefix");
        } catch (ConfigurationException e) {
            return "";
        }
        return prefix;
    }

    /**
     * @return URL suffix, never with a leading slash.
     */
    private String getUrlSuffix() {
        String suffix;
        try {
            suffix = Application.getConfiguration().
                    getString("HttpResolver.url_suffix");
        } catch (ConfigurationException e) {
            return "";
        }
        return suffix;
    }

}
