package edu.illinois.library.cantaloupe.resource.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Retains tasks submitted via
 * {@link edu.illinois.library.cantaloupe.resource.api.TasksResource} in a list
 * for observation by
 * {@link edu.illinois.library.cantaloupe.resource.api.TaskResource}.
 */
final class TaskMonitor {

    private final List<APITask<?>> tasks = new ArrayList<>();

    synchronized void add(APITask<?> task) {
        tasks.add(task);
    }

    synchronized APITask<?> get(final UUID uuid) {
        Optional<APITask<?>> task = tasks.stream().
                filter(t -> t.getUUID().equals(uuid)).
                findFirst();
        return task.orElse(null);
    }

}
