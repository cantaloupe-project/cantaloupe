package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;

import javax.script.ScriptException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class DelegateWatermarkService {

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
    Watermark getWatermark(
            OperationList opList, Dimension fullSize, URL requestUrl,
            Map<String,String> requestHeaders, String clientIp,
            Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final Map<String,Object> defs = getWatermarkProperties(
                opList, fullSize, requestUrl, requestHeaders, clientIp,
                cookies);
        if (defs != null) {
            final int inset = ((Long) defs.get("inset")).intValue();
            final Position position = (Position) defs.get("position");
            final File image = (File) defs.get("pathname");
            if (image != null) {
                return new ImageWatermark(image, position, inset);
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
                        Float.parseFloat((String) defs.get("stroke_width"));
                return new StringWatermark(string, position, inset, font, color,
                        strokeColor, strokeWidth);
            }
        }
        return null;
    }

    /**
     * Invokes the watermark delegate method to retrieve watermark properties.
     *
     * @param opList
     * @param fullSize
     * @param requestUrl
     * @param requestHeaders
     * @param clientIp
     * @param cookies
     * @return For image watermarks, a map with <var>inset</var>,
     *         <var>position</var>, and <var>pathname</var> keys. For string
     *         watermarks, a map with <var>inset</var>, <var>position</var>,
     *         <var>string</var>, <var>color</var>, <var>font</var>, and
     *         <var>font_size</var> keys. <var>null</var> for no watermark.
     * @throws IOException
     * @throws ScriptException
     * @throws DelegateScriptDisabledException
     */
    private Map<String,Object> getWatermarkProperties(
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
        final String method = "watermark";
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
        Map<String,Object> map = (Map<String,Object>) result;
        if (map.get("pathname") != null) {
            map.put("pathname", new File((String) map.get("pathname")));
        }
        map.put("position", Position.fromString((String) map.get("position")));
        return map;
    }

}
