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
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ProcessorConnectorTest extends BaseTest {

    public static class StreamProcessorRetrievalStrategyTest extends BaseTest {

        @Test
        public void testGetConfigValue() {
            assertEquals("CacheStrategy",
                    ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.getConfigValue());
            assertEquals("StreamStrategy",
                    ProcessorConnector.StreamProcessorRetrievalStrategy.STREAM.getConfigValue());
        }

        @Test
        public void testToString() {
            assertEquals("CacheStrategy",
                    ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.toString());
            assertEquals("StreamStrategy",
                    ProcessorConnector.StreamProcessorRetrievalStrategy.STREAM.toString());
        }

    }

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private ProcessorConnector instance;

    private static void recursiveDeleteOnExit(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
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

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new ProcessorConnector();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "FilesystemResolver");
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                TestUtil.getImage("jpg").getParent().toString() + "/");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
    }

    @Test
    public void testGetStreamProcessorRetrievalStrategy() {
        final Configuration config = Configuration.getInstance();
        // config set to stream
        config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                "StreamStrategy");
        assertEquals(ProcessorConnector.StreamProcessorRetrievalStrategy.STREAM,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());

        // config set to cache
        config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.getConfigValue());
        assertEquals(ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());

        // config not set
        config.clearProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY);
        assertEquals(ProcessorConnector.StreamProcessorRetrievalStrategy.STREAM,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());
    }

    @Test
    public void testConnectWithFileResolverAndFileProcessor() throws Exception {
        final Resolver resolver = new ResolverFactory().newResolver(IDENTIFIER,
                new RequestContext());
        final Processor processor = new ProcessorFactory().newProcessor(Format.JPG);

        instance.connect(resolver, processor, IDENTIFIER);

        assertEquals(
                ((FileResolver) resolver).getPath(),
                ((FileProcessor) processor).getSourceFile());
    }

    @Test
    public void testConnectWithFileResolverAndStreamProcessor()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "ImageMagickProcessor");

        final Resolver resolver = new ResolverFactory().newResolver(IDENTIFIER,
                new RequestContext());
        final Processor processor = new ProcessorFactory().newProcessor(Format.JPG);

        instance.connect(resolver, processor, IDENTIFIER);

        StreamSource ss1 = ((StreamResolver) resolver).newStreamSource();
        StreamSource ss2 = ((StreamProcessor) processor).getStreamSource();

        assertEqualSources(ss1, ss2);
    }

    @Test(expected = IncompatibleResolverException.class)
    public void testConnectWithStreamResolverAndFileProcessorWithSourceCacheDisabled()
            throws Exception {
        final Identifier identifier = new Identifier("jp2");
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
        config.setProperty(Key.PROCESSOR_FALLBACK, "OpenJpegProcessor");

        final Resolver resolver = new ResolverFactory().
                newResolver(identifier, new RequestContext());
        final Processor processor = new ProcessorFactory().newProcessor(Format.JP2);

        instance.connect(resolver, processor, identifier);
    }

    @Test
    public void testConnectWithStreamResolverAndFileProcessor()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        final Identifier identifier = new Identifier("jp2");

        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.PROCESSOR_FALLBACK, "OpenJpegProcessor");

            final Resolver resolver = new ResolverFactory().
                    newResolver(identifier, new RequestContext());
            final Processor processor = new ProcessorFactory().newProcessor(Format.JP2);

            instance.connect(resolver, processor, identifier);

            assertEquals(
                    CacheFactory.getSourceCache().getSourceImageFile(identifier),
                    ((FileProcessor) processor).getSourceFile());
        } finally {
            server.stop();
            recursiveDeleteOnExit(cacheFolder);
        }
    }

    @Test
    public void testConnectWithStreamResolverAndStreamProcessorWithStreamStrategy()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getHTTPURI() + "/");

            final Resolver resolver = new ResolverFactory().
                    newResolver(IDENTIFIER, new RequestContext());
            final Processor processor = new MockStreamProcessor();

            instance.connect(resolver, processor, IDENTIFIER);

            StreamSource ss1 = ((StreamResolver) resolver).newStreamSource();
            StreamSource ss2 = ((StreamProcessor) processor).getStreamSource();

            assertEqualSources(ss1, ss2);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testConnectWithStreamResolverAndStreamProcessorWithCacheStrategyAndSourceCacheEnabled()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
            config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                    ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.getConfigValue());
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());

            final Resolver resolver = new ResolverFactory().
                    newResolver(IDENTIFIER, new RequestContext());
            final Processor processor = new MockStreamProcessor();

            instance.connect(resolver, processor, IDENTIFIER);

            assertEqualSources(
                    CacheFactory.getSourceCache().getSourceImageFile(IDENTIFIER),
                    ((StreamProcessor) processor).getStreamSource());
        } finally {
            server.stop();
            recursiveDeleteOnExit(cacheFolder);
        }
    }

    /**
     * Tests that {@link ProcessorConnector#connect} passes through an
     * {@link IOException} thrown by {@link
     * edu.illinois.library.cantaloupe.cache.SourceCache#getSourceImageFile(Identifier)}
     * when using
     * {@link edu.illinois.library.cantaloupe.processor.ProcessorConnector.StreamProcessorRetrievalStrategy#CACHE}.
     */
    @Test(expected = IOException.class)
    public void testConnectWithStreamResolverAndStreamProcessorWithCacheStrategyAndSourceCacheGetSourceImageFileThrowingException()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE,
                    MockBrokenSourceImageFileCache.class.getSimpleName());
            config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                    ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.getConfigValue());

            final Resolver resolver = new ResolverFactory().
                    newResolver(IDENTIFIER, new RequestContext());
            final Processor processor = new MockStreamProcessor();

            instance.connect(resolver, processor, IDENTIFIER);

            assertEqualSources(
                    CacheFactory.getSourceCache().getSourceImageFile(IDENTIFIER),
                    ((StreamProcessor) processor).getStreamSource());
        } finally {
            server.stop();
        }
    }

    /**
     * Tests that {@link ProcessorConnector#connect} passes through an
     * {@link IOException} thrown by {@link
     * edu.illinois.library.cantaloupe.cache.SourceCache#newSourceImageOutputStream(Identifier)}
     * when using
     * {@link edu.illinois.library.cantaloupe.processor.ProcessorConnector.StreamProcessorRetrievalStrategy#CACHE}.
     */
    @Test(expected = IOException.class)
    public void testConnectWithStreamResolverAndStreamProcessorWithCacheStrategyAndSourceCacheNewSourceImageOutputStreamThrowingException()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE,
                    MockBrokenSourceInputStreamCache.class.getSimpleName());
            config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                    ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.getConfigValue());

            final Resolver resolver = new ResolverFactory().
                    newResolver(IDENTIFIER, new RequestContext());
            final Processor processor = new MockStreamProcessor();

            instance.connect(resolver, processor, IDENTIFIER);

            assertEqualSources(
                    CacheFactory.getSourceCache().getSourceImageFile(IDENTIFIER),
                    ((StreamProcessor) processor).getStreamSource());
        } finally {
            server.stop();
        }
    }

    /**
     * Tests that {@link ProcessorConnector#connect} recovers when using
     * {@link edu.illinois.library.cantaloupe.processor.ProcessorConnector.StreamProcessorRetrievalStrategy#CACHE}
     * and {@link
     * edu.illinois.library.cantaloupe.cache.SourceCache#getSourceImageFile(Identifier)}
     * throws an {@link IOException} only once.
     */
    @Test
    public void testConnectWithStreamResolverAndStreamProcessorWithCacheStrategyAndSourceCacheGetSourceImageFileRetries()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE,
                    MockUnreliableSourceImageFileCache.class.getSimpleName());
            config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                    ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.getConfigValue());

            final Resolver resolver = new ResolverFactory().
                    newResolver(IDENTIFIER, new RequestContext());
            final Processor processor = new MockStreamProcessor();

            instance.connect(resolver, processor, IDENTIFIER);

            assertEqualSources(
                    CacheFactory.getSourceCache().getSourceImageFile(IDENTIFIER),
                    ((StreamProcessor) processor).getStreamSource());
        } finally {
            server.stop();
        }
    }

    /**
     * Tests that {@link ProcessorConnector#connect} recovers when using
     * {@link edu.illinois.library.cantaloupe.processor.ProcessorConnector.StreamProcessorRetrievalStrategy#CACHE}
     * and {@link
     * edu.illinois.library.cantaloupe.cache.SourceCache#newSourceImageOutputStream(Identifier)}
     * throws an {@link IOException} only once.
     */
    @Test
    public void testConnectWithStreamResolverAndStreamProcessorWithCacheStrategyAndSourceCacheNewSourceImageOutputStreamRetries()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE,
                    MockUnreliableSourceOutputStreamCache.class.getSimpleName());
            config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                    ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.getConfigValue());

            final Resolver resolver = new ResolverFactory().
                    newResolver(IDENTIFIER, new RequestContext());
            final Processor processor = new MockStreamProcessor();

            instance.connect(resolver, processor, IDENTIFIER);

            assertEqualSources(
                    CacheFactory.getSourceCache().getSourceImageFile(IDENTIFIER),
                    ((StreamProcessor) processor).getStreamSource());
        } finally {
            server.stop();
        }
    }

    @Test(expected = CacheDisabledException.class)
    public void testConnectWithStreamResolverAndStreamProcessorWithCacheStrategyAndSourceCacheDisabled()
            throws Exception {
        final WebServer server = new WebServer();
        final Path cacheFolder = Files.createTempDirectory("test");
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getHTTPURI() + "/");
            config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                    ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE.getConfigValue());
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());

            final Resolver resolver = new ResolverFactory().
                    newResolver(IDENTIFIER, new RequestContext());
            final Processor processor = new MockStreamProcessor();

            instance.connect(resolver, processor, IDENTIFIER);
        } finally {
            server.stop();
            recursiveDeleteOnExit(cacheFolder);
        }
    }

    private void assertEqualSources(Path path, StreamSource ss)
            throws IOException {
        assertEqualSources(Files.newInputStream(path), ss.newInputStream());
    }

    private void assertEqualSources(StreamSource ss1, StreamSource ss2)
            throws IOException {
        assertEqualSources(ss1.newInputStream(), ss2.newInputStream());
    }

    private void assertEqualSources(InputStream is1, InputStream is2)
            throws IOException {
        try {
            ByteArrayOutputStream os1 = new ByteArrayOutputStream();
            ByteArrayOutputStream os2 = new ByteArrayOutputStream();
            IOUtils.copy(is1, os1);
            IOUtils.copy(is2, os2);
            assertTrue(Arrays.equals(os1.toByteArray(), os2.toByteArray()));
        } finally {
            is1.close();
            is2.close();
        }
    }

}
