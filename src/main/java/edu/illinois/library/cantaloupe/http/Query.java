package edu.illinois.library.cantaloupe.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a collection of URI query arguments.
 */
public final class Query implements Iterable<KeyValuePair> {

    private final List<KeyValuePair> pairs = new ArrayList<>();

    public Query() {}

    public Query(String query) {
        if (!query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length > 1) {
                    add(kv[0], kv[1]);
                } else if (kv.length > 0) {
                    add(kv[0]);
                }
            }
        }
    }

    /**
     * Copy constructor.
     */
    public Query(Query query) {
        pairs.addAll(query.getAll());
    }

    public void add(String name) {
        add(name, null);
    }

    public void add(String name, String value) {
        pairs.add(new KeyValuePair(name, value));
    }

    public void clear() {
        pairs.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Query) {
            Query other = (Query) obj;
            List<KeyValuePair> allOthers = other.getAll();
            return allOthers.equals(getAll());
        }
        return super.equals(obj);
    }

    public List<KeyValuePair> getAll() {
        return new ArrayList<>(pairs);
    }

    public List<KeyValuePair> getAll(String key) {
        return pairs.stream().
                filter(kv -> kv.getKey().equals(key)).
                collect(Collectors.toList());
    }

    public String getFirstValue(String key) {
        for (KeyValuePair pair : pairs) {
            if (pair.getKey().equals(key)) {
                return pair.getValue();
            }
        }
        return null;
    }

    public String getFirstValue(String key, String defaultValue) {
        Optional<String> header = pairs.stream().
                filter(kv -> kv.getKey().equals(key)).
                map(kv -> (kv.getValue() != null && !kv.getValue().isEmpty()) ?
                        kv.getValue() : defaultValue).
                findFirst();
        return header.orElse(defaultValue);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public boolean isEmpty() {
        return pairs.isEmpty();
    }

    @Override
    public Iterator<KeyValuePair> iterator() {
        return pairs.iterator();
    }

    public void remove(String key) {
        pairs.removeIf(pair -> pair.getKey().equals(key));
    }

    /**
     * @see #clear()
     */
    public void removeAll(String name) {
        final Collection<KeyValuePair> toRemove = new ArrayList<>(pairs.size());
        for (KeyValuePair pair : pairs) {
            if (name.equals(pair.getKey())) {
                toRemove.add(pair);
            }
        }
        pairs.removeAll(toRemove);
    }

    /**
     * Replaces all headers with the given name with a single header.
     * @see #add
     */
    public void set(String name, String value) {
        removeAll(name);
        add(name, value);
    }

    public int size() {
        return pairs.size();
    }

    public Stream<KeyValuePair> stream() {
        return pairs.stream();
    }

    /**
     * @return Query as a map of key-value pairs. Multiple same-keyed
     *         arguments will be lost. Some values may be {@literal null}.
     */
    public Map<String,String> toMap() {
        Map<String,String> map = new HashMap<>();
        for (KeyValuePair pair : pairs) {
            map.put(pair.getKey(), pair.getValue());
        }
        return map;
    }

    @Override
    public String toString() {
        if (!isEmpty()) {
            return stream()
                    .map(KeyValuePair::toString)
                    .collect(Collectors.joining("&"));
        }
        return "";
    }

}
