package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.util.Rational;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unmodifiable {@link Map} interface to a {@link RequestContext}. Changes to
 * the backing {@link RequestContext} are reflected immediately by the
 * accessors.
 *
 * @since 5.0
 */
final class RequestContextMap<K, V> implements Map<K, V> {

    static final String CLIENT_IP_KEY        = "client_ip";
    static final String COOKIES_KEY          = "cookies";
    static final String FULL_SIZE_KEY        = "full_size";
    static final String IDENTIFIER_KEY       = "identifier";
    static final String LOCAL_URI_KEY        = "local_uri";
    static final String PAGE_COUNT_KEY       = "page_count";
    static final String PAGE_NUMBER_KEY      = "page_number";
    static final String METADATA_KEY         = "metadata";
    static final String OPERATIONS_KEY       = "operations";
    static final String OUTPUT_FORMAT_KEY    = "output_format";
    static final String REQUEST_HEADERS_KEY  = "request_headers";
    static final String REQUEST_URI_KEY      = "request_uri";
    static final String RESULTING_SIZE_KEY   = "resulting_size";
    static final String SCALE_CONSTRAINT_KEY = "scale_constraint";

    private final RequestContext backingContext;

    RequestContextMap(RequestContext backingContext) {
        this.backingContext = backingContext;
    }

    @Override
    public void clear() {
        unmodifiable();
    }

    @Override
    public boolean containsKey(Object key) {
        return (get(key) != null);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        switch ((String) key) {
            case CLIENT_IP_KEY:
                return (V) backingContext.getClientIP();
            case COOKIES_KEY:
                Map<String,String> cookies = backingContext.getCookies();
                return (cookies != null) ?
                        (V) Collections.unmodifiableMap(backingContext.getCookies()) :
                        null;
            case FULL_SIZE_KEY:
                Dimension size = backingContext.getFullSize();
                return (size != null) ? (V) toMap(size) : null;
            case IDENTIFIER_KEY:
                Identifier identifier = backingContext.getIdentifier();
                return (identifier != null) ? (V) identifier.toString() : null;
            case LOCAL_URI_KEY:
                URI uri = backingContext.getLocalURI();
                return (uri != null) ? (V) uri.toString() : null;
            case METADATA_KEY:
                Metadata metadata = backingContext.getMetadata();
                return (metadata != null) ? (V) metadata.toMap() : null;
            case OPERATIONS_KEY:
                OperationList opList = backingContext.getOperationList();
                return (opList != null) ?
                        (V) opList.toMap(backingContext.getFullSize()).get("operations") : null;
            case OUTPUT_FORMAT_KEY:
                Format format = backingContext.getOutputFormat();
                return (format != null) ?
                        (V) format.getPreferredMediaType().toString() : null;
            case PAGE_COUNT_KEY:
                return (V) backingContext.getPageCount();
            case PAGE_NUMBER_KEY:
                return (V) backingContext.getPageNumber();
            case REQUEST_HEADERS_KEY:
                Map<String,String> headers = backingContext.getRequestHeaders();
                return (headers != null) ?
                        (V) Collections.unmodifiableMap(backingContext.getRequestHeaders()) : null;
            case REQUEST_URI_KEY:
                uri = backingContext.getRequestURI();
                return (uri != null) ? (V) uri.toString() : null;
            case RESULTING_SIZE_KEY:
                size = backingContext.getResultingSize();
                return (size != null) ? (V) toMap(size) : null;
            case SCALE_CONSTRAINT_KEY:
                ScaleConstraint constraint = backingContext.getScaleConstraint();
                if (constraint != null) {
                    Rational rational = constraint.getRational();
                    return (V) List.of(
                            rational.getNumerator(),
                            rational.getDenominator());
                }
                return null;
            default:
                return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V put(K key, V value) {
        unmodifiable();
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        unmodifiable();
    }

    @Override
    public V remove(Object key) {
        unmodifiable();
        return null;
    }

    @Override
    public int size() {
        int size = (backingContext.getClientIP() != null)     ? 1 : 0;
        size += (backingContext.getCookies() != null)         ? 1 : 0;
        size += (backingContext.getFullSize() != null)        ? 1 : 0;
        size += (backingContext.getIdentifier() != null)      ? 1 : 0;
        size += (backingContext.getLocalURI() != null)        ? 1 : 0;
        size += (backingContext.getMetadata() != null)        ? 1 : 0;
        size += (backingContext.getOperationList() != null)   ? 1 : 0;
        size += (backingContext.getOutputFormat() != null)    ? 1 : 0;
        size += (backingContext.getPageCount() != null)       ? 1 : 0;
        size += (backingContext.getPageNumber() != null)      ? 1 : 0;
        size += (backingContext.getRequestHeaders() != null)  ? 1 : 0;
        size += (backingContext.getRequestURI() != null)      ? 1 : 0;
        size += (backingContext.getResultingSize() != null)   ? 1 : 0;
        size += (backingContext.getScaleConstraint() != null) ? 1 : 0;
        return size;
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    private Map<String,Integer> toMap(Dimension size) {
        return Map.of("width", size.intWidth(), "height", size.intHeight());
    }

    private void unmodifiable() {
        throw new UnsupportedOperationException("This implementation is " +
                "backed by a " + RequestContext.class.getSimpleName() +
                " and is unmodifiable");
    }

}
