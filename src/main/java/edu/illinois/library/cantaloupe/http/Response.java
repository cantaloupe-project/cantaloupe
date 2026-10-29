package edu.illinois.library.cantaloupe.http;

import java.nio.charset.StandardCharsets;

public final class Response {

    private byte[] body;
    private final Headers headers = new Headers();
    private int status;
    private Transport transport;

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        return new String(getBody(), StandardCharsets.UTF_8);
    }

    public Headers getHeaders() {
        return headers;
    }

    public int getStatus() {
        return status;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

}
