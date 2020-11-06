package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Mutable URI class.</p>
 *
 * <p>Unlike {@link java.net.URL} and {@link java.net.URI}, this class does not
 * throw {@link java.net.MalformedURLException}s or {@link URISyntaxException}s,
 * which are unfortunately checked, making these classes a pain to use.</p>
 *
 * <p>Components are stored unencoded.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc3986">RFC 3986: Uniform
 * Resource Identifier</a>
 */
public final class Reference {

    private String scheme, user, secret, host, path, fragment;
    private int port = -1;
    private Query query = new Query();

    public static String decode(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    public static String encode(String decoded) {
        return URLEncoder.encode(decoded, StandardCharsets.UTF_8);
    }

    public Reference() {}

    /**
     * Copy constructor.
     */
    public Reference(Reference reference) {
        setUser(reference.getUser());
        setSecret(reference.getSecret());
        setScheme(reference.getScheme());
        setHost(reference.getHost());
        setPort(reference.getPort());
        setPath(reference.getPath());
        setQuery(new Query(reference.getQuery()));
        setFragment(reference.getFragment());
    }

    /**
     * Initializes an instance from a string. Illegal unencoded characters are
     * allowed as long as they don't break the URI structure.
     *
     * @throws IllegalArgumentException if the argument is not a valid URI.
     */
    public Reference(String reference) {
        try {
            // N.B.: java.net.URL is less fussy than java.net.URI about
            // URL-safe characters.
            URL url = new URL(reference);
            String userInfo = url.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":");
                setUser(parts[0]);
                setSecret(parts[1]);
            }
            setScheme(url.getProtocol());
            setHost(url.getHost());
            setPort(url.getPort());
            setPath(decode(url.getPath()));
            if (url.getQuery() != null) {
                setQuery(new Query(url.getQuery()));
            }
            setFragment(url.getRef());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Reference(URI uri) {
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":");
            setUser(parts[0]);
            setSecret(parts[1]);
        }
        setScheme(uri.getScheme());
        setHost(uri.getHost());
        setPort(uri.getPort());
        setPath(uri.getPath());
        if (uri.getQuery() != null) {
            setQuery(new Query(uri.getQuery()));
        }
        setFragment(uri.getFragment());
    }

    /**
     * <p>Mutates the instance according to any reverse proxying-related
     * request headers present in the argument. Currently supported
     * include:</p>
     *
     * <ul>
     *     <li>{@literal X-Forwarded-Proto}</li>
     *     <li>{@literal X-Forwarded-Host}</li>
     *     <li>{@literal X-Forwarded-Port}</li>
     *     <li>{@literal X-Forwarded-Path}</li>
     * </ul>
     *
     * @param headers Request headers.
     */
    public void applyProxyHeaders(Headers headers) {
        // N.B.: Header values may be comma-separated lists indicating a chain
        // of reverse proxies in order from closest-to-the-client to
        // closest-to-this-application.

        // Apply the protocol.
        final String protoHeader = headers.getFirstValue("X-Forwarded-Proto", "");
        if (!protoHeader.isEmpty()) {
            String proto = protoHeader.split(",")[0].trim();
            setScheme(proto);
        }

        // Apply the host.
        boolean hostContainsPort = false;
        final String hostHeader = headers.getFirstValue("X-Forwarded-Host", "");
        if (!hostHeader.isEmpty()) {
            String host = hostHeader.split(",")[0];
            host = host.substring(host.indexOf("://") + 1).trim();

            // The host may include a colon-separated port number.
            String[] parts = host.split(":");
            setHost(parts[0]);
            if (parts.length > 1) {
                hostContainsPort = true;
                setPort(Integer.parseInt(parts[1]));
            }
        }

        // Apply the port.
        // The port is obtained from the following in order of preference:
        // 1. The X-Forwarded-Port header
        // 2. The port in the X-Forwarded-Host header
        // 3. The default port of the protocol in the X-Forwarded-Proto header
        final String portHeader = headers.getFirstValue("X-Forwarded-Port", "");
        if (!portHeader.isEmpty()) {
            String portStr = portHeader.split(",")[0].trim();
            setPort(Integer.parseInt(portStr));
        } else if (!hostContainsPort && !protoHeader.isEmpty()) {
            setPort("https".equalsIgnoreCase(protoHeader) ? 443 : 80);
        }

        // Apply the path.
        final String pathHeader = headers.getFirstValue("X-Forwarded-Path", "");
        if (!pathHeader.isEmpty()) {
            String path = pathHeader.split(",")[0].trim();
            setPath(StringUtils.stripEnd(path, "/"));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Reference) {
            return obj.toString().equals(toString());
        }
        return super.equals(obj);
    }

    /**
     * @return Authority (userinfo + host + port). If the port is the
     *         standard port for the scheme, it is omitted.
     */
    public String getAuthority() {
        final StringBuilder builder = new StringBuilder();
        if (getUser() != null && !getUser().isEmpty() &&
                getSecret() != null && !getSecret().isEmpty()) {
            builder.append(getUser());
            builder.append(":");
            builder.append(getSecret());
            builder.append("@");
        }
        builder.append(getHost());
        if (("http".equalsIgnoreCase(getScheme()) && getPort() > 0 && getPort() != 80) ||
                ("https".equalsIgnoreCase(getScheme()) && getPort() > 0 && getPort() != 443)) {
            builder.append(":");
            builder.append(getPort());
        }
        return builder.toString();
    }

    public String getFragment() {
        return fragment;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public List<String> getPathComponents() {
        String path = getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return Arrays.asList(path.split("/"));
    }

    /**
     * @return String after the period in the last path component, or {@literal
     *         null} if the last path component does not contain a period or
     *         the period is at the first index in the component.
     */
    public String getPathExtension() {
        List<String> components = getPathComponents();
        if (components.size() > 0) {
            String component = components.get(components.size() - 1);
            int index = component.indexOf(".");
            if (index > 0) {
                return component.substring(index + 1);
            }
        }
        return null;
    }

    public int getPort() {
        return port;
    }

    public Query getQuery() {
        return query;
    }

    public String getRelativePath(String basePath) {
        int index = getPath().indexOf(basePath);
        if (index == 0) {
            return getPath().substring(basePath.length());
        }
        return getPath();
    }

    /**
     * @return Lowercase scheme.
     */
    public String getScheme() {
        return scheme;
    }

    public String getSecret() {
        return secret;
    }

    public String getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Updates a component (token between slashes) of the path.
     *
     * @param componentIndex Zero-based path component index.
     * @param pathComponent  Path component.
     * @throws IndexOutOfBoundsException if the given index is greater than the
     *                                   number of path components minus one.
     */
    public void setPathComponent(int componentIndex, String pathComponent) {
        List<String> components = getPathComponents();
        components.set(componentIndex, pathComponent);
        setPath("/" + String.join("/", components));
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @param query Cannot be {@code null}.
     * @throws IllegalArgumentException if the argument is {@code null}.
     */
    public void setQuery(Query query) {
        if (query == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
        this.query = query;
    }

    public void setScheme(String scheme) {
        if (scheme != null) {
            this.scheme = scheme.toLowerCase();
        } else {
            this.scheme = null;
        }
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return URI string with all components encoded.
     */
    @Override
    public String toString() {
        try {
            String query = getQuery().toString();
            URI uri = new URI(getScheme(),
                    getAuthority(),
                    getPath(),
                    query.isBlank() ? null : query,
                    getFragment());
            return uri.toString();
        } catch (URISyntaxException ignore) {
            StringBuilder builder = new StringBuilder();
            builder.append(getScheme());
            builder.append("://");
            builder.append(getAuthority());
            builder.append(getPath());
            if (!getQuery().isEmpty()) {
                builder.append("?");
                builder.append(getQuery().toString());
            }
            if (getFragment() != null) {
                builder.append("#");
                builder.append(getFragment());
            }
            return builder.toString();
        }
    }

}
