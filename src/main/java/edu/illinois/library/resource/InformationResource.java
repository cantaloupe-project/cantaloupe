package edu.illinois.library.resource;

import java.util.Map;

import org.restlet.Message;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.data.Header;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

/**
 * Created by alexd on 9/1/15.
 */
public class InformationResource extends ServerResource {

    /*
    private static Series<Header> getMessageHeaders(Message message) {
        Map<String, Object> attrs = message.getAttributes();
        Series<Header> headers = (Series<Header>) attrs.
                get("org.restlet.http.headers");
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            Series<Header> prev = (Series<Header>)attrs.putIfAbsent(HEADERS_KEY, headers);
            if (prev != null) {
                headers = prev;
            }
        }
        return headers;
    }*/

    @Get
    public Representation doGet() {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = (String) attrs.get("identifier");

        //getMessageHeaders(getResponse()).add("Access-Control-Allow-Origin", "*");
        return new StringRepresentation("InformationResource");
    }

}
