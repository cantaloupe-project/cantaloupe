package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.async.Task;
import edu.illinois.library.cantaloupe.async.TaskQueue;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

/**
 * Resource to enable an RPC-style asynchronous API for performing potentially
 * long-running tasks.
 */
public class TasksResource extends AbstractAPIResource {

    private static final TaskMonitor taskMonitor = new TaskMonitor();

    static TaskMonitor getTaskMonitor() {
        return taskMonitor;
    }

    /**
     * @param rep JSON object with, at a minimum, a <code>verb</code> key with
     *            a value of one of the
     *            {@link com.fasterxml.jackson.annotation.JsonSubTypes.Type}
     *            annotations on {@link APITask}.
     * @return    Empty representation.
     */
    @Post("json")
    public Representation doPost(Representation rep) throws Exception {
        // N.B.: ObjectMapper will deserialize into the correct subclass.
        ObjectReader reader = new ObjectMapper().readerFor(APITask.class);

        try {
            // N.B.: Restlet will close this InputStream.
            APITask task = reader.readValue(rep.getStream());

            // The task may take a long time to complete, so we accept it for
            // processing and immediately return a response. We submit it to a
            // task queue rather than a thread pool to avoid having multiple
            // expensive tasks running in parallel, and also to prevent them
            // from conflicting with each other.
            TaskQueue.getInstance().submit((Task) task);

            // TaskQueue will discard it when it's complete, so we also submit
            // it to TaskMnnitor which will hold onto it for status reporting.
            getTaskMonitor().add(task);

            // Return HTTP 202 Accepted with a Location header pointing to the
            // task's URI.
            final Request request = getRequest();
            final String taskURI =
                    getPublicRootRef(request.getRootRef(), request.getHeaders()) +
                            RestletApplication.TASKS_PATH + "/" +
                            task.getUUID().toString();

            getBufferedResponseHeaders().add("Location", taskURI);
            setStatus(Status.SUCCESS_ACCEPTED);
            commitCustomResponseHeaders();
            return new EmptyRepresentation();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
