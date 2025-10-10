package edu.illinois.library.cantaloupe.resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;

public class IIIFRequest extends Request {

    /**
     * Cached by {@link #getIdentifier()}.
     */
    private Identifier identifier;
   
    public IIIFRequest(HttpServletRequest request, List<String> pathArguments) {
        super(request, pathArguments);
    }

        /**
     * <p>Returns the decoded identifier path component of the URI. (This may
     * not be the identifier that the client supplies or sees; for that, use
     * {@link #getPublicIdentifier()}.)</p>
     *
     * <p>N.B.: Depending on the image request endpoint API, The return value
     * may include "meta-information" that is not part of the identifier but is
     * encoded along with it. In that case, it is not safe to consume via this
     * method, and {@link #getMetaIdentifier()} should be used instead.</p>
     *
     * @return Identifier, or {@code null} if the URI does not have an
     *         identifier path component.
     * @see #getMetaIdentifier()
     * @see #getPublicIdentifier()
     */
    public Identifier getIdentifier() {
        if (identifier == null) {
            String pathComponent = getIdentifierPathComponent();
            if (pathComponent != null) {
                identifier = Identifier.fromURIPathComponent(pathComponent);
            }
        }
        return identifier;
    }

    public static final String PUBLIC_IDENTIFIER_HEADER = "X-Forwarded-ID";


    /**
     * <p>Returns the identifier that the client sees. This will be the value
     * of the {@link #PUBLIC_IDENTIFIER_HEADER} header, if available, or else
     * the {@code identifier} URI path component.</p>
     *
     * <p>The result is not decoded, as the encoding may be influenced by
     * {@link Key#SLASH_SUBSTITUTE}, for example.</p>
     *
     * @see #getIdentifier()
     */
    public String getPublicIdentifier() {
        return getHeaders().getFirstValue(
                PUBLIC_IDENTIFIER_HEADER,
                getIdentifierPathComponent());
    }


    /**
     * <p>Returns the first {@link #getPathArguments() path argument}. (Most
     * resources have an identifier as the first path argument, so this will
     * work for them, but if not, an override will be necessary.)</p>
     *
     * <p>The result is not decoded and may be a {@link MetaIdentifier
     * meta-identifier}. As such, it is not usable without additional
     * processing.</p>
     *
     * @return Identifier, or {@code null} if no path arguments are
     *         available.
     */
    public String getIdentifierPathComponent() {
        List<String> args = getPathArguments();
        return (!args.isEmpty()) ? args.get(0) : null;
    }

}
