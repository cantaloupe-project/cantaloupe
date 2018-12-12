package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.NoSuchFileException;
import java.util.UUID;

/**
 * Resource for monitoring of tasks invoked by {@link TasksResource}.
 */
public class TaskResource extends AbstractAPIResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TaskResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    /**
     * Writes a JSON task representation to the response output stream.
     */
    public void doGET() throws Exception {
        final String uuidStr = getPathArguments().get(0);
        try {
            final UUID uuid = UUID.fromString(uuidStr);
            APITask<?> task = TaskMonitor.getInstance().get(uuid);

            if (task != null) {
                getResponse().setHeader("Content-Type",
                        "application/json;charset=UTF-8");

                new JacksonRepresentation(task)
                        .write(getResponse().getOutputStream());
            } else {
                throw new NoSuchFileException("No such task");
            }
        } catch (IllegalArgumentException e) {
            throw new NoSuchFileException("No such task");
        }
    }

    @Override
    boolean requiresAuth() {
        return true;
    }

}
