package edu.illinois.library.cantaloupe.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a collection of request or response cookies.
 */
public final class Cookies implements Iterable<Cookie> {

    private static final Pattern HEADER_VALUE_PATTERN =
            Pattern.compile("(\\w+)=(\\w+)");

    private final List<Cookie> cookies = new ArrayList<>();

    /**
     * @param headerValue HTTP header value.
     * @return New instance.
     * @throws IllegalArgumentException if the given header value cannot be
     *         parsed.
     */
    public static Cookies fromHeaderValue(String headerValue) {
        final Cookies cookies = new Cookies();
        final Matcher matcher = HEADER_VALUE_PATTERN.matcher(headerValue);
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i += 2) {
                Cookie cookie = new Cookie(matcher.group(i), matcher.group(i + 1));
                cookies.add(cookie);
            }
        }
        return cookies;
    }

    public Cookies() {}

    /**
     * Copy constructor.
     */
    public Cookies(Cookies cookies) {
        cookies.forEach(other -> add(new Cookie(other)));
    }

    public void add(String name, String value) {
        add(new Cookie(name, value));
    }

    public void add(Cookie cookie) {
        cookies.add(cookie);
    }

    public void addAll(Cookies cookies) {
        cookies.forEach(this::add);
    }

    public void clear() {
        cookies.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Cookies) {
            Cookies other = (Cookies) obj;
            List<Cookie> allOthers = other.getAll();
            return allOthers.equals(getAll());
        }
        return super.equals(obj);
    }

    public List<Cookie> getAll() {
        return new ArrayList<>(cookies);
    }

    public List<Cookie> getAll(String name) {
        return cookies.stream().
                filter(h -> h.getName().equalsIgnoreCase(name)).
                collect(Collectors.toList());
    }

    public String getFirstValue(String name) {
        Optional<String> cookie = cookies.stream().
                filter(h -> h.getName().equalsIgnoreCase(name)).
                map(Cookie::getValue).
                findFirst();
        return cookie.orElse(null);
    }

    public String getFirstValue(String name, String defaultValue) {
        Optional<String> cookie = cookies.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .map(c -> (c.getValue() != null && !c.getValue().isEmpty()) ?
                        c.getValue() : defaultValue)
                .findFirst();
        return cookie.orElse(defaultValue);
    }

    @Override
    public int hashCode() {
        return stream()
                .map(Cookie::toString)
                .collect(Collectors.joining())
                .hashCode();
    }

    @Override
    public Iterator<Cookie> iterator() {
        return cookies.iterator();
    }

    public void removeAll(String name) {
        final Collection<Cookie> toRemove = new ArrayList<>(cookies.size());
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                toRemove.add(cookie);
            }
        }
        cookies.removeAll(toRemove);
    }

    /**
     * Replaces all cookies with the given name with a single cookie.
     *
     * @see #add
     */
    public void set(String name, String value) {
        removeAll(name);
        add(name, value);
    }

    public int size() {
        return cookies.size();
    }

    public Stream<Cookie> stream() {
        return cookies.stream();
    }

    /**
     * @return Cookies as a map of name-value pairs. Multiple same-named
     *         cookies will be lost.
     */
    public Map<String,String> toMap() {
        final Map<String,String> map = new HashMap<>();
        cookies.forEach(c -> map.put(c.getName(), c.getValue()));
        return map;
    }

    @Override
    public String toString() {
        if (cookies.isEmpty()) {
            return "";
        }
        return stream().map(Cookie::toString).collect(Collectors.joining("; "));
    }

}
