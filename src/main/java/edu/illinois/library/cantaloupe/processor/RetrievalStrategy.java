package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

/**
 * Strategy used by a {@link Processor} to acquire content for reading.
 */
enum RetrievalStrategy {

    /**
     * The retrieval is aborted.
     */
    ABORT("AbortStrategy"),

    /**
     * Content is streamed into a reader from its source. When multiple
     * threads need to read the same image simultaneously, all will stream it
     * from the source simultaneously.
     */
    STREAM("StreamStrategy"),

    /**
     * Content is downloaded into a temporary location before being read. Each
     * thread is responsible for its own download and downloads are not shared
     * across threads. Downloaded content is deleted immediately after being
     * read.
     */
    DOWNLOAD("DownloadStrategy"),

    /**
     * Source content is downloaded into a {@link SourceCache} before being
     * read. When multiple threads need to read the same image and it has not
     * yet been cached, they will wait (on a monitor) for the image to
     * download.
     */
    CACHE("CacheStrategy");

    private final String configValue;

    static RetrievalStrategy from(Key key) {
        final Configuration config = Configuration.getInstance();
        final String configValue = config.getString(key, "");

        for (RetrievalStrategy s : values()) {
            if (s.getConfigValue().equals(configValue)) {
                return s;
            }
        }
        return null;
    }

    RetrievalStrategy(String configValue) {
        this.configValue = configValue;
    }

    /**
     * @return Representative string value of the strategy in the
     *         application configuration.
     */
    public String getConfigValue() {
        return configValue;
    }

    @Override
    public String toString() {
        return getConfigValue();
    }

}
