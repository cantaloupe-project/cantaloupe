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
        this.value = value;
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
            return this.toString().equals(obj.toString());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode(){
        return this.toString().hashCode();
    }

    /**
     * @return The value of the instance.
     */
    @Override
    public String toString() {
        return this.value;
    }

}
