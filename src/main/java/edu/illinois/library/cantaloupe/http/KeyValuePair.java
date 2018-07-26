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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getKey() != null && !getKey().isEmpty()) {
            builder.append(getKey());
        }
        if (getValue() != null && !getValue().isEmpty()) {
            builder.append("=");
            builder.append(getValue());
        }
        return builder.length() > 0 ? builder.toString() : super.toString();
    }

}
