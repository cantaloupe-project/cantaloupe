package edu.illinois.library.cantaloupe.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a collection of request or response headers.
 */
public final class Headers implements Iterable<Header> {

    private final List<Header> headers = new ArrayList<>();

    public void add(String name, String value) {
        headers.add(new Header(name, value));
    }

    public void clear() {
        headers.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Headers) {
            Headers other = (Headers) obj;
            List<Header> allOthers = other.getAll();
            return allOthers.equals(getAll());
        }
        return super.equals(obj);
    }

    public List<Header> getAll() {
        return new ArrayList<>(headers);
    }

    public List<Header> getAll(String name) {
        return headers.stream().
                filter(h -> h.getName().equals(name)).
                collect(Collectors.toList());
    }

    public String getFirstValue(String name) {
        Optional<String> header = headers.stream().
                filter(h -> h.getName().equals(name)).
                map(Header::getValue).
                findFirst();
        return header.orElse(null);
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }

    public void removeAll(String name) {
        final Collection<Header> toRemove = new ArrayList<>(headers.size());
        for (Header header : headers) {
            if (name.equals(header.getName())) {
                toRemove.add(header);
            }
        }
        headers.removeAll(toRemove);
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
        return headers.size();
    }

    public Stream<Header> stream() {
        return headers.stream();
    }

}
