package edu.illinois.library.cantaloupe.image;

/**
 * Image-server-unique image identifier.
 */
public class Identifier implements Comparable<Identifier> {

    private String value;

    /**
     * @param value Identifier value
     */
    public Identifier(String value) {
        this.setValue(value);
    }

    @Override
    public int compareTo(Identifier identifier) {
        int last = this.toString().compareTo(identifier.toString());
        return (last == 0) ?
                this.toString().compareTo(identifier.toString()) : last;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Identifier) {
            return this.getValue().equals(((Identifier) obj).getValue());
        } else if (obj instanceof String) {
            return this.getValue().equals(obj);
        }
        return super.equals(obj);
    }

    /**
     * @return Unencoded value
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode(){
        return this.getValue().hashCode();
    }

    /**
     * @param val Unencoded value
     */
    public void setValue(String val) {
        value = val;
    }

    /**
     * @return The value of the instance.
     */
    @Override
    public String toString() {
        return getValue();
    }

}
