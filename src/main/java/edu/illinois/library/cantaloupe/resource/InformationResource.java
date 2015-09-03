package edu.illinois.library.cantaloupe.resource;

import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

public class InformationResource extends AbstractResource {

    @Get("json")
    public Representation doGet() {
        this.addHeader("Link",
                "<http://iiif.io/api/image/2/level1.json>;rel=\"profile\"");

        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = (String) attrs.get("identifier");

        StringRepresentation rep = new StringRepresentation(
                getJsonRepresentation().toString());
        rep.setMediaType(new MediaType("application/json"));
        return rep;
    }

    private JSONObject getJsonRepresentation() {
        JSONObject obj = new JSONObject();
        obj.put("@context", "http://iiif.io/api/image/2/context.json");
        obj.put("@id", "http://TODO:");
        obj.put("protocol", "http://iiif.io/api/image");
        obj.put("width", new Integer(1000));
        obj.put("height", new Integer(1000));

        JSONArray sizes = new JSONArray();
        JSONObject size = new JSONObject();
        size.put("width", new Integer(150));
        size.put("height", new Integer(150));
        sizes.put(size);
        size = new JSONObject();
        size.put("width", new Integer(300));
        size.put("height", new Integer(300));
        sizes.put(size);
        obj.put("sizes", sizes);

        JSONArray tiles = new JSONArray();
        JSONObject tile = new JSONObject();
        tile.put("width", new Integer(512));
        JSONArray scaleFactors = new JSONArray();
        scaleFactors.put(1);
        scaleFactors.put(2);
        scaleFactors.put(4);
        scaleFactors.put(8);
        scaleFactors.put(16);
        tile.put("scaleFactors", scaleFactors);
        tiles.put(tile);
        obj.put("tiles", tiles);

        JSONArray profiles = new JSONArray();
        profiles.put("http://iiif.io/api/image/2/level2.json");
        JSONObject profile = new JSONObject();
        JSONArray formats = new JSONArray();
        formats.put("gif");
        formats.put("pdf");
        profile.put("formats", formats);
        JSONArray qualities = new JSONArray();
        qualities.put("color");
        qualities.put("gray");
        profile.put("qualities", qualities);
        JSONArray supports = new JSONArray();
        supports.put("canonicalLinkHeader");
        supports.put("rotationArbitrary");
        supports.put("profileLinkHeader");
        supports.put("http://example.org/feature/");
        profile.put("supports", supports);
        profiles.put(profile);
        obj.put("profile", profiles);

        JSONObject service = new JSONObject();
        service.put("@context", "http://example.org/");
        service.put("profile", "http://example.org");
        service.put("physicalScale", 0.0025);
        service.put("physicalUnits", "in");
        obj.put("service", service);

        return obj;
    }

}
