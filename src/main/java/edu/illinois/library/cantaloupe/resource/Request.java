package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Query;
import edu.illinois.library.cantaloupe.http.Reference;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps an {@link HttpServletRequest}, adding some convenience methods.
 */
public final class Request {

    private HttpServletRequest wrappedRequest;

    private Map<String,String> cookies;
    private Headers headers;
    private Reference reference;

    /**
     * @param request Request that the new instance will wrap.
     */
    Request(HttpServletRequest request) {
        this.wrappedRequest = request;
    }

    public String getContextPath() {
        return wrappedRequest.getContextPath();
    }

    public Map<String,String> getCookies() {
        if (cookies == null) {
            cookies = new HashMap<>();
            final Enumeration<String> names = wrappedRequest.getHeaderNames();
            while (names.hasMoreElements()) {
                final String name = names.nextElement();
                if ("Cookie".equalsIgnoreCase(name)) {
                    final Enumeration<String> values = wrappedRequest.getHeaders(name);
                    while (values.hasMoreElements()) {
                        cookies.put(name, values.nextElement());
                    }
                }
            }
        }
        return cookies;
    }

    public Headers getHeaders() {
        if (headers == null) {
            headers = new Headers();
            final Enumeration<String> names = wrappedRequest.getHeaderNames();
            if (names != null) {
                while (names.hasMoreElements()) {
                    final String name = names.nextElement();
                    final Enumeration<String> values = wrappedRequest.getHeaders(name);
                    while (values.hasMoreElements()) {
                        headers.add(name, values.nextElement());
                    }
                }
            }
        }
        return headers;
    }

    /**
     * @return Stream for reading the request entity.
     */
    public InputStream getInputStream() throws IOException {
        return wrappedRequest.getInputStream();
    }

    public Method getMethod() {
        return Method.valueOf(wrappedRequest.getMethod());
    }

    /**
     * @return Full request URI including query. Note that this may not be the
     *         URI that the user agent supplies or sees.
     * @see AbstractResource#getPublicReference()
     */
    public Reference getReference() {
        if (reference == null) {
            reference = new Reference(wrappedRequest.getRequestURL().toString());

            String q = wrappedRequest.getQueryString();
            if (q != null && !q.isEmpty()) {
                reference.setQuery(new Query(q));
            }
        }
        return reference;
    }

    /**
     * @return Client IP address. Note that this may not be the user agent IP
     *         address, as in the case of e.g. running behind a reverse proxy
     *         server.
     * @see AbstractResource#getCanonicalClientIPAddress()
     */
    public String getRemoteAddr() {
        return wrappedRequest.getRemoteAddr();
    }

    /**
     * @return Wrapped request.
     */
    public HttpServletRequest getServletRequest() {
        return wrappedRequest;
    }

}
