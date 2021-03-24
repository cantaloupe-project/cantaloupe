package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.util.Rational;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
@SuppressWarnings("unchecked")
final class RequestContextMap<K, V> implements Map<K, V> {

    static final String CLIENT_IP_KEY        = "client_ip";
    static final String COOKIES_KEY          = "cookies";
    static final String FULL_SIZE_KEY        = "full_size";
    static final String IDENTIFIER_KEY       = "identifier";
    static final String LOCAL_URI_KEY        = "local_uri";
    static final String METADATA_KEY         = "metadata";
    static final String OPERATIONS_KEY       = "operations";
    static final String OUTPUT_FORMAT_KEY    = "output_format";
    static final String PAGE_COUNT_KEY       = "page_count";
    static final String PAGE_NUMBER_KEY      = "page_number";
    static final String REQUEST_HEADERS_KEY  = "request_headers";
    static final String REQUEST_URI_KEY      = "request_uri";
    static final String RESULTING_SIZE_KEY   = "resulting_size";
    static final String SCALE_CONSTRAINT_KEY = "scale_constraint";

    private static final Set<String> KEY_SET = Set.of(
            CLIENT_IP_KEY, COOKIES_KEY, FULL_SIZE_KEY,
            IDENTIFIER_KEY, LOCAL_URI_KEY, METADATA_KEY,
            OPERATIONS_KEY, OUTPUT_FORMAT_KEY, PAGE_COUNT_KEY,
            PAGE_NUMBER_KEY, REQUEST_HEADERS_KEY,
            REQUEST_URI_KEY, RESULTING_SIZE_KEY,
            SCALE_CONSTRAINT_KEY);

    private final RequestContext backingContext;

    RequestContextMap(RequestContext backingContext) {
        this.backingContext = backingContext;
    }

    @Override
    public void clear() {
        throwUnmodifiable();
    }

    @Override
    public boolean containsKey(Object key) {
        return map().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map().containsValue(value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(map().entrySet());
    }

    @Override
    public V get(Object key) {
        return map().get(key);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<K> keySet() {
        return (Set<K>) KEY_SET;
    }

    private Map<K, V> map() {
        final Map<K, V> map = new HashMap<>(14);
        map.put((K) CLIENT_IP_KEY, clientIP());
        map.put((K) COOKIES_KEY, cookies());
        map.put((K) FULL_SIZE_KEY, fullSize());
        map.put((K) IDENTIFIER_KEY, identifier());
        map.put((K) LOCAL_URI_KEY, localURI());
        map.put((K) METADATA_KEY, metadata());
        map.put((K) OPERATIONS_KEY, operations());
        map.put((K) OUTPUT_FORMAT_KEY, outputFormat());
        map.put((K) PAGE_COUNT_KEY, pageCount());
        map.put((K) PAGE_NUMBER_KEY, pageNumber());
        map.put((K) REQUEST_HEADERS_KEY, requestHeaders());
        map.put((K) REQUEST_URI_KEY, requestURI());
        map.put((K) RESULTING_SIZE_KEY, resultingSize());
        map.put((K) SCALE_CONSTRAINT_KEY, scaleConstraint());
        return map;
    }

    @Override
    public V put(K key, V value) {
        throwUnmodifiable();
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throwUnmodifiable();
    }

    @Override
    public V remove(Object key) {
        throwUnmodifiable();
        return null;
    }

    @Override
    public int size() {
        return map().size();
    }

    @Override
    public Collection<V> values() {
        return map().values();
    }

    private V clientIP() {
        return (V) backingContext.getClientIP();
    }

    private V cookies() {
        Map<String,String> cookies = backingContext.getCookies();
        return (cookies != null) ?
                (V) Collections.unmodifiableMap(backingContext.getCookies()) :
                null;
    }

    private V fullSize() {
        Dimension size = backingContext.getFullSize();
        return (size != null) ? (V) toMap(size) : null;
    }

    private V identifier() {
        Identifier identifier = backingContext.getIdentifier();
        return (identifier != null) ? (V) identifier.toString() : null;
    }

    private V localURI() {
        Reference uri = backingContext.getLocalURI();
        return (uri != null) ? (V) uri.toString() : null;
    }

    private V metadata() {
        Metadata metadata = backingContext.getMetadata();
        return (metadata != null) ? (V) metadata.toMap() : null;
    }

    private V operations() {
        OperationList opList = backingContext.getOperationList();
        return (opList != null) ?
                (V) opList.toMap(backingContext.getFullSize()).get("operations") : null;
    }

    private V outputFormat() {
        Format format = backingContext.getOutputFormat();
        return (format != null) ?
                (V) format.getPreferredMediaType().toString() : null;
    }

    private V pageCount() {
        return (V) backingContext.getPageCount();
    }

    private V pageNumber() {
        return (V) backingContext.getPageNumber();
    }

    private V requestHeaders() {
        Map<String,String> headers = backingContext.getRequestHeaders();
        return (headers != null) ?
                (V) Collections.unmodifiableMap(backingContext.getRequestHeaders()) : null;
    }

    private V requestURI() {
        Reference uri = backingContext.getRequestURI();
        return (uri != null) ? (V) uri.toString() : null;
    }

    private V resultingSize() {
        Dimension size = backingContext.getResultingSize();
        return (size != null) ? (V) toMap(size) : null;
    }

    private V scaleConstraint() {
        ScaleConstraint constraint = backingContext.getScaleConstraint();
        if (constraint != null) {
            Rational rational = constraint.getRational();
            return (V) List.of(
                    rational.getNumerator(),
                    rational.getDenominator());
        }
        return null;
    }

    private static Map<String,Integer> toMap(Dimension size) {
        return Map.of("width", size.intWidth(), "height", size.intHeight());
    }

    private void throwUnmodifiable() {
        throw new UnsupportedOperationException("This implementation is " +
                "backed by a " + RequestContext.class.getSimpleName() +
                " and is unmodifiable");
    }

}
