package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheDisabledException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.IncompatibleResolverException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import static org.junit.Assert.*;

public class SourceImageWranglerTest extends BaseTest {

    private Identifier identifier = new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private static void recursiveDeleteOnExit(File dir) throws IOException {
        Path path = dir.toPath();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
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

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "FilesystemResolver");
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                TestUtil.getImage("jpg").getParentFile().getAbsolutePath() + "/");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
    }

    @Test
    public void testGetStreamProcessorRetrievalStrategy() {
        final Configuration config = Configuration.getInstance();
        // stream
        config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                "StreamStrategy");
        assertEquals(SourceImageWrangler.StreamProcessorRetrievalStrategy.STREAM,
                SourceImageWrangler.getStreamProcessorRetrievalStrategy());

        // cache
        config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                "CacheStrategy");
        assertEquals(SourceImageWrangler.StreamProcessorRetrievalStrategy.CACHE,
                SourceImageWrangler.getStreamProcessorRetrievalStrategy());
    }

    @Test
    public void testWrangleWithFileResolverAndFileProcessor() throws Exception {
        final Resolver resolver = ResolverFactory.getResolver(identifier);
        final Processor processor = new ProcessorFactory().getProcessor(Format.JPG);

        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        assertEquals(
                ((FileResolver) resolver).getFile(),
                ((FileProcessor) processor).getSourceFile());
    }

    @Test
    public void testWrangleWithFileResolverAndStreamProcessor()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "ImageMagickProcessor");

        final Resolver resolver = ResolverFactory.getResolver(identifier);
        final Processor processor = new ProcessorFactory().getProcessor(Format.JPG);

        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        StreamSource ss1 = ((StreamResolver) resolver).newStreamSource();
        StreamSource ss2 = ((StreamProcessor) processor).getStreamSource();

        assertEqualSources(ss1, ss2);
    }

    @Test
    public void testWrangleWithStreamResolverAndFileProcessorWithSourceCacheDisabled()
            throws Exception {
        identifier = new Identifier("jp2");
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
        config.setProperty(Key.PROCESSOR_FALLBACK, "OpenJpegProcessor");

        final Resolver resolver = ResolverFactory.getResolver(identifier);
        final Processor processor = new ProcessorFactory().getProcessor(Format.JP2);

        try {
            new SourceImageWrangler(resolver, processor, identifier).wrangle();
            fail("Expected exception");
        } catch (IncompatibleResolverException e) {
            // pass
        }
    }

    @Test
    public void testWrangleWithStreamResolverAndFileProcessor()
            throws Exception {
        final WebServer server = new WebServer();
        final File cacheFolder = TestUtil.getTempFolder();
        identifier = new Identifier("jp2");

        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.getAbsolutePath());
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getUri() + "/");
            config.setProperty(Key.PROCESSOR_FALLBACK, "OpenJpegProcessor");

            final Resolver resolver = ResolverFactory.getResolver(identifier);
            final Processor processor = new ProcessorFactory().getProcessor(Format.JP2);

            new SourceImageWrangler(resolver, processor, identifier).wrangle();

            assertEquals(
                    CacheFactory.getSourceCache().getSourceImageFile(identifier),
                    ((FileProcessor) processor).getSourceFile());
        } finally {
            server.stop();
            try {
                recursiveDeleteOnExit(cacheFolder);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    @Test
    public void testWrangleWithStreamResolverAndStreamProcessorWithStreamStrategy()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getUri() + "/");
            config.setProperty(Key.PROCESSOR_FALLBACK, "ImageMagickProcessor");

            final Resolver resolver = ResolverFactory.getResolver(identifier);
            final Processor processor = new ProcessorFactory().getProcessor(Format.JPG);

            new SourceImageWrangler(resolver, processor, identifier).wrangle();

            StreamSource ss1 = ((StreamResolver) resolver).newStreamSource();
            StreamSource ss2 = ((StreamProcessor) processor).getStreamSource();

            assertEqualSources(ss1, ss2);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testWrangleWithStreamResolverAndStreamProcessorWithCacheStrategy()
            throws Exception {
        final WebServer server = new WebServer();
        final File cacheFolder = TestUtil.getTempFolder();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getUri() + "/");
            config.setProperty(Key.PROCESSOR_FALLBACK, "ImageMagickProcessor");
            config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
            config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
            config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                    "CacheStrategy");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.getAbsolutePath());

            final Resolver resolver = ResolverFactory.getResolver(identifier);
            final Processor processor = new ProcessorFactory().getProcessor(Format.JPG);

            new SourceImageWrangler(resolver, processor, identifier).wrangle();

            assertEqualSources(
                    CacheFactory.getSourceCache().getSourceImageFile(identifier),
                    ((StreamProcessor) processor).getStreamSource());
        } finally {
            server.stop();
            try {
                recursiveDeleteOnExit(cacheFolder);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    @Test
    public void testWrangleWithStreamResolverAndStreamProcessorWithCacheStrategyAndSourceCacheDisabled()
            throws Exception {
        final WebServer server = new WebServer();
        final File cacheFolder = TestUtil.getTempFolder();
        try {
            server.start();

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
            config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                    server.getUri() + "/");
            config.setProperty(Key.PROCESSOR_FALLBACK, "ImageMagickProcessor");
            config.setProperty(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                    "CacheStrategy");
            config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                    cacheFolder.getAbsolutePath());

            final Resolver resolver = ResolverFactory.getResolver(identifier);
            final Processor processor = new ProcessorFactory().getProcessor(Format.JPG);

            try {
                new SourceImageWrangler(resolver, processor, identifier).wrangle();
                fail("Expected exception");
            } catch (CacheDisabledException e) {
                // pass
            }
        } finally {
            server.stop();
            try {
                recursiveDeleteOnExit(cacheFolder);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    private void assertEqualSources(File file, StreamSource ss)
            throws IOException {
        assertEqualSources(new FileInputStream(file), ss.newInputStream());
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
