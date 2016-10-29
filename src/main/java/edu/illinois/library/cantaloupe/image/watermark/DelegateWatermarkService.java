package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
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
    Map<String,Object> getWatermarkProperties(
            OperationList opList, Dimension fullSize, URL requestUrl,
            Map<String,String> requestHeaders, String clientIp,
            Map<String,String> cookies)
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final Map<String,Integer> fullSizeArg = new HashMap<>();
        fullSizeArg.put("width", fullSize.width);
        fullSizeArg.put("height", fullSize.height);

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
        map.put("file", new File((String) map.get("pathname")));
        map.remove("pathname");
        map.put("position", Position.fromString((String) map.get("position")));
        return map;
    }

}
