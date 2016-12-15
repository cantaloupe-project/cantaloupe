package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;

import javax.script.ScriptException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class DelegateOverlayService {

    /**
     * @param opList
     * @param fullSize
     * @param requestUrl
     * @param requestHeaders
     * @param clientIp
     * @param cookies
     * @return Map with "inset", "position", and "pathname" or "string" keys;
     *         or null
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     */
    Overlay getOverlay(OperationList opList,
                       Dimension fullSize,
                       URL requestUrl,
                       Map<String,String> requestHeaders,
                       String clientIp,
                       Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final Map<String,Object> defs = overlayProperties(
                opList, fullSize, requestUrl, requestHeaders, clientIp,
                cookies);
        if (defs != null) {
            final int inset = ((Long) defs.get("inset")).intValue();
            final Position position = (Position) defs.get("position");
            final String location = (String) defs.get("image");
            if (location != null) {
                try {
                    URL url = new URL(location);
                    return new ImageOverlay(url, position, inset);
                } catch (MalformedURLException e) {
                    return new ImageOverlay(new File(location), position, inset);
                }
            } else {
                final String string = (String) defs.get("string");
                final Font font = new Font((String) defs.get("font"),
                        Font.PLAIN,
                        ((Long) defs.get("font_size")).intValue());
                final Color color =
                        ColorUtil.fromString((String) defs.get("color"));
                final Color strokeColor =
                        ColorUtil.fromString((String) defs.get("stroke_color"));
                final float strokeWidth =
                        Float.parseFloat(defs.get("stroke_width").toString());
                return new StringOverlay(string, position, inset, font, color,
                        strokeColor, strokeWidth);
            }
        }
        return null;
    }

    /**
     * Invokes the overlay delegate method to retrieve overlay properties.
     *
     * @param opList
     * @param fullSize
     * @param requestUrl
     * @param requestHeaders
     * @param clientIp
     * @param cookies
     * @return For image overlays, a map with <var>inset</var>,
     *         <var>position</var>, and <var>pathname</var> keys. For string
     *         overlays, a map with <var>inset</var>, <var>position</var>,
     *         <var>string</var>, <var>color</var>, <var>font</var>, and
     *         <var>font_size</var> keys. <var>null</var> for no overlay.
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     */
    private Map<String,Object> overlayProperties(
            OperationList opList, Dimension fullSize, URL requestUrl,
            Map<String,String> requestHeaders, String clientIp,
            Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final Dimension resultingSize = opList.getResultingSize(fullSize);
        final Map<String,Integer> resultingSizeArg = new HashMap<>();
        resultingSizeArg.put("width", resultingSize.width);
        resultingSizeArg.put("height", resultingSize.height);

        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String method = "overlay";
        final Object result = engine.invoke(method,
                opList.getIdentifier().toString(),           // identifier
                opList.toMap(fullSize).get("operations"),    // operations
                resultingSizeArg,                            // resulting_size
                opList.toMap(fullSize).get("output_format"), // output_format
                requestUrl.toString(),                       // request_uri
                requestHeaders,                              // request_headers
                clientIp,                                    // client_ip
                cookies);                                    // cookies
        if (result == null || (result instanceof Boolean && !((Boolean) result))) {
            return null;
        }

        // The result is expected to be a map. Cast it to that and copy its
        // keys and values into a new map that we can tweak before returning.
        final Map<String,Object> resultMap = ((Map<String,Object>) result);
        final Map<String,Object> properties = new HashMap<>();

        for (final String key : resultMap.keySet()) {
            properties.put(key, resultMap.get(key));
        }
        if (properties.get("pathname") != null) {
            properties.put("pathname",
                    new File((String) properties.get("pathname")));
        }
        properties.put("position",
                Position.fromString((String) properties.get("position")));
        return properties;
    }

}
