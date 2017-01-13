package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.SourceCacheDisabledException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Identifier;
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

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        config.setProperty("FilesystemResolver.lookup_strategy",
                "BasicLookupStrategy");
        config.setProperty("FilesystemResolver.BasicLookupStrategy.path_prefix",
                TestUtil.getImage("jpg").getParentFile().getAbsolutePath() + "/");
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "Java2dProcessor");
    }

    @Test
    public void testGetStreamProcessorRetrievalStrategy() {
        final Configuration config = ConfigurationFactory.getInstance();
        // stream
        config.setProperty(
                SourceImageWrangler.STREAMPROCESSOR_RETRIEVAL_STRATEGY_CONFIG_KEY,
                "StreamStrategy");
        assertEquals(SourceImageWrangler.StreamProcessorRetrievalStrategy.STREAM,
                SourceImageWrangler.getStreamProcessorRetrievalStrategy());

        // cache
        config.setProperty(
                SourceImageWrangler.STREAMPROCESSOR_RETRIEVAL_STRATEGY_CONFIG_KEY,
                "CacheStrategy");
        assertEquals(SourceImageWrangler.StreamProcessorRetrievalStrategy.CACHE,
                SourceImageWrangler.getStreamProcessorRetrievalStrategy());
    }

    @Test
    public void testWrangleWithFileResolverAndFileProcessor() throws Exception {
        final Resolver resolver = ResolverFactory.getResolver(identifier);
        final Processor processor = ProcessorFactory.getProcessor(Format.JPG);

        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        assertEquals(
                ((FileResolver) resolver).getFile(),
                ((FileProcessor) processor).getSourceFile());
    }

    @Test
    public void testWrangleWithFileResolverAndStreamProcessor()
            throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "ImageMagickProcessor");

        final Resolver resolver = ResolverFactory.getResolver(identifier);
        final Processor processor = ProcessorFactory.getProcessor(Format.JPG);

        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        StreamSource ss1 = ((StreamResolver) resolver).newStreamSource();
        StreamSource ss2 = ((StreamProcessor) processor).getStreamSource();

        assertEqualSources(ss1, ss2);
    }

    @Test
    public void testWrangleWithStreamResolverAndFileProcessorWithSourceCacheDisabled()
            throws Exception {
        identifier = new Identifier("jp2");
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "HttpResolver");
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "OpenJpegProcessor");

        final Resolver resolver = ResolverFactory.getResolver(identifier);
        final Processor processor = ProcessorFactory.getProcessor(Format.JP2);

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

            Configuration config = ConfigurationFactory.getInstance();
            config.setProperty(CacheFactory.SOURCE_CACHE_CONFIG_KEY,
                    "FilesystemCache");
            config.setProperty("FilesystemCache.pathname",
                    cacheFolder.getAbsolutePath());
            config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                    "HttpResolver");
            config.setProperty("HttpResolver.lookup_strategy",
                    "BasicLookupStrategy");
            config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                    server.getUri() + "/");
            config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                    "OpenJpegProcessor");

            final Resolver resolver = ResolverFactory.getResolver(identifier);
            final Processor processor = ProcessorFactory.getProcessor(Format.JP2);

            new SourceImageWrangler(resolver, processor, identifier).wrangle();

            assertEquals(
                    CacheFactory.getSourceCache().getImageFile(identifier),
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

            Configuration config = ConfigurationFactory.getInstance();
            config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                    "HttpResolver");
            config.setProperty("HttpResolver.lookup_strategy",
                    "BasicLookupStrategy");
            config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                    server.getUri() + "/");
            config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                    "ImageMagickProcessor");

            final Resolver resolver = ResolverFactory.getResolver(identifier);
            final Processor processor = ProcessorFactory.getProcessor(Format.JPG);

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

            Configuration config = ConfigurationFactory.getInstance();
            config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                    "HttpResolver");
            config.setProperty("HttpResolver.lookup_strategy",
                    "BasicLookupStrategy");
            config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                    server.getUri() + "/");
            config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                    "ImageMagickProcessor");
            config.setProperty(CacheFactory.SOURCE_CACHE_CONFIG_KEY,
                    "FilesystemCache");
            config.setProperty(SourceImageWrangler.STREAMPROCESSOR_RETRIEVAL_STRATEGY_CONFIG_KEY,
                    "CacheStrategy");
            config.setProperty("FilesystemCache.pathname",
                    cacheFolder.getAbsolutePath());

            final Resolver resolver = ResolverFactory.getResolver(identifier);
            final Processor processor = ProcessorFactory.getProcessor(Format.JPG);

            new SourceImageWrangler(resolver, processor, identifier).wrangle();

            assertEqualSources(
                    CacheFactory.getSourceCache().getImageFile(identifier),
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

            Configuration config = ConfigurationFactory.getInstance();
            config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                    "HttpResolver");
            config.setProperty("HttpResolver.lookup_strategy",
                    "BasicLookupStrategy");
            config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                    server.getUri() + "/");
            config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                    "ImageMagickProcessor");
            config.setProperty(SourceImageWrangler.STREAMPROCESSOR_RETRIEVAL_STRATEGY_CONFIG_KEY,
                    "CacheStrategy");
            config.setProperty("FilesystemCache.pathname",
                    cacheFolder.getAbsolutePath());

            final Resolver resolver = ResolverFactory.getResolver(identifier);
            final Processor processor = ProcessorFactory.getProcessor(Format.JPG);

            try {
                new SourceImageWrangler(resolver, processor, identifier).wrangle();
                fail("Expected exception");
            } catch (SourceCacheDisabledException e) {
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
        InputStream is1 = new FileInputStream(file);
        InputStream is2 = ss.newInputStream();
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

    private void assertEqualSources(StreamSource ss1, StreamSource ss2)
            throws IOException {
        InputStream is1 = ss1.newInputStream();
        InputStream is2 = ss2.newInputStream();
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
