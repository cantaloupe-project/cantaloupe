package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.ColorUtil;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;

import javax.script.ScriptException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
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

                final Map<TextAttribute, Object> attributes = new HashMap<>();
                attributes.put(TextAttribute.FAMILY, defs.get("font"));
                attributes.put(TextAttribute.SIZE, defs.get("font_size"));
                attributes.put(TextAttribute.WEIGHT, defs.get("font_weight"));
                attributes.put(TextAttribute.TRACKING, defs.get("glyph_spacing"));
                final Font font = Font.getFont(attributes);

                final Color backgroundColor =
                        ColorUtil.fromString((String) defs.get("background_color"));
                final Color color =
                        ColorUtil.fromString((String) defs.get("color"));
                final int minSize =
                        ((Long) defs.get("font_min_size")).intValue();
                final Color strokeColor =
                        ColorUtil.fromString((String) defs.get("stroke_color"));
                final float strokeWidth =
                        Float.parseFloat(defs.get("stroke_width").toString());

                return new StringOverlay(string, position, inset, font, minSize,
                        color, backgroundColor, strokeColor, strokeWidth);
            }
        }
        return null;
    }

    /**
     * Invokes the overlay delegate method to retrieve overlay properties.
     *
     * <p>The returned map will have the following keys:</p>
     *
     * <dl>
     *     <dt>Image overlays</dt>
     *     <dd>
     *         <dl>
     *             <dt><var>inset</var></dt>
     *             <dd>Integer</dd>
     *             <dt><var>position</var></dt>
     *             <dd>{@link Position}</dd>
     *             <dt><var>pathname</var></dt>
     *             <dd>File</dd>
     *         </dl>
     *     </dd>
     *     <dt>String overlays</dt>
     *     <dd>
     *         <dl>
     *             <dt><var>background_color</var></dt>
     *             <dd>String</dd>
     *             <dt><var>color</var></dt>
     *             <dd>String</dd>
     *             <dt><var>font</var></dt>
     *             <dd>String</dd>
     *             <dt><var>font_size</var></dt>
     *             <dd>Float</dd>
     *             <dt><var>glyph_spacing</var></dt>
     *             <dd>Float</dd>
     *             <dt><var>inset</var></dt>
     *             <dd>Integer</dd>
     *             <dt><var>position</var></dt>
     *             <dd>{@link Position}</dd>
     *             <dt><var>string</var></dt>
     *             <dd>String</dd>
     *         </dl>
     *     </dd>
     * </dl>
     *
     * @param opList
     * @param fullSize
     * @param requestUrl
     * @param requestHeaders
     * @param clientIp
     * @param cookies
     * @return Map with one of the above structures, or <code>null</code> for
     *         no overlay.
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
        @SuppressWarnings("unchecked")
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
