package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.util.Rational;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Wraps a {@link RequestContext}, adapting it for use by a {@link
 * JavaDelegate}.
 *
 * @since 5.0
 */
public final class JavaRequestContext implements JavaContext {

    private final RequestContext backingContext;

    private static Map<String,Integer> toMap(Dimension size) {
        return Map.of("width", size.intWidth(), "height", size.intHeight());
    }

    public JavaRequestContext(RequestContext backingContext) {
        this.backingContext = backingContext;
    }

    public String getClientIPAddress() {
        return backingContext.getClientIP();
    }

    public Map<String,String> getCookies() {
        Map<String,String> cookies = backingContext.getCookies();
        return (cookies != null) ?
                Collections.unmodifiableMap(cookies) : Collections.emptyMap();
    }

    public Map<String,Integer> getFullSize() {
        Dimension fullSize = backingContext.getFullSize();
        return (fullSize != null) ? toMap(fullSize) : null;
    }

    public String getIdentifier() {
        Identifier identifier = backingContext.getIdentifier();
        return (identifier != null) ? identifier.toString() : null;
    }

    public String getLocalURI() {
        URI uri = backingContext.getLocalURI();
        return (uri != null) ? uri.toString() : null;
    }

    public Map<String,Object> getMetadata() {
        Metadata metadata = backingContext.getMetadata();
        return (metadata != null) ? metadata.toMap() : null;
    }

    public List<Map<String,Object>> getOperations() {
        OperationList opList = backingContext.getOperationList();
        if (opList != null) {
            //noinspection unchecked
            return (List<Map<String, Object>>) backingContext.getOperationList()
                    .toMap(backingContext.getFullSize())
                    .get("operations");
        }
        return Collections.emptyList();
    }

    public String getOutputFormat() {
        Format format = backingContext.getOutputFormat();
        if (format != null) {
            return format.getPreferredMediaType().toString();
        }
        return null;
    }

    public Map<String,String> getRequestHeaders() {
        Map<String,String> headers = backingContext.getRequestHeaders();
        return (headers != null) ?
                Collections.unmodifiableMap(headers) : Collections.emptyMap();
    }

    public String getRequestURI() {
        URI uri = backingContext.getRequestURI();
        return (uri != null) ? uri.toString() : null;
    }

    public Map<String,Integer> getResultingSize() {
        Dimension size = backingContext.getResultingSize();
        return (size != null) ? toMap(size) : null;
    }

    public int[] getScaleConstraint() {
        ScaleConstraint constraint = backingContext.getScaleConstraint();
        if (constraint != null) {
            Rational rational = constraint.getRational();
            return new int[]{
                    (int) rational.getNumerator(),
                    (int) rational.getDenominator()};
        }
        return null;
    }

}

