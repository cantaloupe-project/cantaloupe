package edu.illinois.library.cantaloupe.image;

/**
 * Immutable image-server-unique source image identifier.
 */
public class Identifier implements Comparable<Identifier> {

    private String value;

    /**
     * @param value Identifier value
     * @throws IllegalArgumentException If the given value is null.
     */
    public Identifier(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null.");
        }
        this.value = value;
    }

    @Override
    public int compareTo(Identifier identifier) {
        return toString().compareTo(identifier.toString());
    }

    /**
     * @param obj Instance to compare.
     * @return {@literal true} if {@literal obj} is a reference to the same
     *         instance; {@literal true} if it is a different instance with the
     *         same value; {@literal true} if it is a {@link String} instance
     *         with the same value; {@literal false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Identifier) {
            return toString().equals(obj.toString());
        } else if (obj instanceof String) {
            return toString().equals(obj);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode(){
        return toString().hashCode();
    }

    /**
     * @return Value of the instance.
     */
    @Override
    public String toString() {
        return value;
    }

}
