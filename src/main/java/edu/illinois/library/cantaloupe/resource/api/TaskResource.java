package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.UUID;

/**
 * Resource for monitoring of tasks invoked by {@link TasksResource}.
 */
public class TaskResource extends AbstractAPIResource {

    /**
     * @return JSON task representation.
     */
    @Get
    public Representation doGet() throws Exception {
        final Map<String,Object> attrs = getRequest().getAttributes();
        final String uuidStr = (String) attrs.get("uuid");

        try {
            final UUID uuid = UUID.fromString(uuidStr);
            APITask task = TasksResource.getTaskMonitor().get(uuid);

            if (task != null) {
                return new JSONRepresentation(task);
            } else {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new FileNotFoundException("No such task");
        }
    }

}
