package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.processor.Orientation;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PngMetadata extends AbstractMetadata
        implements Metadata {

    private static final Map<String,String> recognizedTags = new HashMap<>();

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
    public PngMetadata(IIOMetadata metadata, String formatName) {
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
    public List<IIOMetadataNode> getNativeMetadata() {
        final List<IIOMetadataNode> foundNodes = new ArrayList<>();
        final NodeList itxtNodes = getAsTree().getElementsByTagName("tEXt");
        for (int i = 0; i < itxtNodes.getLength(); i++) {
            final IIOMetadataNode itxtNode = (IIOMetadataNode) itxtNodes.item(i);
            final NodeList entries = itxtNode.getElementsByTagName("tEXtEntry");
            for (int j = 0; j < entries.getLength(); j++) {
                final String keyword = ((IIOMetadataNode) entries.item(j)).
                        getAttribute("keyword");
                if (recognizedTags.containsKey(keyword)) {
                    foundNodes.add((IIOMetadataNode) entries.item(j));
                }
            }
        }
        return foundNodes;
    }

    @Override
    public Orientation getOrientation() {
        final String xmpData = (String) getXmp();
        // Trim off the junk
        final int start = xmpData.indexOf("<rdf:RDF");
        final int end = xmpData.indexOf("</rdf:RDF");
        final String xmp = xmpData.substring(start, end + 10);

        final Orientation orientation = readOrientation(xmp);
        if (orientation != null) {
            return orientation;
        }
        return Orientation.ROTATE_0;
    }

    @Override
    public Object getXmp() {
        final NodeList itxtNodes = getAsTree().getElementsByTagName("iTXt");
        for (int i = 0; i < itxtNodes.getLength(); i++) {
            final IIOMetadataNode itxtNode = (IIOMetadataNode) itxtNodes.item(i);
            final NodeList entries = itxtNode.getElementsByTagName("iTXtEntry");
            for (int j = 0; j < entries.getLength(); j++) {
                final String keyword = ((IIOMetadataNode) entries.item(j)).
                        getAttribute("keyword");
                if ("XML:com.adobe.xmp".equals(keyword)) {
                    return ((IIOMetadataNode) entries.item(j)).
                            getAttribute("text");
                }
            }
        }
        return null;
    }

}
