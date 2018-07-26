package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Status;

public class ResourceException extends Exception {

    private Status status;

    public ResourceException(Status status) {
        super(status.getDescription());
        this.status = status;
    }

    public ResourceException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

}
