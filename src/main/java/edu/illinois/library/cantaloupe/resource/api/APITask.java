package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.illinois.library.cantaloupe.async.AuditableFutureTask;

import java.util.concurrent.Callable;

class APITask<T> extends AuditableFutureTask<T> {

    private String verb;

    APITask(Callable<T> callable) {
        super(callable);
        setVerb(((Command) callable).getVerb());
    }

    @JsonGetter
    String getVerb() {
        return verb;
    }

    private void setVerb(String verb) {
        this.verb = verb;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return getVerb() + " " + getUUID();
    }

}
