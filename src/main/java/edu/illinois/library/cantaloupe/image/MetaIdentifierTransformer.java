package edu.illinois.library.cantaloupe.image;

/**
 * Converts a {@link MetaIdentifier} to and from a string.
 *
 * @since 5.0
 */
public interface MetaIdentifierTransformer {

    /**
     * Deserializes the given meta-identifier string into its component parts.
     * If the string cannot be parsed, it is set as the returned instance's
     * {@link MetaIdentifier#getIdentifier() identifier property}.
     *
     * @param metaIdentifier String to deserialize.
     */
    MetaIdentifier deserialize(String metaIdentifier);

    /**
     * @param metaIdentifier Instance to serialize.
     * @return Serialized instance.
     */
    String serialize(MetaIdentifier metaIdentifier);

}
