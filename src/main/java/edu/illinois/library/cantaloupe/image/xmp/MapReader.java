package edu.illinois.library.cantaloupe.image.xmp;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extracts label-value pairs from an XMP model, making them available in a
 * {@link Map}.
 *
 * @author Alex Dolski UIUC
 * @since 6.0
 */
public final class MapReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MapReader.class);

    private static final Map<String,String> PREFIXES = Map.ofEntries(
            Map.entry("http://ns.adobe.com/camera-raw-settings/1.0/", "crs"),
            Map.entry("http://purl.org/dc/elements/1.1/", "dc"),
            Map.entry("http://purl.org/dc/terms/", "dcterms"),
            Map.entry("http://ns.adobe.com/exif/1.0/", "exif"),
            Map.entry("http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/", "Iptc4xmpCore"),
            Map.entry("http://ns.adobe.com/iX/1.0/", "iX"),
            Map.entry("http://ns.adobe.com/pdf/1.3/", "pdf"),
            Map.entry("http://ns.adobe.com/photoshop/1.0/", "photoshop"),
            Map.entry("http://ns.adobe.com/tiff/1.0/", "tiff"),
            Map.entry("http://ns.adobe.com/xap/1.0/", "xmp"),
            Map.entry("http://ns.adobe.com/xap/1.0/bj/", "xmpBJ"),
            Map.entry("http://ns.adobe.com/xmp/1.0/DynamicMedia/", "xmpDM"),
            Map.entry("http://ns.adobe.com/xmp/identifier/qual/1.0/", "xmpidq"),
            Map.entry("http://ns.adobe.com/xap/1.0/mm/", "xmpMM"),
            Map.entry("http://ns.adobe.com/xap/1.0/rights/", "xmpRights"),
            Map.entry("http://ns.adobe.com/xap/1.0/t/pg/", "xmpTPg"));

    private final Model model;
    private final Map<String,Object> elements = new TreeMap<>();
    private boolean hasReadElements;

    /**
     * @param xmp XMP string. {@code <rdf:RDF>} must be the root element.
     * @see Utils#trimXMP
     */
    public MapReader(String xmp) throws IOException {
        RIOT.init();
        this.model = ModelFactory.createDefaultModel();
        try (StringReader reader = new StringReader(xmp)) {
            model.read(reader, null, "RDF/XML");
        } catch (RiotException | NullPointerException e) {
            // The XMP string may be invalid RDF/XML, or there may be a bug
            // in Jena (that would be the NPE). Not much we can do.
            throw new IOException(e);
        }
    }

    /**
     * @param model XMP model, already initialized.
     */
    public MapReader(Model model) {
        this.model = model;
    }

    public Map<String,Object> readElements() throws IOException {
        if (!hasReadElements) {
            StmtIterator it = model.listStatements();
            while (it.hasNext()) {
                Statement stmt = it.next();
                //System.out.println(stmt.getSubject() + " " + stmt.getSubject().isAnon());
                //System.out.println("  " + stmt.getPredicate());
                //System.out.println("    " + stmt.getObject() + " " + stmt.getObject().isLiteral());
                //System.out.println("---------------------------");
                if (!stmt.getSubject().isAnon()) {
                    recurse(stmt);
                }
            }
            LOGGER.trace("readElements(): read {} elements", elements.size());
            hasReadElements = true;
        }
        return Collections.unmodifiableMap(elements);
    }

    private void recurse(Statement stmt) {
        recurse(stmt, null);
    }

    private void recurse(Statement stmt, String predicateOverride) {
        String predicate = stmt.getPredicate().toString();
        if (stmt.getObject().isLiteral()) {
            addElement(label(predicateOverride != null ? predicateOverride : predicate),
                    stmt.getObject().asLiteral().getValue());
        } else {
            StmtIterator it = model.listStatements(
                    stmt.getObject().asResource(), null, (RDFNode) null);
            while (it.hasNext()) {
                Statement substmt = it.next();
                predicateOverride = null;
                if (substmt.getPredicate().toString().matches("(.*)#_\\d+\\b")) {
                    predicateOverride = predicate;
                }
                recurse(substmt, predicateOverride);
            }
        }
    }

    private void addElement(String label, Object value) {
        if (elements.containsKey(label)) {
            if (elements.get(label) instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> valueList = (List<Object>) elements.get(label);
                valueList.add(value);
            } else {
                List<Object> valueList = new ArrayList<>();
                valueList.add(elements.get(label));
                valueList.add(value);
                elements.put(label, valueList);
            }
        } else {
            elements.put(label, value);
        }
    }

    private String label(String uri) {
        for (Map.Entry<String,String> entry : PREFIXES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                String prefix  = entry.getValue();
                String[] parts = uri.split("/");
                return prefix + ":" + parts[parts.length - 1];
            }
        }
        return uri;
    }

}