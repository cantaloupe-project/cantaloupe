package edu.illinois.library.cantaloupe.http;

import org.eclipse.jetty.http.HttpMethod;

public enum Method {

    DELETE(HttpMethod.DELETE),
    GET(HttpMethod.GET),
    HEAD(HttpMethod.HEAD),
    OPTIONS(HttpMethod.OPTIONS),
    POST(HttpMethod.POST),
    PUT(HttpMethod.PUT);

    private HttpMethod jettyMethod;

    Method(HttpMethod jettyMethod) {
        this.jettyMethod = jettyMethod;
    }

    public HttpMethod toJettyMethod() {
        return jettyMethod;
    }

}
