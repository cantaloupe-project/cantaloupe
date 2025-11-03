package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.CompletableOutputStream;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.source.StatResult;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ImageRequestHandlerTest extends BaseTest {
    private static class IntrospectiveCallback implements ImageRequestHandler.Callback {
        private boolean isPreAuthorizeCalled, isAuthorizeCalled,
                isSourceAccessedCalled,
                isWillStreamImageFromDerivativeCacheCalled,
                isInfoAvailableCalled, isWillProcessImageCalled;

        @Override
        public boolean preAuthorize() {
            isPreAuthorizeCalled = true;
            return true;
        }

        @Override
        public boolean authorize() {
            isAuthorizeCalled = true;
            return true;
        }

        @Override
        public void sourceAccessed(StatResult result) {
            isSourceAccessedCalled = true;
        }

        @Override
        public void willStreamImageFromDerivativeCache() {
            isWillStreamImageFromDerivativeCacheCalled = true;
        }

        @Override
        public void infoAvailable(Info info) {
            isInfoAvailableCalled = true;
        }

        @Override
        public void willProcessImage(Processor processor, Info info) {
            isWillProcessImageCalled = true;
        }
    }

    private MockHttpServletRequest servletRequest;
    private IIIFRequest request;
    private Configuration configuration;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configuration = Configuration.getInstance();
        servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURL("http://example.org/iiif/2/foo");
        request = new IIIFRequest(servletRequest, Collections.emptyList(), configuration);
    }


    @Test
    void handleCallsPreAuthorizationCallback() throws Exception {
        {   // Configure the application.
            configuration.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isPreAuthorizeCalled);
        }
    }

    @Test
    void handleCallsAuthorizationCallback() throws Exception {
        {   // Configure the application.
            configuration.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isAuthorizeCalled);
        }
    }

    @Test
    void handleCallsSourceAccessedCallback() throws Exception {
        { // Configure the application.
            configuration.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isSourceAccessedCalled);
        }
    }

    @Test
    void handleCallsCacheStreamingCallback() throws Exception {
        {   // Configure the application.
            configuration.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
            configuration.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            configuration.setProperty(Key.DERIVATIVE_CACHE, "HeapCache");
            configuration.setProperty(Key.HEAPCACHE_TARGET_SIZE, "1MB");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        final Identifier identifier = new Identifier("jpg-rgb-64x48x8.jpg");
        final Metadata metadata     = new Metadata();
        opList.setIdentifier(identifier);
        Encode encode = new Encode(Format.get("jpg"));
        encode.setCompression(Compression.JPEG);
        encode.setQuality(80);
        encode.setMetadata(metadata);
        opList.add(encode);

        // Add an info to the derivative cache.
        CacheFacade facade = new CacheFacade(configuration);
        DerivativeCache cache = facade.getDerivativeCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 48)
                .withFormat(Format.get("jpg"))
                .withIdentifier(identifier)
                .withMetadata(metadata)
                .build();
        cache.put(identifier, info);

        // Add an "image" to the derivative cache.
        try (CompletableOutputStream os =
                     cache.newDerivativeImageOutputStream(opList)) {
            os.write(new byte[] { 0x35, 0x35, 0x35 });
            os.setComplete(true);
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isWillStreamImageFromDerivativeCacheCalled);
        }
    }

    @Test
    void handleCallsInfoAvailableCallback() throws Exception {
        { // Configure the application.
            configuration.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isInfoAvailableCalled);
        }
    }

    @Test
    void handleCallsProcessingCallback() throws Exception {
        { // Configure the application.
            configuration.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isWillProcessImageCalled);
        }
    }

    @Test
    void handleProcessesImage() throws Exception {
        { // Configure the application.
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertTrue(outputStream.toByteArray().length > 5000);
        }
    }

    @Test
    void handleStreamsFromDerivativeCache() throws Exception {
        {   // Configure the application.
            configuration.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
            configuration.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            configuration.setProperty(Key.DERIVATIVE_CACHE, "HeapCache");
            configuration.setProperty(Key.HEAPCACHE_TARGET_SIZE, "1MB");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        final Identifier identifier = new Identifier("jpg-rgb-64x48x8.jpg");
        final Metadata metadata     = new Metadata();
        opList.setIdentifier(identifier);
        Encode encode = new Encode(Format.get("jpg"));
        encode.setCompression(Compression.JPEG);
        encode.setQuality(80);
        encode.setMetadata(metadata);
        opList.add(encode);

        // Add an info to the derivative cache.
        CacheFacade facade = new CacheFacade(configuration);
        DerivativeCache cache = facade.getDerivativeCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 48)
                .withFormat(Format.get("jpg"))
                .withIdentifier(identifier)
                .withMetadata(metadata)
                .build();
        cache.put(identifier, info);

        // Add an "image" to the derivative cache.
        final byte[] expected = new byte[] { 0x35, 0x35, 0x35 };
        try (CompletableOutputStream os =
                     cache.newDerivativeImageOutputStream(opList)) {
            os.write(expected);
            os.setComplete(true);
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertArrayEquals(expected, outputStream.toByteArray());
        }
    }

    @Test
    void handleWithFailedPreAuthorization() throws Exception {
        { // Configure the application.
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                new ImageRequestHandler.Callback() {
                    @Override
                    public boolean preAuthorize() {
                        return false;
                    }
                    @Override
                    public boolean authorize() {
                        return true;
                    }
                    @Override
                    public void sourceAccessed(StatResult result) {
                    }
                    @Override
                    public void willStreamImageFromDerivativeCache() {
                    }
                    @Override
                    public void infoAvailable(Info info) {
                    }
                    @Override
                    public void willProcessImage(Processor processor, Info info) {
                    }
                },
                configuration);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertEquals(0, outputStream.toByteArray().length);
        }
    }

    @Test
    void handleWithFailedAuthorization() throws Exception {
        { // Configure the application.
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                new ImageRequestHandler.Callback() {
                    @Override
                    public boolean preAuthorize() {
                        return true;
                    }
                    @Override
                    public boolean authorize() {
                        return false;
                    }
                    @Override
                    public void sourceAccessed(StatResult result) {
                    }
                    @Override
                    public void willStreamImageFromDerivativeCache() {
                    }
                    @Override
                    public void infoAvailable(Info info) {
                    }
                    @Override
                    public void willProcessImage(Processor processor, Info info) {
                    }
                },
                configuration);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertEquals(0, outputStream.toByteArray().length);
        }
    }

    @Test
    void handleWithIllegalPageIndex() throws Exception {
        { // Configure the application.
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"))
                .withPageIndex(9999)
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            assertThrows(IllegalClientArgumentException.class, () ->
                    handler.handle(outputStream));
        }
    }

    @Test
    void handleWithInvalidOperationList() throws Exception {
        { // Configure the application.
            configuration.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            configuration.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList =
                new OperationList(new Identifier("jpg-rgb-64x48x8.jpg"));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                request,
                callback,
                configuration);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            assertThrows(ValidationException.class, () ->
                    handler.handle(outputStream));
        }
    }

    @Test
    void handleDeletesIncompatibleSourceCachedImageWhenSoConfigured()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            {   // Configure the application.
                configuration.setProperty(Key.SOURCE_STATIC, "HttpSource");
                configuration.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                        server.getHTTPURI().toString() + "/");
                configuration.setProperty(Key.PROCESSOR_FALLBACK,
                        edu.illinois.library.cantaloupe.processor.MockStreamProcessor.class.getName());
                configuration.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                        "CacheStrategy");
                configuration.setProperty(Key.PROCESSOR_PURGE_INCOMPATIBLE_FROM_SOURCE_CACHE,
                        true); // what this test is testing
                configuration.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
                configuration.setProperty(Key.SOURCE_CACHE_TTL, 300);
                configuration.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                        Application.getTempPath().toString());
            }

            // Configure the request.
            final OperationList opList = new OperationList();
            final Identifier identifier = new Identifier("jpg-rgb-64x48x8.jpg");
            final Metadata metadata = new Metadata();
            opList.setIdentifier(identifier);
            Encode encode = new Encode(Format.get("jpg"));
            encode.setCompression(Compression.JPEG);
            encode.setQuality(80);
            encode.setMetadata(metadata);
            opList.add(encode);

            final CacheFacade cacheFacade = new CacheFacade(configuration);
            ImageRequestHandler.Callback callback =new ImageRequestHandler.Callback() {
                @Override
                public boolean preAuthorize() {
                    return true;
                }
                @Override
                public boolean authorize() {
                    return true;
                }
                @Override
                public void sourceAccessed(StatResult result) {
                }
                @Override
                public void willStreamImageFromDerivativeCache() {
                }
                @Override
                public void infoAvailable(Info info) {
                }
                @Override
                public void willProcessImage(Processor processor, Info info) {
                }
            };

            try (ImageRequestHandler handler = new ImageRequestHandler(opList,
                                                                       request,
                                                                       callback,
                                                                       configuration);
                 OutputStream outputStream = OutputStream.nullOutputStream()) {
                // The first request should cause the source image to be
                // source-cached...
                handler.handle(outputStream);
                // Overwrite the source-cached image with garbage, destroying
                // any format-signifying magic bytes.
                Path file = cacheFacade.getSourceCacheFile(identifier).get();
                Files.write(file, "This is garbage".getBytes(StandardCharsets.UTF_8));
                // Send the same request again. The source cache will be
                // consulted instead of the source.
                handler.handle(outputStream);
                fail("Expected a SourceFormatException");
            } catch (SourceFormatException e) {
                // The delete happens asynchronously, so give it some time.
                Thread.sleep(2000);
                assertFalse(cacheFacade.getSourceCacheFile(identifier).isPresent());
            }
        } finally {
            server.stop();
        }
    }

    @Test
    void handleDoesNotDeleteIncompatibleSourceCachedImageWhenNotConfiguredTo()
            throws Exception {
        final WebServer server = new WebServer();
        try {
            server.start();

            {   // Configure the application.
                configuration.setProperty(Key.SOURCE_STATIC, "HttpSource");
                configuration.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                        server.getHTTPURI().toString() + "/");
                configuration.setProperty(Key.PROCESSOR_FALLBACK,
                        edu.illinois.library.cantaloupe.processor.MockStreamProcessor.class.getName());
                configuration.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                        "CacheStrategy");
                configuration.setProperty(Key.PROCESSOR_PURGE_INCOMPATIBLE_FROM_SOURCE_CACHE,
                        false); // what this test is testing
                configuration.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
                configuration.setProperty(Key.SOURCE_CACHE_TTL, 300);
                configuration.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                        Application.getTempPath().toString());
            }

            // Configure the request.
            final OperationList opList = new OperationList();
            final Identifier identifier = new Identifier("jpg-rgb-64x48x8.jpg");
            final Metadata metadata = new Metadata();
            opList.setIdentifier(identifier);
            Encode encode = new Encode(Format.get("jpg"));
            encode.setCompression(Compression.JPEG);
            encode.setQuality(80);
            encode.setMetadata(metadata);
            opList.add(encode);

            final CacheFacade cacheFacade = new CacheFacade(configuration);

            ImageRequestHandler.Callback callback =new ImageRequestHandler.Callback() {
            @Override
                public boolean preAuthorize() {
                    return true;
                }
                @Override
                public boolean authorize() {
                    return true;
                }
                @Override
                public void sourceAccessed(StatResult result) {
                }
                @Override
                public void willStreamImageFromDerivativeCache() {
                }
                @Override
                public void infoAvailable(Info info) {
                }
                @Override
                public void willProcessImage(Processor processor, Info info) {
                }
            };

            try (ImageRequestHandler handler = new ImageRequestHandler(opList,
                                                                       request,
                                                                       callback,
                                                                       configuration);
                 OutputStream outputStream = OutputStream.nullOutputStream()) {
                // The first request should cause the source image to be
                // source-cached...
                handler.handle(outputStream);
                // Overwrite the source-cached image with garbage, destroying
                // any format-signifying magic bytes.
                Path file = cacheFacade.getSourceCacheFile(identifier).get();
                Files.write(file, "This is garbage".getBytes(StandardCharsets.UTF_8));
                // Send the same request again. The source cache will be
                // consulted instead of the source.
                handler.handle(outputStream);
                fail("Expected a SourceFormatException");
            } catch (SourceFormatException e) {
                // The source-cached file is not supposed to get deleted, but
                // it will happen asynchronously if it does, so give it some
                // time.
                Thread.sleep(2000);
                Path file = cacheFacade.getSourceCacheFile(identifier).get();
                assertTrue(Files.exists(file));
                Files.delete(file);
            }
        } finally {
            server.stop();
        }
    }

}
