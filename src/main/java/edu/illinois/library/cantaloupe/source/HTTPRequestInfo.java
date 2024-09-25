package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

class HTTPRequestInfo {

    private final Headers headers = new Headers();
    private String uri, username, secret;
    private boolean isSendingHeadRequest = true;

    String getBasicAuthToken() {
        if (getUsername() != null && getSecret() != null) {
            byte[] bytes = (getUsername() + ":" + getSecret())
                    .getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(bytes);
        }
        return null;
    }

    Headers getHeaders() {
        return headers;
    }

    String getSecret() {
        return secret;
    }

    String getURI() {
        return uri;
    }

    String getUsername() {
        return username;
    }

    boolean isSendingHeadRequest() {
        return isSendingHeadRequest;
    }

    void setHeaders(Map<String,Object> headers) {
        if (headers != null) {
            headers.forEach((key, value) ->
                    this.headers.add(key, value.toString()));
        } else {
            this.headers.clear();
        }
    }

    void setSecret(String secret) {
        this.secret = secret;
    }

    void setSendingHeadRequest(boolean isUsingHeadRequest) {
        this.isSendingHeadRequest = isUsingHeadRequest;
    }

    void setURI(String uri) {
        this.uri = uri;
    }

    void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return getURI() + "";
    }

}
