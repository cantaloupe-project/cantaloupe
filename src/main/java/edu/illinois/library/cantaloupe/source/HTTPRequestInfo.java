package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

class HTTPRequestInfo {

    private final Headers headers = new Headers();
    private String uri, username, secret;

    HTTPRequestInfo(String uri) {
        this.uri = uri;
    }

    HTTPRequestInfo(String uri, String username, String secret) {
        this(uri);
        this.username = username;
        this.secret = secret;
    }

    HTTPRequestInfo(String uri,
                    String username,
                    String secret,
                    Map<String,?> headers) {
        this(uri, username, secret);
        if (headers != null) {
            headers.forEach((key, value) ->
                    this.headers.add(key, value.toString()));
        }
    }

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

    @Override
    public String toString() {
        return getURI() + "";
    }

}
