package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheDisabledException;
import edu.illinois.library.cantaloupe.cache.MockBrokenSourceImageFileCache;
import edu.illinois.library.cantaloupe.cache.MockBrokenSourceInputStreamCache;
import edu.illinois.library.cantaloupe.cache.MockUnreliableSourceImageFileCache;
import edu.illinois.library.cantaloupe.cache.MockUnreliableSourceOutputStreamCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.source.MockStreamSource;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessorConnectorTest extends BaseTest {

    private static final Identifier IDENTIFIER = new Identifier("jpg");

    private ProcessorConnector instance;

    private static void recursiveDeleteOnExit(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) {
                file.toFile().deleteOnExit();
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs) {
                dir.toFile().deleteOnExit();
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new ProcessorConnector();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
        config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                TestUtil.getImage("jpg").getParent().toString() + "/");
        config.setProperty(Key.PROCESSOR_SELECTION_STRATEGY,
                ManualSelectionStrategy.CONFIGURATION_VALUE);
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
    }

    @Test
    void testGetFallbackRetrievalStrategy() {
        final Configuration config = Configuration.getInstance();
        // config set to stream
        config.setProperty(Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY,
                RetrievalStrategy.STREAM.getConfigValue());
        assertEquals(RetrievalStrategy.STREAM,
                ProcessorConnector.getFallbackRetrievalStrategy());

        // config set to download
        config.setProperty(Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY,
                RetrievalStrategy.DOWNLOAD.getConfigValue());
        assertEquals(RetrievalStrategy.DOWNLOAD,
                ProcessorConnector.getFallbackRetrievalStrategy());

        // config set to cache
        config.setProperty(Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY,
                RetrievalStrategy.CACHE.getConfigValue());
        assertEquals(RetrievalStrategy.CACHE,
                ProcessorConnector.getFallbackRetrievalStrategy());

        // config not set
        config.clearProperty(Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY);
        assertEquals(RetrievalStrategy.DOWNLOAD,
                ProcessorConnector.getFallbackRetrievalStrategy());
    }

    @Test
    void testGetStreamProcessorRetrievalStrategy() {
        final Configuration config = Configuration.getInstance();
        // config set to stream
        config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                RetrievalStrategy.STREAM.getConfigValue());
        assertEquals(RetrievalStrategy.STREAM,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());

        // config set to download
        config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                RetrievalStrategy.DOWNLOAD.getConfigValue());
        assertEquals(RetrievalStrategy.DOWNLOAD,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());

        // config set to cache
        config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                RetrievalStrategy.CACHE.getConfigValue());
        assertEquals(RetrievalStrategy.CACHE,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());

        // config not set
        config.clearProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY);
        assertEquals(RetrievalStrategy.STREAM,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());
    }

    @Test
    void testConnectWithFileSourceAndFileProcessor() throws Exception {
        final Source source = new SourceFactory().newSource(IDENTIFIER, null);
        final Processor processor = new ProcessorFactory().newProcessor(Format.JPG);

        assertNull(instance.connect(source, processor, IDENTIFIER, Format.JPG));

        assertEquals(source.getFile(),
                ((FileProcessor) processor).getSourceFile());
    }

    @Test
    void testConnectWithFileSourceAndStreamProcessor()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, MockStreamProcessor.class.getName());

        final Source source = new SourceFactory().newSource(IDENTIFIER, null);
        final Processor processor = new ProcessorFactory().newProcessor(Format.JPG);

        assertNull(instance.connect(source, processor, IDENTIFIER, Format.JPG));

        StreamFactory ss1 = source.newStreamFactory();
        StreamFactory ss2 = ((StreamProcessor) processor).getStreamFactory();

        assertEqualSources(ss1, ss2);
    }

    @Test
    void testConnectWithStreamSourceAndFileProcessorAndDownloadStrategy()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            final Identifier identifier = new Identifier("jp2");
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "HttpSource");
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.PROCESSOR_FALLBACK,
                    MockFileProcessor.class.getName());
            config.setProperty(Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.DOWNLOAD.getConfigValue());

            final Source source = new SourceFactory().newSource(
                    identifier, null);
            final Processor processor = new ProcessorFactory().newProcessor(Format.JP2);

            assertNotNull(instance.connect(source, processor, identifier, Format.JPG));
        } finally {
            server.stop();
        }
    }

    @Test
    void testConnectWithStreamSourceAndFileProcessorAndCacheStrategyAndSourceCacheAvailable()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        final Identifier identifier = new Identifier("jp2");

        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());
            config.setProperty(Key.SOURCE_STATIC, "HttpSource");
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.PROCESSOR_FALLBACK,
                    MockFileProcessor.class.getName());
            config.setProperty(Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());

            final Source source = new SourceFactory().newSource(identifier, null);
            final Processor processor = new ProcessorFactory().newProcessor(Format.JP2);

            assertNull(instance.connect(source, processor, identifier, Format.JPG));

            assertEquals(
                    CacheFactory.getSourceCache().get().getSourceImageFile(identifier).orElseThrow(),
                    ((FileProcessor) processor).getSourceFile());
        } finally {
            server.stop();
            recursiveDeleteOnExit(cacheFolder);
        }
    }

    @Test
    void testConnectWithStreamSourceAndFileProcessorAndCacheStrategyAndSourceCacheDisabled()
            throws Exception {
        final Identifier identifier = new Identifier("jp2");
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC, MockStreamSource.class.getName());
        config.setProperty(Key.PROCESSOR_FALLBACK,
                MockFileProcessor.class.getName());
        config.setProperty(Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY,
                RetrievalStrategy.CACHE.getConfigValue());

        final Source source = new SourceFactory().newSource(identifier, null);
        final Processor processor = new ProcessorFactory().newProcessor(Format.JP2);

        assertThrows(CacheDisabledException.class,
                () -> instance.connect(source, processor, identifier, Format.JPG));
    }

    @Test
    void testConnectWithStreamSourceAndFileProcessorAndAbortStrategy()
            throws Exception {
        final Identifier identifier = new Identifier("jp2");
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_STATIC, MockStreamSource.class.getName());
        config.setProperty(Key.PROCESSOR_FALLBACK,
                MockFileProcessor.class.getName());
        config.setProperty(Key.PROCESSOR_FALLBACK_RETRIEVAL_STRATEGY,
                RetrievalStrategy.ABORT.getConfigValue());

        final Source source = new SourceFactory().newSource(identifier, null);
        final Processor processor = new ProcessorFactory().newProcessor(Format.JP2);

        assertThrows(IncompatibleSourceException.class,
                () -> instance.connect(source, processor, identifier, Format.JPG));
    }

    @Test
    void testConnectWithStreamSourceAndFileProcessorAndStrategyNotSet()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();
            final Identifier identifier = new Identifier("jp2");
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "HttpSource");
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.PROCESSOR_FALLBACK,
                    MockFileProcessor.class.getName());

            final Source source = new SourceFactory().newSource(identifier, null);
            final Processor processor = new ProcessorFactory().newProcessor(Format.JP2);

            assertNotNull(instance.connect(source, processor, identifier, Format.JPG));
        } finally {
            server.stop();
        }
    }

    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithStreamStrategy()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "HttpSource");
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                    server.getHTTPURI() + "/");

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final StreamProcessor processor = new MockStreamProcessor();

            assertNull(instance.connect(source, processor, IDENTIFIER, Format.JPG));

            StreamFactory ss1 = source.newStreamFactory();
            StreamFactory ss2 = processor.getStreamFactory();

            assertEqualSources(ss1, ss2);
        } finally {
            server.stop();
        }
    }

    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithDownloadStrategy()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC,
                    MockStreamSource.class.getName());
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.DOWNLOAD.getConfigValue());
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());

            final Source source = new SourceFactory().
                    newSource(IDENTIFIER, null);
            final Processor processor = new MockStreamProcessor();

            Future<Path> tempFile = instance.connect(source, processor,
                    IDENTIFIER, Format.JPG);
            Files.delete(tempFile.get());
        } finally {
            server.stop();
        }
    }

    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithCacheStrategyAndSourceCacheAvailable()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC,
                    MockStreamSource.class.getName());
            config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final StreamProcessor processor = new MockStreamProcessor();

            assertNull(instance.connect(source, processor, IDENTIFIER, Format.JPG));

            assertEqualSources(
                    CacheFactory.getSourceCache().get().getSourceImageFile(IDENTIFIER).orElseThrow(),
                    processor.getStreamFactory());
        } finally {
            server.stop();
            recursiveDeleteOnExit(cacheFolder);
        }
    }

    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithCacheStrategyAndSourceCacheAvailableAndSourceSupportingDirectSeekingAndProcessorSupportingSeeking()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "HttpSource");
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final MockStreamProcessor processor = new MockStreamProcessor();
            processor.setSeeking(true);

            assertNull(instance.connect(source, processor, IDENTIFIER, Format.JPG));

            StreamFactory ss1 = source.newStreamFactory();
            StreamFactory ss2 = processor.getStreamFactory();

            assertEqualSources(ss1, ss2);
        } finally {
            server.stop();
            recursiveDeleteOnExit(cacheFolder);
        }
    }

    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithCacheStrategyAndSourceCacheAvailableAndSourceSupportingDirectSeekingAndProcessorNotSupportingSeeking()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "HttpSource");
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.HTTPSOURCE_CHUNKING_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final MockStreamProcessor processor = new MockStreamProcessor();
            processor.setSeeking(false);

            assertNull(instance.connect(source, processor, IDENTIFIER, Format.JPG));

            assertEqualSources(
                    CacheFactory.getSourceCache().get().getSourceImageFile(IDENTIFIER).orElseThrow(),
                    processor.getStreamFactory());
        } finally {
            server.stop();
            recursiveDeleteOnExit(cacheFolder);
        }
    }

    /**
     * Tests that {@link ProcessorConnector#connect(Source, Processor,
     * Identifier, Format)} passes through an {@link IOException} thrown by
     * {@link
     * edu.illinois.library.cantaloupe.cache.SourceCache#getSourceImageFile(Identifier)}
     * when using {@link RetrievalStrategy#CACHE}.
     */
    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithCacheStrategyAndSourceCacheGetSourceImageFileThrowingException()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "HttpSource");
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.SOURCE_CACHE,
                    MockBrokenSourceImageFileCache.class.getSimpleName());
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final StreamProcessor processor = new MockStreamProcessor();

            assertThrows(IOException.class,
                    () -> instance.connect(source, processor, IDENTIFIER, Format.JPG));
        } finally {
            server.stop();
        }
    }

    /**
     * Tests that {@link ProcessorConnector#connect(Source, Processor,
     * Identifier, Format)} passes through an {@link IOException} thrown by
     * {@link
     * edu.illinois.library.cantaloupe.cache.SourceCache#newSourceImageOutputStream(Identifier)}
     * when using {@link RetrievalStrategy#CACHE}.
     */
    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithCacheStrategyAndSourceCacheNewSourceImageOutputStreamThrowingException()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC,
                    MockStreamSource.class.getName());
            config.setProperty(Key.SOURCE_CACHE,
                    MockBrokenSourceInputStreamCache.class.getSimpleName());
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final StreamProcessor processor = new MockStreamProcessor();

            assertThrows(IOException.class,
                    () -> instance.connect(source, processor, IDENTIFIER, Format.JPG));
        } finally {
            server.stop();
        }
    }

    /**
     * Tests that {@link ProcessorConnector#connect(Source, Processor,
     * Identifier, Format)} recovers when using {@link RetrievalStrategy#CACHE}
     * and {@link
     * edu.illinois.library.cantaloupe.cache.SourceCache#getSourceImageFile(Identifier)}
     * throws an {@link IOException} only once.
     */
    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithCacheStrategyAndSourceCacheGetSourceImageFileRetries()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC,
                    MockStreamSource.class.getName());
            config.setProperty(Key.SOURCE_CACHE,
                    MockUnreliableSourceImageFileCache.class.getSimpleName());
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final StreamProcessor processor = new MockStreamProcessor();

            assertNull(instance.connect(source, processor, IDENTIFIER, Format.JPG));

            assertEqualSources(
                    CacheFactory.getSourceCache().get().getSourceImageFile(IDENTIFIER).orElseThrow(),
                    processor.getStreamFactory());
        } finally {
            server.stop();
        }
    }

    /**
     * Tests that {@link ProcessorConnector#connect(Source, Processor,
     * Identifier, Format)} recovers when using {@link RetrievalStrategy#CACHE}
     * and {@link
     * edu.illinois.library.cantaloupe.cache.SourceCache#newSourceImageOutputStream(Identifier)}
     * throws an {@link IOException} only once.
     */
    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithCacheStrategyAndSourceCacheNewSourceImageOutputStreamRetries()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "HttpSource");
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.SOURCE_CACHE,
                    MockUnreliableSourceOutputStreamCache.class.getSimpleName());
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final StreamProcessor processor = new MockStreamProcessor();

            assertNull(instance.connect(source, processor, IDENTIFIER, Format.JPG));

            assertEqualSources(
                    CacheFactory.getSourceCache().get().getSourceImageFile(IDENTIFIER).orElseThrow(),
                    processor.getStreamFactory());
        } finally {
            server.stop();
        }
    }

    @Test
    void testConnectWithStreamSourceAndStreamProcessorWithCacheStrategyAndSourceCacheDisabled()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC,
                    MockStreamSource.class.getName());
            config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                    RetrievalStrategy.CACHE.getConfigValue());
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());

            final Source source = new SourceFactory().newSource(IDENTIFIER, null);
            final Processor processor = new MockStreamProcessor();

            assertThrows(CacheDisabledException.class,
                    () -> instance.connect(source, processor, IDENTIFIER, Format.JPG));
        } finally {
            server.stop();
            recursiveDeleteOnExit(cacheFolder);
        }
    }

    private void assertEqualSources(Path path, StreamFactory ss)
            throws IOException {
        assertEqualSources(Files.newInputStream(path), ss.newInputStream());
    }

    private void assertEqualSources(StreamFactory ss1, StreamFactory ss2)
            throws IOException {
        assertEqualSources(ss1.newInputStream(), ss2.newInputStream());
    }

    private void assertEqualSources(InputStream is1, InputStream is2)
            throws IOException {
        try (ByteArrayOutputStream os1 = new ByteArrayOutputStream();
             ByteArrayOutputStream os2 = new ByteArrayOutputStream()) {
            is1.transferTo(os1);
            is2.transferTo(os2);
            assertArrayEquals(os1.toByteArray(), os2.toByteArray());
        }
    }

}
