package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RetrievalStrategyTest extends BaseTest {

    @Test
    void testFromWithAbortStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY;
        config.setProperty(key, RetrievalStrategy.ABORT.getConfigValue());

        assertEquals(RetrievalStrategy.ABORT, RetrievalStrategy.from(key));
    }

    @Test
    void testFromWithCacheStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY;
        config.setProperty(key, RetrievalStrategy.CACHE.getConfigValue());

        assertEquals(RetrievalStrategy.CACHE, RetrievalStrategy.from(key));
    }

    @Test
    void testFromWithDownloadStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY;
        config.setProperty(key, RetrievalStrategy.DOWNLOAD.getConfigValue());

        assertEquals(RetrievalStrategy.DOWNLOAD, RetrievalStrategy.from(key));
    }

    @Test
    void testFromWithStreamStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY;
        config.setProperty(key, RetrievalStrategy.STREAM.getConfigValue());

        assertEquals(RetrievalStrategy.STREAM, RetrievalStrategy.from(key));
    }

    @Test
    void testGetConfigValue() {
        assertEquals("AbortStrategy", RetrievalStrategy.ABORT.getConfigValue());
        assertEquals("CacheStrategy", RetrievalStrategy.CACHE.getConfigValue());
        assertEquals("DownloadStrategy", RetrievalStrategy.DOWNLOAD.getConfigValue());
        assertEquals("StreamStrategy", RetrievalStrategy.STREAM.getConfigValue());
    }

    @Test
    void testToString() {
        assertEquals("AbortStrategy", RetrievalStrategy.ABORT.toString());
        assertEquals("CacheStrategy", RetrievalStrategy.CACHE.toString());
        assertEquals("DownloadStrategy", RetrievalStrategy.DOWNLOAD.toString());
        assertEquals("StreamStrategy", RetrievalStrategy.STREAM.toString());
    }

}