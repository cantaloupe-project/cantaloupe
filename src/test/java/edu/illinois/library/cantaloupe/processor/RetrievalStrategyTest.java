package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class RetrievalStrategyTest extends BaseTest {

    @Test
    public void testFromWithAbortStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY;
        config.setProperty(key, RetrievalStrategy.ABORT.getConfigValue());

        assertEquals(RetrievalStrategy.ABORT, RetrievalStrategy.from(key));
    }

    @Test
    public void testFromWithCacheStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY;
        config.setProperty(key, RetrievalStrategy.CACHE.getConfigValue());

        assertEquals(RetrievalStrategy.CACHE, RetrievalStrategy.from(key));
    }

    @Test
    public void testFromWithDownloadStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY;
        config.setProperty(key, RetrievalStrategy.DOWNLOAD.getConfigValue());

        assertEquals(RetrievalStrategy.DOWNLOAD, RetrievalStrategy.from(key));
    }

    @Test
    public void testFromWithStreamStrategy() {
        final Configuration config = Configuration.getInstance();
        final Key key = Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY;
        config.setProperty(key, RetrievalStrategy.STREAM.getConfigValue());

        assertEquals(RetrievalStrategy.STREAM, RetrievalStrategy.from(key));
    }

    @Test
    public void testGetConfigValue() {
        assertEquals("AbortStrategy", RetrievalStrategy.ABORT.getConfigValue());
        assertEquals("CacheStrategy", RetrievalStrategy.CACHE.getConfigValue());
        assertEquals("DownloadStrategy", RetrievalStrategy.DOWNLOAD.getConfigValue());
        assertEquals("StreamStrategy", RetrievalStrategy.STREAM.getConfigValue());
    }

    @Test
    public void testToString() {
        assertEquals("AbortStrategy", RetrievalStrategy.ABORT.toString());
        assertEquals("CacheStrategy", RetrievalStrategy.CACHE.toString());
        assertEquals("DownloadStrategy", RetrievalStrategy.DOWNLOAD.toString());
        assertEquals("StreamStrategy", RetrievalStrategy.STREAM.toString());
    }

}