package edu.illinois.library.cantaloupe.resource;

import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public abstract class CustomOutputRepresentation extends OutputRepresentation {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JSONRepresentation.class);

    protected Callable<?> onRelease;

    public CustomOutputRepresentation(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    public void release() {
        super.release();
        if (onRelease != null) {
            try {
                onRelease.call();
            } catch (Exception e) {
                LOGGER.error("release(): {}", e.getMessage());
            }
        }
    }

}
