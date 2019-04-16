package edu.illinois.library.cantaloupe.processor.codec.png;

import edu.illinois.library.cantaloupe.processor.codec.IIOMetadata;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadataNode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class PNGMetadata extends IIOMetadata {

    private boolean checkedForNativeMetadata, checkedForXMP;

    /**
     * Creates an instance with conventional accessors.
     */
    PNGMetadata() {
        this(null, null);
    }

    /**
     * Creates an instance whose getters ignore the setters and read from the
     * supplied arguments instead.
     */
    PNGMetadata(javax.imageio.metadata.IIOMetadata metadata,
                String formatName) {
        super(metadata, formatName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String,String>> getNativeMetadata() {
        if (nativeMetadata == null && !checkedForNativeMetadata &&
                iioMetadata != null) {
            checkedForNativeMetadata = true;
            final NodeList itxtNodes = getAsTree().getElementsByTagName("tEXt");
            if (itxtNodes.getLength() > 0) {
                nativeMetadata = new HashMap<>();
                for (int i = 0; i < itxtNodes.getLength(); i++) {
                    final IIOMetadataNode itxtNode = (IIOMetadataNode) itxtNodes.item(i);
                    final NodeList entries = itxtNode.getElementsByTagName("tEXtEntry");
                    for (int j = 0; j < entries.getLength(); j++) {
                        final IIOMetadataNode node = ((IIOMetadataNode) entries.item(j));
                        final String keyword = node.getAttribute("keyword");
                        ((Map<String,String>) nativeMetadata).put(keyword, node.getAttribute("value"));
                    }
                }
            }
        }
        return Optional.ofNullable((Map<String,String>) nativeMetadata);
    }

    @Override
    public Optional<String> getXMP() {
        if (xmp == null && !checkedForXMP && iioMetadata != null) {
            checkedForXMP = true;
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
        return Optional.ofNullable(xmp);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String,Object> map = super.toMap();
        Optional<Map<String,String>> optMetadata = getNativeMetadata();
        if (optMetadata.isPresent()) {
            Map<String,String> metadata = optMetadata.get();
            map = new HashMap<>(super.toMap());
            map.put("native", metadata);
            return Collections.unmodifiableMap(map);
        }
        return map;
    }

}
