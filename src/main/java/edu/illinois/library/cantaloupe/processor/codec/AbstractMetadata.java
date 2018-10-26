package edu.illinois.library.cantaloupe.processor.codec;

import org.slf4j.Logger;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.nio.charset.StandardCharsets;

abstract class AbstractMetadata {

    private String formatName;
    private IIOMetadata iioMetadata;

    AbstractMetadata(IIOMetadata metadata, String format) {
        iioMetadata = metadata;
        formatName = format;
    }

    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) getIIOMetadata().getAsTree(getFormatName());
    }

    private String getFormatName() {
        return formatName;
    }

    IIOMetadata getIIOMetadata() {
        return iioMetadata;
    }

    abstract Logger getLogger();

    abstract byte[] getXMP();

    String getXMPRDF() {
        final byte[] xmpData = getXMP();
        if (xmpData != null) {
            final String xmp = new String(xmpData, StandardCharsets.UTF_8);
            final int start = xmp.indexOf("<rdf:RDF");
            final int end = xmp.indexOf("</rdf:RDF");
            return (start >= 0) ? xmp.substring(start, end + 10) : xmp;
        }
        return null;
    }

}
