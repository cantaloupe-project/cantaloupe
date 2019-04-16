package edu.illinois.library.cantaloupe.image.iptc;

public enum DataType {

    /**
     * 16-bit unsigned integer represented by a {@literal long}.
     */
    UNSIGNED_INT_16,

    /**
     * String composed only of digit characters.
     */
    DIGITS,

    /**
     * ISO 646 or ISO 4873 Default Version string, represented by a {@link
     * String}.
     */
    STRING

}
