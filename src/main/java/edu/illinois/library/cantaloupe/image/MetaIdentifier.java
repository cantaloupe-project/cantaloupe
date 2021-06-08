package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>An {@link Identifier} is used to uniquely identify a source image file
 * or object. But a client may require more specificity than this&mdash;for
 * example, to be able to retrieve a particular page from within a multi-page
 * image, and/or at a particular scale limit. Ideally these parameters would be
 * supplied as arguments to the image request API service, but said service may
 * not support them. (This is the case for the IIIF Image API, at least,
 * through version 3.0.)</p>
 *
 * <p>Hence this class, which enables joining meta-information about an
 * identifier with the identifier itself, for utilization by an image-request
 * API that doesn't natively support such information.</p>
 *
 * <h1>Input</h1>
 *
 * <p>When meta-identifiers are supplied to the application via URIs, they must
 * go through some processing steps before they can be used (order is
 * important):</p>
 *
 * <ol>
 *     <li>URI decoding</li>
 *     <li>{@link StringUtils#decodeSlashes(String) slash decoding}</li>
 * </ol>
 *
 * <p>({@link MetaIdentifier#fromURIPathComponent(String, DelegateProxy)} will
 * handle all of this.)</p>
 *
 * <h1>Output</h1>
 *
 * <p>The input steps must be reversed for output. Note that requests can
 * supply a {@link
 * edu.illinois.library.cantaloupe.resource.AbstractResource#PUBLIC_IDENTIFIER_HEADER}
 * to suggest that the meta-identifier supplied in a URI is different from the
 * one the user agent is seeing and supplying to a reverse proxy.</p>
 *
 * <p>So, the steps for output are:</p>
 *
 * <ol>
 *     <li>Replace the URI meta-identifier with the one from {@link
 *     edu.illinois.library.cantaloupe.resource.AbstractResource#PUBLIC_IDENTIFIER_HEADER},
 *     if present</li>
 *     <li>Encode slashes</li>
 *     <li>URI encoding</li>
 * </ol>
 *
 * @since 5.0
 * @author Alex Dolski UIUC
 */
public final class MetaIdentifier {

    /**
     * Used to create new {@link MetaIdentifier} instances.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {

        private Identifier identifier;
        private Integer pageNumber;
        private int scaleConstraintNumerator, scaleConstraintDenominator;

        public Builder withIdentifier(String identifier) {
            this.identifier = new Identifier(identifier);
            return this;
        }

        public Builder withIdentifier(Identifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public Builder withScaleConstraint(int numerator, int denominator) {
            this.scaleConstraintNumerator   = numerator;
            this.scaleConstraintDenominator = denominator;
            return this;
        }

        public MetaIdentifier build() {
            MetaIdentifier metaIdentifier = new MetaIdentifier(identifier);
            metaIdentifier.setPageNumber(pageNumber);
            if (scaleConstraintNumerator != 0 && scaleConstraintDenominator != 0) {
                metaIdentifier.setScaleConstraint(new ScaleConstraint(
                        scaleConstraintNumerator,
                        scaleConstraintDenominator));
            }
            return metaIdentifier;
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetaIdentifier.class);

    private Identifier identifier;
    private Integer pageNumber;
    private ScaleConstraint scaleConstraint;
    private transient boolean isFrozen;

    public static MetaIdentifier.Builder builder() {
        return new Builder();
    }

    /**
     * <p>Deserializes the given meta-identifier string using the {@link
     * MetaIdentifierTransformer} specified in the application
     * configuration.</p>
     *
     * <p>This is a shortcut to using {@link
     * MetaIdentifierTransformerFactory}.</p>
     *
     * @return New deserialized instance.
     */
    public static MetaIdentifier fromString(String string,
                                            DelegateProxy delegateProxy) {
        final MetaIdentifierTransformer xformer =
                new MetaIdentifierTransformerFactory().newInstance(delegateProxy);
        return xformer.deserialize(string);
    }

    /**
     * Translates the string in a raw URI path component into a new instance
     * using the {@link MetaIdentifierTransformer} specified in the application
     * configuration..
     *
     * @param pathComponent Raw URI path component.
     * @param delegateProxy Delegate proxy.
     */
    public static MetaIdentifier fromURIPathComponent(String pathComponent,
                                                      DelegateProxy delegateProxy) {
        // Decode entities.
        final String decodedComponent = Reference.decode(pathComponent);
        // Decode slash substitutes.
        final String deSlashedComponent =
                StringUtils.decodeSlashes(decodedComponent);
        LOGGER.debug("[Raw path component: {}] -> " +
                        "[decoded: {}] -> [slashes substituted: {}]",
                pathComponent, decodedComponent, deSlashedComponent);
        return fromString(deSlashedComponent, delegateProxy);
    }

    /**
     * Creates a minimal valid instance. For more options, use {@link Builder}.
     */
    public MetaIdentifier(String identifier) {
        this(new Identifier(identifier));
    }

    /**
     * Creates a minimal valid instance. For more options, use {@link Builder}.
     */
    public MetaIdentifier(Identifier identifier) {
        this.setIdentifier(identifier);
    }

    /**
     * Copy constructor.
     */
    public MetaIdentifier(MetaIdentifier metaIdentifier) {
        this(metaIdentifier.getIdentifier());
        setPageNumber(metaIdentifier.getPageNumber());
        setScaleConstraint(metaIdentifier.getScaleConstraint());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof MetaIdentifier) {
            MetaIdentifier other = (MetaIdentifier) obj;
            return identifier.equals(other.identifier) &&
                    Objects.equals(pageNumber, other.pageNumber) &&
                    Objects.equals(scaleConstraint, other.scaleConstraint);
        }
        return super.equals(obj);
    }

    /**
     * Makes the instance immutable, so that future invocations of setter
     * methods will throw {@link IllegalStateException}s.
     */
    public void freeze() {
        this.isFrozen = true;
    }

    /**
     * @return Identifier. Never {@code null}.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * @return Page number. May be {@code null}.
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * @return Scale constraint. May be {@code null}.
     */
    public ScaleConstraint getScaleConstraint() {
        return scaleConstraint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, pageNumber, scaleConstraint);
    }

    /**
     * @param identifier Identifier to set. Must not be {@code null}.
     */
    public void setIdentifier(Identifier identifier) {
        checkFrozen();
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier cannot be null");
        }
        this.identifier = identifier;
    }

    /**
     * @param pageNumber Page number, greater than or equal to 1. May be
     *                   {@code null}.
     */
    public void setPageNumber(Integer pageNumber) {
        checkFrozen();
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be >= 1");
        }
        this.pageNumber = pageNumber;
    }

    /**
     * @param scaleConstraint Scale constraint. May be {@code null}.
     */
    public void setScaleConstraint(ScaleConstraint scaleConstraint) {
        checkFrozen();
        this.scaleConstraint = scaleConstraint;
    }

    private void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    @Override
    public String toString() {
        return new StandardMetaIdentifierTransformer().serialize(this);
    }

}
