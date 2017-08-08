package edu.illinois.library.cantaloupe.resource;

import java.util.HashMap;
import java.util.Map;

public class RequestContext {
    private String requestURI;
    private Map<String,String> requestHeaders;
    private String clientIP;
    private Map<String,String> cookies;

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public Map<String, Object> asMap() {
        Map map = new HashMap<String, Object>();
        map.put("URI", requestURI);
        map.put("headers", requestHeaders);
        map.put("clientIP", clientIP);
        map.put("cookies", requestURI);
        return map;
    }
}
