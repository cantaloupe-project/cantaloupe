package edu.illinois.library.cantaloupe.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.auth.BasicAuth;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.api.APITask;
import edu.illinois.library.cantaloupe.resource.api.Command;
import edu.illinois.library.cantaloupe.resource.api.TaskMonitor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for task management endpoints.
 * Replaces the previous api.TasksResource and api.TaskResource classes.
 *
 * Note: This is a placeholder implementation. The original TaskMonitor and APITask
 * classes are package-private and would need to be refactored for full functionality.
 */
@RestController
@RequestMapping("/tasks")
public class TasksController {
    private final Configuration configuration;
    static final String BASIC_REALM = Application.getName() + " API Realm";

    @Autowired
    public TasksController(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Submits a new task.
     * Accepts a JSON object in the request entity with, at a minimum, a
     * {@literal verb} key with a value of one of the {@link
     * com.fasterxml.jackson.annotation.JsonSubTypes.Type} annotations on
     * {@link APITask}.
     * @throws ResourceException
     * @throws IOException
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(HttpServletRequest request,
                                                         HttpServletResponse response)
            throws ResourceException, IOException {
        beforeAll(request, response);

        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");

                // N.B.: ObjectMapper will deserialize into the correct subclass.
        ObjectReader reader = new ObjectMapper().readerFor(Command.class);

        try {
            Command command = reader.readValue(request.getInputStream());
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

            // Return 202 Accepted and a Location header pointing to the task URI.
            final String taskURI = "/tasks/" + task.getUUID().toString();
            response.setHeader("Location", taskURI);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (NullPointerException | JsonProcessingException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Returns information about a specific task.
     * @throws ResourceException
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable("uuid") String uuidStr,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response)
            throws ResourceException {
        beforeAll(request, response);

        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        final UUID uuid = UUID.fromString(uuidStr);
        APITask<?> task = TaskMonitor.getInstance().get(uuid);

        if (task != null) {
            // Create a simple Map representation of the task for JSON serialization
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("uuid", task.getUUID());
            taskMap.put("verb", task.getVerb());
            taskMap.put("status", task.getStatus());
            taskMap.put("queued_at", task.getInstantQueued());
            taskMap.put("started_at", task.getInstantStarted());
            taskMap.put("stopped_at", task.getInstantStopped());
            if (task.getException() != null) {
                taskMap.put("exception", task.getException().getMessage());
            }
            return ResponseEntity.ok(taskMap);
        }
        // Placeholder implementation
        return ResponseEntity.notFound().build();
    }



    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options(HttpServletRequest request,
                                       HttpServletResponse response)
            throws ResourceException {
        beforeAll(request, response);

        return ResponseEntity.noContent()
                .header("Allow", "POST,OPTIONS")
                .build();
    }

    @RequestMapping(value = "/{uuid}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsForTask(@PathVariable String uuid,
                                               HttpServletRequest request,
                                               HttpServletResponse response)
            throws ResourceException {
        beforeAll(request, response);

        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }

    private void beforeAll(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        if (!configuration.getBoolean(Key.API_ENABLED, false)) {
            throw new EndpointDisabledException();
        }
        // Perform HTTP Basic Authentication
        BasicAuth.authenticateUsingBasic(BASIC_REALM, user -> {
            final String configUser = configuration.getString(Key.API_USERNAME, "");
            if (!configUser.isEmpty() && configUser.equals(user)) {
                return configuration.getString(Key.API_SECRET);
            }
            return null;
        }, request, response);
    }
}
