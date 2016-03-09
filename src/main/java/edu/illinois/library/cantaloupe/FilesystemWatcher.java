package edu.illinois.library.cantaloupe;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Watches a directory for changes.
 *
 * Based on <a href="http://docs.oracle.com/javase/tutorial/essential/io/notification.html">
 *     Watching a Directory for Changes</a>.
 */
class FilesystemWatcher {

    public interface Callback {
        void created(Path path);
        void deleted(Path path);
        void modified(Path path);
    }

    private final Callback callback;
    private final Map<WatchKey,Path> keys;
    private volatile boolean shouldStop = false;
    private final WatchService watcher;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /**
     * Creates a WatchService and registers the given directory.
     */
    FilesystemWatcher(Path dir, Callback callback) throws IOException {
        this.callback = callback;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();

        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE,
                ENTRY_MODIFY);
        keys.put(key, dir);
    }

    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents() {
        while (true) {
            if (shouldStop) {
                return;
            }
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                switch (kind.name()) {
                    case "ENTRY_CREATE":
                        callback.created(child);
                        break;
                    case "ENTRY_DELETE":
                        callback.deleted(child);
                        break;
                    case "ENTRY_MODIFY":
                        callback.modified(child);
                        break;
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    public void stop() {
        shouldStop = true;
    }

}