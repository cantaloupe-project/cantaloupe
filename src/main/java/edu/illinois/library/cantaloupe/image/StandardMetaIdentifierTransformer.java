package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Transforms meta-identifiers to or from one of the following formats:</p>
 *
 * <ul>
 *     <li>{@code Identifier}</li>
 *     <li>{@code Identifier;PageNumber}</li>
 *     <li>{@code Identifier;ScaleConstraint}</li>
 *     <li>{@code Identifier;PageNumber;ScaleConstraint}</li>
 * </ul>
 *
 * <p>(The {@code ;} character is customizable via the {@link
 * Key#STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER} configuration key.)</p>
 *
 * @since 5.0
 * @author Alex Dolski UIUC
 */
public final class StandardMetaIdentifierTransformer
        implements MetaIdentifierTransformer {

    private static final String DEFAULT_COMPONENT_DELIMITER = ";";

    private static String getComponentDelimiter() {
        Configuration config = Configuration.getInstance();
        return config.getString(
                Key.STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER,
                DEFAULT_COMPONENT_DELIMITER);
    }

    private static Pattern getReverseMetaIdentifierPattern() {
        final String separator = StringUtils.reverse(getComponentDelimiter());
        return Pattern.compile("^(?<sc>\\d+:\\d+)?" + separator +
                "?(?<page>\\d+)?" + separator + "?(?<id>.+)");
    }

    /**
     * Breaks apart the given meta-identifier into its constituent components.
     */
    @Override
    public MetaIdentifier deserialize(final String metaIdentifier) {
        // Reversing the string enables it to be easily parsed using a regex.
        // Otherwise it would be a lot harder to parse any component delimiters
        // present in the identifier portion.
        final String reversedMetaID = StringUtils.reverse(metaIdentifier);
        final Matcher matcher       = getReverseMetaIdentifierPattern().matcher(reversedMetaID);
        final MetaIdentifier.Builder builder = MetaIdentifier.builder();
        if (matcher.matches()) {
            String idStr = StringUtils.reverse(matcher.group("id"));
            builder.withIdentifier(idStr);
            if (matcher.group("page") != null) {
                String pageStr = StringUtils.reverse(matcher.group("page"));
                int pageNumber = Integer.parseInt(pageStr);
                builder.withPageNumber(pageNumber);
            }
            if (matcher.group("sc") != null) {
                String scStr = StringUtils.reverse(matcher.group("sc"));
                String[] parts = scStr.split(":");
                if (parts.length == 2) {
                    builder.withScaleConstraint(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]));
                }
            }
            return builder.build();
        }
        return builder.withIdentifier(metaIdentifier).build();
    }

    /**
     * Joins the give instance into a meta-identifier string.
     *
     * @param metaIdentifier Instance to serialize.
     */
    @Override
    public String serialize(MetaIdentifier metaIdentifier) {
        return serialize(metaIdentifier, true);
    }

    /**
     * Joins the give instance into a meta-identifier string.
     *
     * @param metaIdentifier Instance to serialize.
     * @param normalize Whether to omit redundant information (such as a page
     *                  number of 1) from the result. This is used in testing.
     */
    public String serialize(MetaIdentifier metaIdentifier, boolean normalize) {
        final String separator = getComponentDelimiter();
        final StringBuilder builder = new StringBuilder();
        builder.append(metaIdentifier.getIdentifier());
        if (metaIdentifier.getPageNumber() != null) {
            if (!normalize || metaIdentifier.getPageNumber() != 1) {
                builder.append(separator);
                builder.append(metaIdentifier.getPageNumber());
            }
        }
        if (metaIdentifier.getScaleConstraint() != null) {
            if (!normalize || metaIdentifier.getScaleConstraint().hasEffect()) {
                builder.append(separator);
                builder.append(metaIdentifier.getScaleConstraint());
            }
        }
        return builder.toString();
    }

}
