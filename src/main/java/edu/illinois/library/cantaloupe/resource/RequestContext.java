package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains information about a client request.
 */
public final class RequestContext {

    static final String CLIENT_IP_KEY        = "client_ip";
    static final String COOKIES_KEY          = "cookies";
    static final String FULL_SIZE_KEY        = "full_size";
    static final String IDENTIFIER_KEY       = "identifier";
    static final String METADATA_KEY         = "metadata";
    static final String OPERATIONS_KEY       = "operations";
    static final String OUTPUT_FORMAT_KEY    = "output_format";
    static final String REQUEST_HEADERS_KEY  = "request_headers";
    static final String REQUEST_URI_KEY      = "request_uri";
    static final String RESULTING_SIZE_KEY   = "resulting_size";
    static final String SCALE_CONSTRAINT_KEY = "scale_constraint";

    private final Map<String,Object> backingMap = new HashMap<>();

    /**
     * Sets or clears {@link #CLIENT_IP_KEY}.
     *
     * @param clientIP May be {@literal null}.
     */
    public void setClientIP(String clientIP) {
        if (clientIP != null) {
            backingMap.put(CLIENT_IP_KEY, clientIP);
        } else {
            backingMap.remove(CLIENT_IP_KEY);
        }
    }

    /**
     * Sets or clears {@link #COOKIES_KEY}.
     *
     * @param cookies May be {@literal null}.
     */
    public void setCookies(Map<String,String> cookies) {
        if (cookies != null) {
            backingMap.put(COOKIES_KEY, Collections.unmodifiableMap(cookies));
        } else {
            backingMap.remove(COOKIES_KEY);
        }
    }

    /**
     * Sets or clears {@link #IDENTIFIER_KEY}.
     *
     * @param identifier May be {@literal null}.
     */
    public void setIdentifier(Identifier identifier) {
        if (identifier != null) {
            backingMap.put(IDENTIFIER_KEY, identifier.toString());
        } else {
            backingMap.remove(IDENTIFIER_KEY);
        }
    }

    /**
     * Sets or clears {@link #METADATA_KEY}.
     *
     * @param metadata May be {@literal null}.
     */
    public void setMetadata(Metadata metadata) {
        if (metadata != null) {
            backingMap.put(METADATA_KEY, metadata.toMap());
        } else {
            backingMap.remove(METADATA_KEY);
        }
    }

    /**
     * Sets or clears {@link #FULL_SIZE_KEY}, {@link #IDENTIFIER_KEY},
     * {@link #OPERATIONS_KEY}, {@link #OUTPUT_FORMAT_KEY}, and
     * {@link #RESULTING_SIZE_KEY}.
     *
     * @param opList   May be {@literal null}.
     * @param fullSize May be {@literal null}.
     */
    public void setOperationList(OperationList opList, Dimension fullSize) {
        if (opList != null && fullSize != null) {
            backingMap.put(FULL_SIZE_KEY, toMap(fullSize));
            backingMap.put(IDENTIFIER_KEY, opList.getIdentifier().toString());
            backingMap.put(OPERATIONS_KEY,
                    opList.toMap(fullSize).get("operations"));
            backingMap.put(OUTPUT_FORMAT_KEY,
                    opList.getOutputFormat().getPreferredMediaType().toString());
            backingMap.put(RESULTING_SIZE_KEY,
                    toMap(opList.getResultingSize(fullSize)));
        } else {
            backingMap.remove(FULL_SIZE_KEY);
            backingMap.remove(IDENTIFIER_KEY);
            backingMap.remove(OPERATIONS_KEY);
            backingMap.remove(OUTPUT_FORMAT_KEY);
            backingMap.remove(RESULTING_SIZE_KEY);
        }
    }

    /**
     * Sets {@link #REQUEST_HEADERS_KEY}.
     *
     * @param requestHeaders May be {@literal null}.
     */
    public void setRequestHeaders(Map<String,String> requestHeaders) {
        if (requestHeaders != null) {
            backingMap.put(REQUEST_HEADERS_KEY,
                    Collections.unmodifiableMap(requestHeaders));
        } else {
            backingMap.remove(REQUEST_HEADERS_KEY);
        }
    }

    /**
     * Sets {@link #REQUEST_URI_KEY}.
     *
     * @param uri May be {@literal null}.
     */
    public void setRequestURI(URI uri) {
        if (uri != null) {
            backingMap.put(REQUEST_URI_KEY, uri.toString());
        } else {
            backingMap.remove(REQUEST_URI_KEY);
        }
    }

    /**
     * Sets {@link #SCALE_CONSTRAINT_KEY}.
     *
     * @param scaleConstraint May be {@literal null}.
     */
    public void setScaleConstraint(ScaleConstraint scaleConstraint) {
        if (scaleConstraint != null) {
            backingMap.put(SCALE_CONSTRAINT_KEY, List.of(
                    scaleConstraint.getRational().getNumerator(),
                    scaleConstraint.getRational().getDenominator()));
        } else {
            backingMap.remove(SCALE_CONSTRAINT_KEY);
        }
    }

    /**
     * <p>Returns a &quot;live view&quot; map representation of the instance.
     * Keys correspond to non-{@literal null} properties. Any of the keys
     * represented by the class key constants may be present, like {@link
     * #CLIENT_IP_KEY}, etc.</p>
     *
     * @return Map representation of the instance.
     */
    public Map<String,Object> toMap() {
        return backingMap;
    }

    private Map<String,Integer> toMap(Dimension size) {
        Map<String,Integer> map = new HashMap<>();
        map.put("width", size.intWidth());
        map.put("height", size.intHeight());
        return Collections.unmodifiableMap(map);
    }

}
