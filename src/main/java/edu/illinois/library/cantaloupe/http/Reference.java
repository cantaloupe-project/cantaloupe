package edu.illinois.library.cantaloupe.http;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Mutable URI class modeled after the one in Restlet.</p>
 *
 * <p>Unlike {@link java.net.URL} and {@link java.net.URI}, this class does not
 * throw {@link java.net.MalformedURLException}s or {@link URISyntaxException}s,
 * which are unfortunately checked, making these classes a pain to use.</p>
 */
public final class Reference {

    private String scheme, user, secret, host, path, fragment;
    private int port = -1;
    private Query query = new Query();

    public static String decode(String encoded) {
        try {
            return java.net.URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
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
        setQuery(reference.getQuery());
        setFragment(reference.getFragment());
    }

    /**
     * @throws IllegalArgumentException
     */
    public Reference(String reference) {
        try {
            URI uri = new URI(reference);
            String userInfo = uri.getRawUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":");
                setUser(parts[0]);
                setSecret(parts[1]);
            }
            setScheme(uri.getScheme());
            setHost(uri.getHost());
            setPort(uri.getPort());
            setPath(uri.getRawPath());
            if (uri.getRawQuery() != null) {
                setQuery(new Query(uri.getRawQuery()));
            }
            setFragment(uri.getRawFragment());
        } catch (URISyntaxException e) {
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Reference) {
            Reference other = (Reference) obj;
            return other.toString().equals(toString());
        }
        return super.equals(obj);
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
     *         the period is at index 0 in the component.
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
        setPath("/" + components.stream().collect(Collectors.joining("/")));
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setQuery(Query query) {
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

    public URI toURI() {
        try {
            return new URI(toString());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e); // should never happen
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getScheme());
        builder.append("://");
        if (getUser() != null && !getUser().isEmpty() &&
                getSecret() != null && !getSecret().isEmpty()) {
            builder.append(getUser());
            builder.append(":");
            builder.append(getSecret());
            builder.append("@");
        }
        builder.append(getHost());
        if (("http".equals(getScheme()) && getPort() > 0 && getPort() != 80) ||
                ("https".equals(getScheme()) && getPort() > 0 && getPort() != 443)) {
            builder.append(":");
            builder.append(getPort());
        }
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
