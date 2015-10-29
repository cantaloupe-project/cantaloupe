package edu.illinois.library.cantaloupe.image;

/**
 * Encapsulates the "identifier" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#identifier">IIIF Image API
 * 2.0</a>
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
        return (obj instanceof Identifier &&
                this.getValue().equals(((Identifier) obj).getValue()));
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
     * @return Unencoded value
     */
    public String toString() {
        return getValue();
    }

}
