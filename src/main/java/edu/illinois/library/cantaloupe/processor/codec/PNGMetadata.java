package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadataNode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @see <a href="http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PNG.html">
 *      PNG Tags</a>
 */
class PNGMetadata extends IIOMetadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PNGMetadata.class);

    private static final Map<String,String> RECOGNIZED_TAGS = new HashMap<>();

    private boolean checkedForNativeMetadata = false;
    private boolean checkedForXmp = false;

    /**
     * Cached by {@link #getNativeMetadata()}.
     */
    private List<IIOMetadataNode> nativeMetadata = new ArrayList<>();

    /**
     * Cached by {@link #getOrientation()}.
     */
    private Orientation orientation;

    /**
     * Cached by {@link #getXMP()}.
     */
    private String xmp;

    static {
        // These were generally taken from
        // http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PNG.html#TextualData
        RECOGNIZED_TAGS.put("Artist", "Artist");
        RECOGNIZED_TAGS.put("Author", "Author");
        RECOGNIZED_TAGS.put("Comment", "Comment");
        RECOGNIZED_TAGS.put("Copyright", "Copyright");
        RECOGNIZED_TAGS.put("create-date", "CreateDate");
        RECOGNIZED_TAGS.put("Creation Time", "CreationTime");
        RECOGNIZED_TAGS.put("Description", "Description");
        RECOGNIZED_TAGS.put("Disclaimer", "Disclaimer");
        RECOGNIZED_TAGS.put("Document", "Document");
        RECOGNIZED_TAGS.put("Label", "Label");
        RECOGNIZED_TAGS.put("Make", "Make");
        RECOGNIZED_TAGS.put("Model", "Model");
        RECOGNIZED_TAGS.put("modify-date", "ModDate");
        RECOGNIZED_TAGS.put("Software", "Software");
        RECOGNIZED_TAGS.put("Source", "Source");
        RECOGNIZED_TAGS.put("TimeStamp", "TimeStamp");
        RECOGNIZED_TAGS.put("Title", "Title");
        RECOGNIZED_TAGS.put("URL", "URL");
        RECOGNIZED_TAGS.put("Warning", "Warning");
    }

    PNGMetadata(javax.imageio.metadata.IIOMetadata metadata,
                String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return {@literal null}.
     */
    @Override
    public Object getEXIF() {
        return null;
    }

    /**
     * @return {@literal null}.
     */
    @Override
    public Object getIPTC() {
        return null;
    }

    /**
     * @return Native PNG metadata.
     */
    List<IIOMetadataNode> getNativeMetadata() {
        if (!checkedForNativeMetadata) {
            checkedForNativeMetadata = true;
            final NodeList itxtNodes = getAsTree().getElementsByTagName("tEXt");
            for (int i = 0; i < itxtNodes.getLength(); i++) {
                final IIOMetadataNode itxtNode = (IIOMetadataNode) itxtNodes.item(i);
                final NodeList entries = itxtNode.getElementsByTagName("tEXtEntry");
                for (int j = 0; j < entries.getLength(); j++) {
                    final String keyword = ((IIOMetadataNode) entries.item(j)).
                            getAttribute("keyword");
                    if (RECOGNIZED_TAGS.containsKey(keyword)) {
                        nativeMetadata.add((IIOMetadataNode) entries.item(j));
                    }
                }
            }
        }
        return nativeMetadata;
    }

    @Override
    public Orientation getOrientation() {
        if (orientation == null) {
            final String xmp = getXMP();
            if (xmp != null) {
                final Orientation readOrientation = Util.readOrientation(xmp);
                if (readOrientation != null) {
                    orientation = readOrientation;
                }
            }
            if (orientation == null) {
                orientation = Orientation.ROTATE_0;
            }
        }
        return orientation;
    }

    @Override
    public String getXMP() {
        if (!checkedForXmp) {
            checkedForXmp = true;
            final NodeList itxtNodes = getAsTree().getElementsByTagName("iTXt");
            for (int i = 0; i < itxtNodes.getLength(); i++) {
                final IIOMetadataNode itxtNode = (IIOMetadataNode) itxtNodes.item(i);
                final NodeList entries = itxtNode.getElementsByTagName("iTXtEntry");
                for (int j = 0; j < entries.getLength(); j++) {
                    final String keyword = ((IIOMetadataNode) entries.item(j)).
                            getAttribute("keyword");
                    if ("XML:com.adobe.xmp".equals(keyword)) {
                        byte[] xmpBytes = ((IIOMetadataNode) entries.item(j))
                                .getAttribute("text")
                                .getBytes(Charset.forName("UTF-8"));
                        xmp = new String(xmpBytes, StandardCharsets.UTF_8);
                        xmp = StringUtils.trimXMP(xmp);
                    }
                }
            }
        }
        return xmp;
    }

}
