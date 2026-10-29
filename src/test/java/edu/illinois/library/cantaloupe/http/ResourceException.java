package edu.illinois.library.cantaloupe.http;

import java.io.IOException;

public class ResourceException extends IOException {

    private Response response;

    public ResourceException(Response response) {
        super("HTTP " + response.getStatus());
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public int getStatusCode() {
        return getResponse().getStatus();
    }

}
