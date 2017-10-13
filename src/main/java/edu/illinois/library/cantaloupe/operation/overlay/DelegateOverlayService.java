package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class DelegateOverlayService {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(DelegateOverlayService.class);

    /**
     * @param opList
     * @param fullSize
     * @param requestURI
     * @param requestHeaders
     * @param clientIP
     * @param cookies
     * @return Map with "inset", "position", and "pathname" or "string" keys;
     *         or null
     */
    Overlay getOverlay(OperationList opList,
                       Dimension fullSize,
                       URI requestURI,
                       Map<String,String> requestHeaders,
                       String clientIP,
                       Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final Map<String,Object> defs = overlayProperties(opList, fullSize,
                requestURI, requestHeaders, clientIP, cookies);
        if (defs != null) {
            final int inset = ((Long) defs.get("inset")).intValue();
            final Position position = (Position) defs.get("position");
            final String location = (String) defs.get("image");
            if (location != null) {
                try {
                    URI overlayURI;
                    // If the location in the configuration starts with a
                    // supported URI scheme, create a new URI for it.
                    // Otherwise, get its absolute path and convert that to a
                    // file: URI.
                    if (ImageOverlay.SUPPORTED_URI_SCHEMES.stream().anyMatch(location::startsWith)) {
                        overlayURI = new URI(location);
                    } else {
                        overlayURI = Paths.get(location).toUri();
                    }
                    return new ImageOverlay(overlayURI, position, inset);
                } catch (URISyntaxException e) {
                    LOGGER.error("getOverlay(): {}", e.getMessage());
                    return null;
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
                        Color.fromString((String) defs.get("background_color"));
                final Color color =
                        Color.fromString((String) defs.get("color"));
                final int minSize =
                        ((Long) defs.get("font_min_size")).intValue();
                final Color strokeColor =
                        Color.fromString((String) defs.get("stroke_color"));
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
     * @param requestURI
     * @param requestHeaders
     * @param clientIP
     * @param cookies
     * @return Map with one of the above structures, or <code>null</code> for
     *         no overlay.
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     */
    private Map<String,Object> overlayProperties(
            OperationList opList, Dimension fullSize, URI requestURI,
            Map<String,String> requestHeaders, String clientIP,
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
                requestURI.toString(),                       // request_uri
                requestHeaders,                              // request_headers
                clientIP,                                    // client_ip
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
