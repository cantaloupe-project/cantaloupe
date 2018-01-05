package edu.illinois.library.cantaloupe.http;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpField;

import java.io.UnsupportedEncodingException;

public final class Response {

    private byte[] body;
    private final Headers headers = new Headers();
    private int status;
    private Transport transport;

    static Response fromJettyResponse(ContentResponse jresponse) {
        Response response = new Response();
        response.setBody(jresponse.getContent());
        response.setStatus(jresponse.getStatus());

        switch (jresponse.getVersion()) {
            case HTTP_2:
                response.setTransport(Transport.HTTP2_0);
                break;
            default:
                response.setTransport(Transport.HTTP1_1);
                break;
        }

        for (HttpField field : jresponse.getHeaders()) {
            response.getHeaders().add(field.getName(), field.getValue());
        }

        return response;
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        try {
            return new String(getBody(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
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
