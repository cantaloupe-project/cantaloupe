package edu.illinois.library.cantaloupe.http;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class Response {

    private byte[] body;
    private final Headers headers = new Headers();
    private int status;
    private Transport transport;

    static Response fromHttpClientResponse(HttpResponse<byte[]> jresponse) {
        Response response = new Response();
        response.setBody(jresponse.body());
        response.setStatus(jresponse.statusCode());

        switch (jresponse.version()) {
            case HTTP_2:
                response.setTransport(Transport.HTTP2_0);
                break;
            default:
                response.setTransport(Transport.HTTP1_1);
                break;
        }

        jresponse.headers().map().forEach((name, list) ->
                list.forEach(h ->
                        response.getHeaders().add(name, h)));

        return response;
    }

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
