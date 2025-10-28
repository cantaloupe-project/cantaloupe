package edu.illinois.library.cantaloupe.resource.api;

import java.util.concurrent.Callable;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.illinois.library.cantaloupe.async.AuditableFutureTask;

public class APITask<T> extends AuditableFutureTask<T> {

    private String verb;

    public APITask(Callable<T> callable) {
        super(callable);
        setVerb(((Command) callable).getVerb());
    }

    @JsonGetter
    public String getVerb() {
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
