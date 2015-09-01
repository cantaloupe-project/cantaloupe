package edu.illinois.library;

import org.restlet.Component;
import org.restlet.data.Protocol;

public class App {

    public static void main(String[] args) throws Exception {
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, 8182);
        component.getDefaultHost().attach("/iiif",
                new ImageServerApplication());
        component.start();
    }

}
