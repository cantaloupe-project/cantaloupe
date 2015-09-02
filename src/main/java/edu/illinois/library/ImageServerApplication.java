package edu.illinois.library;

import edu.illinois.library.resource.ImageResource;
import edu.illinois.library.resource.InformationResource;
import edu.illinois.library.resource.LandingResource;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.engine.application.CorsFilter;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by alexd on 9/1/15.
 */
public class ImageServerApplication extends Application {

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        // enable filename-extension-style content negotiation
        this.getTunnelService().setExtensionsTunnel(true);

        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_EQUALS);

        CorsFilter corsFilter = new CorsFilter(getContext(), router);
        corsFilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
        corsFilter.setAllowedCredentials(true);

        // 2.1 Image Request
        // http://iiif.io/api/image/2.0/#image-request-uri-syntax
        // {scheme}://{server}{/prefix}/{identifier}/{region}/{size}/{rotation}/{quality}.{format}
        router.attach("/{identifier}/{region}/{size}/{rotation}/{quality}",
                ImageResource.class);

        // 5 Information Request
        // http://iiif.io/api/image/2.0/#information-request
        // {scheme}://{server}{/prefix}/{identifier}/info.json
        router.attach("/{identifier}/info", InformationResource.class);

        // landing page
        router.attach("/{uri}", LandingResource.class).
                setMatchingMode(Template.MODE_STARTS_WITH);

        return corsFilter;
    }

}