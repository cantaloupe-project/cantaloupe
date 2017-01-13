package edu.illinois.library.cantaloupe.operation.overlay;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Thread-safe, in-memory image overlay cache.
 */
abstract class ImageOverlayCache {

    private static final Logger logger = LoggerFactory.
            getLogger(ImageOverlayCache.class);

    private static final Set<String> downloadingOverlays =
            new ConcurrentSkipListSet<>();
    private static final Map<String,byte[]> overlays =
            new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    /**
     * @param file Overlay image file.
     * @return Overlay image.
     * @throws IOException If the image cannot be accessed.
     */
    static byte[] putAndGet(File file) throws IOException {
        return putAndGet(file.getAbsolutePath());
    }

    /**
     * @param url Overlay image URL.
     * @return Overlay image.
     * @throws IOException If the image cannot be accessed.
     */
    static byte[] putAndGet(URL url) throws IOException {
        return putAndGet(url.toString());
    }

    /**
     * @param pathnameOrURL Pathname or URL of the overlay image.
     * @return Overlay image.
     * @throws IOException If the image cannot be accessed.
     */
    static byte[] putAndGet(String pathnameOrURL) throws IOException {
        // If the overlay is currently being downloaded in another thread,
        // wait for it to download.
        synchronized (lock) {
            while (downloadingOverlays.contains(pathnameOrURL)) {
                try {
                    logger.debug("putAndGet(): waiting on {}", pathnameOrURL);
                    lock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        // Try to pluck it out of the cache.
        byte[] cachedValue = overlays.get(pathnameOrURL);
        if (cachedValue != null) {
            logger.debug("putAndGet(): hit for {}", pathnameOrURL);
            return cachedValue;
        }

        logger.debug("putAndGet(): miss for {}", pathnameOrURL);

        // It's not being downloaded and isn't cached, so download and cache it.
        InputStream is = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            downloadingOverlays.add(pathnameOrURL);
            if (pathnameOrURL.startsWith("http://") ||
                    pathnameOrURL.startsWith("https://")) {
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
