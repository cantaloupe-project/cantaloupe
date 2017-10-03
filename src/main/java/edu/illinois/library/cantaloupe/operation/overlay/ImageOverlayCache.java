package edu.illinois.library.cantaloupe.operation.overlay;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Thread-safe, in-memory image overlay cache.
 */
class ImageOverlayCache {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ImageOverlayCache.class);

    private final Set<String> downloadingOverlays =
            new ConcurrentSkipListSet<>();
    private final Map<String, byte[]> overlays = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    /**
     * @param file Overlay image file.
     * @return Overlay image.
     * @throws IOException If the image cannot be accessed.
     */
    byte[] putAndGet(File file) throws IOException {
        return putAndGet(file.getAbsolutePath());
    }

    /**
     * @param uri Overlay image URI.
     * @return Overlay image.
     * @throws IOException If the image cannot be accessed.
     */
    byte[] putAndGet(URI uri) throws IOException {
        return putAndGet(uri.toString());
    }

    /**
     * @param pathnameOrURL Pathname or URL of the overlay image.
     * @return Overlay image.
     * @throws IOException If the image cannot be accessed.
     */
    byte[] putAndGet(String pathnameOrURL) throws IOException {
        // If the overlay is currently being downloaded in another thread,
        // wait for it to download.
        synchronized (lock) {
            while (downloadingOverlays.contains(pathnameOrURL)) {
                try {
                    LOGGER.debug("putAndGet(): waiting on {}", pathnameOrURL);
                    lock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        // Try to pluck it out of the cache.
        byte[] cachedValue = overlays.get(pathnameOrURL);
        if (cachedValue != null) {
            LOGGER.debug("putAndGet(): hit for {}", pathnameOrURL);
            return cachedValue;
        }

        LOGGER.debug("putAndGet(): miss for {}", pathnameOrURL);

        // It's not being downloaded and isn't cached, so download and cache it.
        InputStream is = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            downloadingOverlays.add(pathnameOrURL);

            if (ImageOverlay.SUPPORTED_URI_SCHEMES.stream()
                    .filter(pathnameOrURL::startsWith).count() > 0) {
                final URL url = new URL(pathnameOrURL);
                is = url.openStream();
            } else {
                is = new FileInputStream(pathnameOrURL);
            }
            IOUtils.copy(is, os);
            overlays.put(pathnameOrURL, os.toByteArray());
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            downloadingOverlays.remove(pathnameOrURL);
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        return overlays.get(pathnameOrURL);
    }

}
