package edu.illinois.library.cantaloupe.resource;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

public class MockHttpServletResponse implements HttpServletResponse {

    private String description;
    private int status;

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public void addDateHeader(String s, long l) {
    }

    @Override
    public void addHeader(String s, String s1) {
    }

    @Override
    public void addIntHeader(String s, int i) {
    }

    @Override
    public boolean containsHeader(String s) {
        return false;
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
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String s) {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return null;
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
    }

    @Override
    public void setIntHeader(String s, int i) {
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
