package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Header;
import edu.illinois.library.cantaloupe.http.Headers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

public class MockHttpServletResponse implements HttpServletResponse {

    private int status;
    private String description;
    private final Headers headers = new Headers();
    private final ByteArrayServletOutputStream outputStream =
            new ByteArrayServletOutputStream();

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public void addDateHeader(String s, long l) {
    }

    @Override
    public void addHeader(String s, String s1) {
        headers.add(s, s1);
    }

    @Override
    public void addIntHeader(String s, int i) {
        headers.add(s, Integer.toString(i));
    }

    @Override
    public boolean containsHeader(String s) {
        return headers.toMap().containsKey(s);
    }

    @Override
    public String encodeRedirectUrl(String s) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String s) {
        return null;
    }

    @Override
    public String encodeUrl(String s) {
        return null;
    }

    @Override
    public String encodeURL(String s) {
        return null;
    }

    @Override
    public void flushBuffer() {
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public String getHeader(String s) {
        return headers.getFirstValue(s);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.stream()
                .map(Header::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getHeaders(String s) {
        return headers.getAll(s).stream()
                .map(Header::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public ByteArrayServletOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public PrintWriter getWriter() {
        return null;
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetBuffer() {
    }

    @Override
    public void sendError(int i) {
    }

    @Override
    public void sendError(int i, String s) {
    }

    @Override
    public void sendRedirect(String s) {
    }

    @Override
    public void setBufferSize(int i) {
    }

    @Override
    public void setCharacterEncoding(String s) {
    }

    @Override
    public void setContentLength(int i) {
    }

    @Override
    public void setContentLengthLong(long l) {
    }

    @Override
    public void setContentType(String s) {
    }

    @Override
    public void setDateHeader(String s, long l) {
    }

    @Override
    public void setHeader(String s, String s1) {
        headers.set(s, s1);
    }

    @Override
    public void setIntHeader(String s, int i) {
        headers.set(s, Integer.toString(i));
    }

    @Override
    public void setLocale(Locale locale) {
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public void setStatus(int status, String description) {
        setStatus(status);
        this.description = description;
    }

}
