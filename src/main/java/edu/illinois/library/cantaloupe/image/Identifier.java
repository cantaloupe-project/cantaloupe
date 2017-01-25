package edu.illinois.library.cantaloupe.image;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Image-server-unique image identifier.
 */
public class Identifier implements Comparable<Identifier> {

    private static final Logger logger = LoggerFactory.
            getLogger(Identifier.class);

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
        int last = this.toString().compareTo(identifier.toString());
        return (last == 0) ?
                this.toString().compareTo(identifier.toString()) : last;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Identifier) {
            return this.toString().equals(obj.toString());
        } else if (obj instanceof String) {
            return this.toString().equals(obj);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode(){
        return this.toString().hashCode();
    }

    /**
     * Identifiers have no character or length restrictions, but most
     * filesystems do. This method returns a filename-safe, length-conscious
     * string guaranteed to uniquely represent the instance.
     *
     * @return Filename-safe representation of the instance.
     */
    public String toFilename() {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(toString().getBytes(Charset.forName("UTF8")));
            return Hex.encodeHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.error("filenameFor(): {}", e.getMessage(), e);
        }
        return toString(); // This should never hit.
    }

    /**
     * @return The value of the instance.
     */
    @Override
    public String toString() {
        return this.value;
    }

}
