package edu.illinois.library.cantaloupe.http;

import java.util.Arrays;
import java.util.Objects;

/**
 * <p>Immutable class encapsulating a cookie.</p>
 *
 * <p>Instances are typically created from client input, and not used for
 * output. Therefore, compatibility is preferred over strictness. In practice
 * this means that any UTF-8 character is allowed in the name or value except
 * {@literal ;} or {@literal =}.</p>
 *
 * @see <a href="http://www.ietf.org/rfc/rfc6265.txt">RFC 6265: HTTP State
 *   Management Mechanism</a>
 */
public final class Cookie {

    private String name, value;

    /**
     * @param name  May be empty but not {@code null}.
     * @param value May be empty but not {@code null}.
     * @throws IllegalArgumentException if either argument is {@code null}.
     */
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
        return Arrays.hashCode(new String[] { getName(), getValue() });
    }

    private void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        } else if (name.contains(";") || name.contains("=")) {
            throw new IllegalArgumentException("Name contains illegal characters");
        }
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    private void setValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value is null");
        } else if (value.contains(";") || value.contains("=")) {
            throw new IllegalArgumentException("Value contains illegal characters");
        }
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getName() != null && !getName().isBlank()) {
            builder.append(getName());
            builder.append("=");
        }
        if (getValue() != null) {
            builder.append(getValue());
        }
        return builder.toString();
    }

}
