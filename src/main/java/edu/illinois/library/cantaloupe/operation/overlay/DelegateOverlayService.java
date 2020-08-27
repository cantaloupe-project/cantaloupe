package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

final class DelegateOverlayService implements OverlayService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateOverlayService.class);

    private final DelegateProxy delegateProxy;

    DelegateOverlayService(DelegateProxy delegateProxy) {
        this.delegateProxy = delegateProxy;
    }

    @Override
    public boolean isAvailable() {
        return DelegateProxyService.isDelegateAvailable();
    }

    /**
     * @return Map with {@literal inset}, {@literal position}, and {@literal
     *         pathname} or {@literal string} keys; or {@literal null}
     */
    @Override
    public Overlay newOverlay() throws ScriptException {
        final Map<String,Object> defs = overlayProperties(delegateProxy);
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
                String string = (String) defs.get("string");

                Map<TextAttribute, Object> attributes = Map.of(
                        TextAttribute.FAMILY, defs.get("font"),
                        TextAttribute.SIZE, defs.get("font_size"),
                        TextAttribute.WEIGHT, defs.get("font_weight"),
                        TextAttribute.TRACKING, defs.get("glyph_spacing"));
                Font font = Font.getFont(attributes);

                Color backgroundColor =
                        Color.fromString((String) defs.get("background_color"));
                Color color =
                        Color.fromString((String) defs.get("color"));
                int minSize =
                        ((Long) defs.get("font_min_size")).intValue();
                Color strokeColor =
                        Color.fromString((String) defs.get("stroke_color"));
                float strokeWidth =
                        Float.parseFloat(defs.get("stroke_width").toString());
                boolean wordWrap = (boolean) defs.get("word_wrap");

                return new StringOverlay(string, position, inset, font, minSize,
                        color, backgroundColor, strokeColor, strokeWidth,
                        wordWrap);
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
     * @param proxy
     * @return Map with one of the above structures, or {@literal null} for
     *         no overlay.
     */
    private Map<String,Object> overlayProperties(DelegateProxy proxy)
            throws ScriptException {
        final Map<String,Object> resultMap = proxy.getOverlayProperties();

        if (resultMap.isEmpty()) {
            return null;
        }

        // Copy the map into a new one that we can tweak before returning.
        final Map<String,Object> props = new HashMap<>(resultMap);
        if (props.get("pathname") != null) {
            props.put("pathname", new File((String) props.get("pathname")));
        }
        props.put("position",
                Position.fromString((String) props.get("position")));
        return props;
    }

}
