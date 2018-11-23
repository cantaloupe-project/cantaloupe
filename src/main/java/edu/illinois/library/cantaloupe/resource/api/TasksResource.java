package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Resource to enable an RPC-style asynchronous API for performing potentially
 * long-running tasks.
 */
public class TasksResource extends AbstractAPIResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TasksResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.OPTIONS, Method.POST };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    /**
     * Accepts a JSON object in the request entity with, at a minimum, a
     * {@literal verb} key with a value of one of the {@link
     * com.fasterxml.jackson.annotation.JsonSubTypes.Type} annotations on
     * {@link APITask}.
     */
    @Override
    public void doPOST() throws Exception {
        // N.B.: ObjectMapper will deserialize into the correct subclass.
        ObjectReader reader = new ObjectMapper().readerFor(Command.class);

        try {
            Command command = reader.readValue(getRequest().getInputStream());
            Callable<?> callable = (Callable<?>) command;
            APITask<?> task = new APITask<>(callable);

            // The task may take a while to complete, so we accept it for
            // processing and immediately return a response, which we submit
            // to a queue rather than a thread pool to avoid having multiple
            // expensive tasks running in parallel, and also to prevent them
            // from interfering with each other.
            TaskQueue.getInstance().submit(task);

            // TaskQueue will discard it when it's complete, so we also submit
            // it to TaskMnnitor which will hold onto it for status reporting.
            TaskMonitor.getInstance().add(task);

            // Return 202 Accepted and a Location header pointing to the task
            // URI.
            getResponse().setStatus(Status.ACCEPTED.getCode());

            final String taskURI = getPublicRootReference() +
                    Route.TASKS_PATH + "/" + task.getUUID().toString();
            getResponse().setHeader("Location", taskURI);
        } catch (NullPointerException | JsonProcessingException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
    }

}
