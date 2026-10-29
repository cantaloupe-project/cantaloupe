package edu.illinois.library.cantaloupe.image.xmp;

import edu.illinois.library.cantaloupe.Application;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandlerFactory;

public final class Utils {

    private static final String XMP_TOOLKIT = Application.getName() + " " +
            Application.getVersion();

    /**
     * Returns an XMP string encapsulated in an {@literal x:xmpmeta} element,
     * which is itself encapsulated in an {@literal xpacket} PI.
     *
     * @param xmp XMP string with an {@literal rdf:RDF} root element.
     * @return    Encapsulated XMP data packet.
     */
    public static String encapsulateXMP(String xmp) {
        final StringBuilder b = new StringBuilder();
        b.append("<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>");
        b.append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"");
        b.append(XMP_TOOLKIT);
        b.append("\">");
        b.append(xmp);
        b.append("</x:xmpmeta>");
        // Append the magic trailer
        b.append(" ".repeat(2048));
        b.append("<?xpacket end=\"r\"?>");
        return b.toString();
    }

    /**
     * Strips any enclosing tags or other content around the {@literal rdf:RDF}
     * element within an RDF/XML XMP string. The element may be self-closing
     * (i.e. {@literal <rdf:RDF .../>}).
     */
    public static String trimXMP(String xmp) {
        final int start = xmp.indexOf("<rdf:RDF");
        if (start < 0) {
            return xmp;
        }
        final int end = xmp.indexOf("</rdf:RDF");
        if (end > -1) {
            return xmp.substring(start, end + 10);
        }
        final int startTagEnd = xmp.indexOf('>', start);
        if (startTagEnd > 0 && xmp.charAt(startTagEnd - 1) == '/') {
            return xmp.substring(start, startTagEnd + 1);
        }
        return xmp;
    }

    /**
     * Parses an RDF/XML string into the given model. Unlike {@link
     * Model#read(java.io.Reader, String, String)}, parse errors are not
     * logged by Jena; they are only thrown, leaving it up to the caller to
     * decide how to handle them. (XMP embedded in images is frequently
     * invalid, e.g. containing unescaped ampersands.)
     *
     * @param model  Model to read into.
     * @param rdfXML RDF/XML string.
     * @param base   Base URI. May be {@code null}.
     * @throws RiotException if the string is not valid RDF/XML.
     */
    public static void readRDFXML(Model model,
                                  String rdfXML,
                                  String base) {
        RDFParser.fromString(rdfXML, Lang.RDFXML)
                .base(base)
                .errorHandler(ErrorHandlerFactory.errorHandlerNoLogging)
                .parse(model);
    }

}
