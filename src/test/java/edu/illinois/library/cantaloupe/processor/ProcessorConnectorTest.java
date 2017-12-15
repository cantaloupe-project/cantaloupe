package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheDisabledException;
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
    public void getStreamProcessorRetrievalStrategy() {
        final Configuration config = Configuration.getInstance();
        // stream
        config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                "StreamStrategy");
        assertEquals(ProcessorConnector.StreamProcessorRetrievalStrategy.STREAM,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());

        // cache
        config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                "CacheStrategy");
        assertEquals(ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE,
                ProcessorConnector.getStreamProcessorRetrievalStrategy());
    }

    @Test
    public void connectWithFileResolverAndFileProcessor() throws Exception {
        final Resolver resolver = new ResolverFactory().newResolver(IDENTIFIER,
                new RequestContext());
        final Processor processor = new ProcessorFactory().newProcessor(Format.JPG);

        instance.connect(resolver, processor, IDENTIFIER);

        assertEquals(
                ((FileResolver) resolver).getPath().toFile(),
                ((FileProcessor) processor).getSourceFile());
    }

    @Test
    public void connectWithFileResolverAndStreamProcessor() throws Exception {
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

    @Test
    public void connectWithStreamResolverAndFileProcessorWithSourceCacheDisabled()
            throws Exception {
        final Identifier identifier = new Identifier("jp2");
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
        config.setProperty(Key.PROCESSOR_FALLBACK, "OpenJpegProcessor");

        final Resolver resolver = new ResolverFactory().
                newResolver(identifier, new RequestContext());
        final Processor processor = new ProcessorFactory().newProcessor(Format.JP2);

        try {
            instance.connect(resolver, processor, identifier);
            fail("Expected exception");
        } catch (IncompatibleResolverException e) {
            // pass
        }
    }

    @Test
    public void connectWithStreamResolverAndFileProcessor() throws Exception {
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
    public void connectWithStreamResolverAndStreamProcessorWithStreamStrategy()
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
    public void connectWithStreamResolverAndStreamProcessorWithCacheStrategy()
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
                    "CacheStrategy");
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

    @Test
    public void connectWithStreamResolverAndStreamProcessorWithCacheStrategyAndSourceCacheDisabled()
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
                    "CacheStrategy");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.toString());

            final Resolver resolver = new ResolverFactory().
                    newResolver(IDENTIFIER, new RequestContext());
            final Processor processor = new MockStreamProcessor();

            try {
                instance.connect(resolver, processor, IDENTIFIER);
                fail("Expected exception");
            } catch (CacheDisabledException e) {
                // pass
            }
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
