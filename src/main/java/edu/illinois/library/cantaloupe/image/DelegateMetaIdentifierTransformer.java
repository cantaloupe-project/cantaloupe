package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms meta-identifiers to or from arbitrary formats using a delegate
 * method.
 *
 * @since 5.0
 * @author Alex Dolski UIUC
 */
public final class DelegateMetaIdentifierTransformer
        implements MetaIdentifierTransformer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateMetaIdentifierTransformer.class);

    private static final String IDENTIFIER_KEY       = "identifier";
    private static final String PAGE_NUMBER_KEY      = "page_number";
    private static final String SCALE_CONSTRAINT_KEY = "scale_constraint";

    private DelegateProxy delegateProxy;

    /**
     * Breaks apart the given meta-identifier into its constituent components.
     */
    @Override
    public MetaIdentifier deserialize(String metaIdentifier) {
        try {
            final Map<String, Object> result =
                    delegateProxy.deserializeMetaIdentifier(metaIdentifier);
            final MetaIdentifier.Builder builder = MetaIdentifier.builder();
            { // identifier
                builder.withIdentifier((String) result.get(IDENTIFIER_KEY));
            }
            { // page number
                Long pageNumber = (Long) result.get(PAGE_NUMBER_KEY);
                if (pageNumber != null) {
                    builder.withPageNumber(pageNumber.intValue());
                }
            }
            { // scale constraint
                @SuppressWarnings("unchecked")
                List<Long> scaleConstraint =
                        (List<Long>) result.get(SCALE_CONSTRAINT_KEY);
                if (scaleConstraint != null) {
                    builder.withScaleConstraint(
                            scaleConstraint.get(0).intValue(),
                            scaleConstraint.get(1).intValue());
                }
            }
            return builder.build();
        } catch (ScriptException e) {
            LOGGER.error("deserialize(): {}", e.getMessage());
        }
        return null;
    }

    /**
     * Joins the give instance into a meta-identifier string.
     */
    @Override
    public String serialize(MetaIdentifier metaIdentifier) {
        final Map<String,Object> map = new HashMap<>();
        { // identifier
            map.put(IDENTIFIER_KEY, metaIdentifier.getIdentifier().toString());
        }
        { // page number
            if (metaIdentifier.getPageNumber() != null) {
                map.put(PAGE_NUMBER_KEY, metaIdentifier.getPageNumber());
            }
        }
        { // scale constraint
            ScaleConstraint sc = metaIdentifier.getScaleConstraint();
            if (sc == null) {
                sc = new ScaleConstraint(1, 1);
            }
            if (sc.hasEffect()) {
                map.put(SCALE_CONSTRAINT_KEY, List.of(
                        sc.getRational().getNumerator(),
                        sc.getRational().getDenominator()));
            }
        }
        try {
            return delegateProxy.serializeMetaIdentifier(map);
        } catch (ScriptException e) {
            LOGGER.error("serialize(): {}", e.getMessage());
            return null;
        }
    }

    void setDelegateProxy(DelegateProxy delegateProxy) {
        this.delegateProxy = delegateProxy;
    }

}
