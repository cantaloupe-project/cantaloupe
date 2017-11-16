package edu.illinois.library.cantaloupe.http;

import org.eclipse.jetty.client.api.ContentResponse;

import java.io.IOException;

public class ResourceException extends IOException {

    private ContentResponse response;

    public ResourceException(ContentResponse response) {
        super("HTTP " + response.getStatus() + ": " + response.getReason());
        this.response = response;
    }

    public ContentResponse getResponse() {
        return response;
    }

    public int getStatusCode() {
        return getResponse().getStatus();
    }

}
