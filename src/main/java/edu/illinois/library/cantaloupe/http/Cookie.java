package edu.illinois.library.cantaloupe.http;

import java.util.Objects;

/**
 * Immutable class encapsulating a cookie.
 */
public final class Cookie {

    private String name;
    private String value;

    public Cookie(String name, String value) {
        setName(name);
        setValue(value);
    }

    /**
     * Copy constructor.
     */
    public Cookie(Cookie cookie) {
        this(cookie.getName(), cookie.getValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Cookie) {
            Cookie other = (Cookie) obj;
            return (Objects.equals(getName(), other.getName()) &&
                    Objects.equals(getValue(), other.getValue()));
        }
        return super.equals(obj);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    private void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    private void setValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Illegal value: " + name);
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return getName() + ": " + getValue();
    }

}
