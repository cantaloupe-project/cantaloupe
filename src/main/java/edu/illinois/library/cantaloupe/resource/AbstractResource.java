package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import org.restlet.data.Header;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public abstract class AbstractResource extends ServerResource {

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        // override the Server header
        this.getServerInfo().setAgent("Cantaloupe/" + Application.getVersion());
    }

    /**
     * Convenience method that adds a response header.
     *
     * @param key Header key
     * @param value Header value
     */
    @SuppressWarnings({"unchecked"})
    protected void addHeader(String key, String value) {
        Series<Header> responseHeaders = (Series<Header>) getResponse().
                getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Series(Header.class);
            getResponse().getAttributes().
                    put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add(new Header(key, value));
    }

}
