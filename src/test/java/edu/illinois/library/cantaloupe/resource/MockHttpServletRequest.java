package edu.illinois.library.cantaloupe.resource;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MockHttpServletRequest implements HttpServletRequest {

    private String contextPath;
    private String method = "GET";
    private String remoteAddr;
    private String requestURL = "http://example.org/path";
    private final Map<String, List<String>> headers = new HashMap<>();

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) {
        return false;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String s) {
        return 0;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    @Override
    public String getHeader(String s) {
        List<String> values = headers.get(s);
        if (values != null) {
            return values.get(0);
        }
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        final Iterator<String> iter = headers.keySet().iterator();
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return iter.hasNext();
            }
            @Override
            public String nextElement() {
                return iter.next();
            }
        };
    }

    /**
     * @return Mutable map of headers.
     */
    public Map<String,List<String>> getHeaders() {
        return this.headers;
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        List<String> values = headers.get(s);
        Iterator<String> iter;
        if (values != null) {
            iter = values.iterator();
        } else {
            iter = Collections.emptyIterator();
        }
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return iter.hasNext();
            }
            @Override
            public String nextElement() {
                return iter.next();
            }
        };
    }

    @Override
    public int getIntHeader(String s) {
        return 0;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public Locale getLocale() {
        return Locale.ENGLISH;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() {
        return null;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getParameter(String s) {
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String s) {
        return new String[0];
    }

    @Override
    public Part getPart(String s) {
        return null;
    }

    @Override
    public Collection<Part> getParts() {
        return null;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public BufferedReader getReader() {
        return null;
    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public String getRequestURI() {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(requestURL);
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return "example.org";
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getServletPath() {
        return "/";
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean b) {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public void login(String s, String s1) {
    }

    @Override
    public void logout() {
    }

    @Override
    public void removeAttribute(String s) {
    }

    @Override
    public void setAttribute(String s, Object o) {
    }

    @Override
    public void setCharacterEncoding(String s) {
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRemoteAddr(String addr) {
        this.remoteAddr = addr;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest,
                                   ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) {
        return null;
    }

}
