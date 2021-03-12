package edu.illinois.library.cantaloupe.http;

import java.util.Objects;

class KeyValuePair {

    private String key;
    private String value;

    KeyValuePair(String key, String value) {
        setKey(key);
        setValue(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof KeyValuePair) {
            KeyValuePair other = (KeyValuePair) obj;
            return (Objects.equals(getKey(), other.getKey()) &&
                    Objects.equals(getValue(), other.getValue()));
        }
        return super.equals(obj);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    private void setKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Illegal key: " + key);
        }
        this.key = key;
    }

    /**
     * @param value Value to set. {@literal null} values are allowed.
     */
    private void setValue(String value) {
        this.value = value;
    }

    /**
     * @return URL-encoded key-value pair.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getKey() != null && !getKey().isBlank()) {
            builder.append(Reference.encode(getKey()));
        }
        if (getValue() != null && !getValue().isBlank()) {
            builder.append("=");
            builder.append(Reference.encode(getValue()));
        }
        return builder.length() > 0 ? builder.toString() : super.toString();
    }

}
