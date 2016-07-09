package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.processor.Orientation;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PngMetadata extends AbstractMetadata implements Metadata {

    private static final Map<String,String> recognizedTags = new HashMap<>();

    private boolean checkedForNativeMetadata = false;
    private boolean checkedForXmp = false;

    /** Cached by getNativeMetadata() */
    private List<IIOMetadataNode> nativeMetadata = new ArrayList<>();

    /** Cached by getOrientation() */
    private Orientation orientation;

    /** Cached by getXmp() */
    private String xmp;

    static {
        // These were generally taken from
        // http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PNG.html#TextualData
        recognizedTags.put("Artist", "Artist");
        recognizedTags.put("Author", "Author");
        recognizedTags.put("Comment", "Comment");
        recognizedTags.put("Copyright", "Copyright");
        recognizedTags.put("create-date", "CreateDate");
        recognizedTags.put("Creation Time", "CreationTime");
        recognizedTags.put("Description", "Description");
        recognizedTags.put("Disclaimer", "Disclaimer");
        recognizedTags.put("Document", "Document");
        recognizedTags.put("Label", "Label");
        recognizedTags.put("Make", "Make");
        recognizedTags.put("Model", "Model");
        recognizedTags.put("modify-date", "ModDate");
        recognizedTags.put("Software", "Software");
        recognizedTags.put("Source", "Source");
        recognizedTags.put("TimeStamp", "TimeStamp");
        recognizedTags.put("Title", "Title");
        recognizedTags.put("URL", "URL");
        recognizedTags.put("Warning", "Warning");
    }

    /**
     * @param metadata
     * @param formatName
     */
    PngMetadata(IIOMetadata metadata, String formatName) {
        super(metadata, formatName);
    }

    /**
     * @return Null.
     */
    @Override
    public Object getExif() {
        return null;
    }

    /**
     * @return Null.
     */
    @Override
    public Object getIptc() {
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
                    if (recognizedTags.containsKey(keyword)) {
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
            final String xmp = getXmp();
            if (xmp != null) {
                final Orientation readOrientation = readOrientation(xmp);
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
    public String getXmp() {
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
                        xmp = ((IIOMetadataNode) entries.item(j)).
                                getAttribute("text");
                        // Trim off the junk
                        final int start = xmp.indexOf("<rdf:RDF");
                        final int end = xmp.indexOf("</rdf:RDF");
                        xmp = xmp.substring(start, end + 10);
                    }
                }
            }
        }
        return xmp;
    }

}
