package edu.illinois.library.cantaloupe.resource.api;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Retains tasks submitted via {@link
 * edu.illinois.library.cantaloupe.resource.api.TasksResource} for observation
 * by {@link edu.illinois.library.cantaloupe.resource.api.TaskResource}.
 */
final class TaskMonitor {

    private final List<APITask<?>> tasks = new CopyOnWriteArrayList<>();

    void add(APITask<?> task) {
        tasks.add(task);
    }

    APITask<?> get(final UUID uuid) {
        return tasks.stream()
                .filter(t -> t.getUUID().equals(uuid))
                .findFirst()
                .orElse(null);
    }



}
