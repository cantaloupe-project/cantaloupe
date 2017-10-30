package edu.illinois.library.cantaloupe.operation.overlay;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Thread-safe, in-memory image overlay cache.
 */
final class ImageOverlayCache {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ImageOverlayCache.class);

    private final Set<URI> downloadingOverlays =
            new ConcurrentSkipListSet<>();
    private final Map<URI, byte[]> overlays = new ConcurrentHashMap<>();

    /**
     * @param uri Overlay image URI.
     * @return Overlay image.
     * @throws IOException If the image cannot be accessed.
     */
    byte[] putAndGet(URI uri) throws IOException {
        // If the overlay is currently being downloaded in another thread,
        // wait for it to download.
        synchronized (this) {
            while (downloadingOverlays.contains(uri)) {
                try {
                    LOGGER.debug("putAndGet(): waiting on {}", uri);
                    wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        // Try to pluck it out of the cache.
        byte[] cachedValue = overlays.get(uri);
        if (cachedValue != null) {
            LOGGER.debug("putAndGet(): hit for {}", uri);
            return cachedValue;
        }

        LOGGER.debug("putAndGet(): miss for {}", uri);

        // It's not being downloaded and isn't cached, so download and cache it.
        downloadingOverlays.add(uri);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (InputStream is = uri.toURL().openStream()) {
            IOUtils.copy(is, os);
            overlays.put(uri, os.toByteArray());
        } finally {
            IOUtils.closeQuietly(os);
            downloadingOverlays.remove(uri);
            synchronized (this) {
                notifyAll();
            }
        }
        return overlays.get(uri);
    }

}
