package edu.illinois.library.cantaloupe.image.xmp;

import edu.illinois.library.cantaloupe.Application;

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
     * element within an RDF/XML XMP string.
     */
    public static String trimXMP(String xmp) {
        final int start = xmp.indexOf("<rdf:RDF");
        final int end = xmp.indexOf("</rdf:RDF");
        if (start > -1 && end > -1) {
            xmp = xmp.substring(start, end + 10);
        }
        return xmp;
    }

}
