package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.CompletableOutputStream;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.*;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.source.StatResult;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void handleCallsPreAuthorizationCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
                callback,
                false,
                false);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isPreAuthorizeCalled);
        }
    }

    @Test
    void handleCallsAuthorizationCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
                callback,
                false,
                false);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isAuthorizeCalled);
        }
    }

    @Test
    void handleCallsSourceAccessedCallback() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
                callback,
                false,
                false);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isSourceAccessedCalled);
        }
    }

    @Test
    void handleCallsCacheStreamingCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
            config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            config.setProperty(Key.DERIVATIVE_CACHE, "HeapCache");
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
        CacheFacade facade = new CacheFacade();
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
                null,
                new RequestContext(),
                callback,
                false,
                false);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isWillStreamImageFromDerivativeCacheCalled);
        }
    }

    @Test
    void handleCallsInfoAvailableCallback() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
                callback,
                false,
                false);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isInfoAvailableCalled);
        }
    }

    @Test
    void handleCallsProcessingCallback() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
                callback,
                false,
                false);
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isWillProcessImageCalled);
        }
    }

    @Test
    void handleProcessesImage() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
                callback,
                false,
                false);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertTrue(outputStream.toByteArray().length > 5000);
        }
    }

    @Test
    void handleStreamsFromDerivativeCache() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
            config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            config.setProperty(Key.DERIVATIVE_CACHE, "HeapCache");
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
        CacheFacade facade = new CacheFacade();
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
                null,
                new RequestContext(),
                callback,
                false,
                false);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertArrayEquals(expected, outputStream.toByteArray());
        }
    }

    @Test
    void handleWithFailedPreAuthorization() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
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
                false,
                false);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertEquals(0, outputStream.toByteArray().length);
        }
    }

    @Test
    void handleWithFailedAuthorization() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.get("jpg")));

        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
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
                false,
                false);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertEquals(0, outputStream.toByteArray().length);
        }
    }

    @Test
    void handleWithIllegalPageIndex() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
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
                null,
                null,
                callback,
                false,
                false);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            assertThrows(IllegalClientArgumentException.class, () ->
                    handler.handle(outputStream));
        }
    }

    @Test
    void handleWithInvalidOperationList() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        // Configure the request.
        final OperationList opList =
                new OperationList(new Identifier("jpg-rgb-64x48x8.jpg"));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = new ImageRequestHandler(
                opList,
                null,
                new RequestContext(),
                callback,
                false,
                false);
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
                final Configuration config = Configuration.getInstance();
                config.setProperty(Key.SOURCE_STATIC, "HttpSource");
                config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                        server.getHTTPURI().toString() + "/");
                config.setProperty(Key.PROCESSOR_FALLBACK,
                        edu.illinois.library.cantaloupe.processor.MockStreamProcessor.class.getName());
                config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                        "CacheStrategy");
                config.setProperty(Key.PROCESSOR_PURGE_INCOMPATIBLE_FROM_SOURCE_CACHE,
                        true); // what this test is testing
                config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
                config.setProperty(Key.SOURCE_CACHE_TTL, 300);
                config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
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

            final CacheFacade cacheFacade = new CacheFacade();
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
            try (ImageRequestHandler handler = new ImageRequestHandler(opList, null, new RequestContext(), callback, false, false);
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
                final Configuration config = Configuration.getInstance();
                config.setProperty(Key.SOURCE_STATIC, "HttpSource");
                config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                        server.getHTTPURI().toString() + "/");
                config.setProperty(Key.PROCESSOR_FALLBACK,
                        edu.illinois.library.cantaloupe.processor.MockStreamProcessor.class.getName());
                config.setProperty(Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                        "CacheStrategy");
                config.setProperty(Key.PROCESSOR_PURGE_INCOMPATIBLE_FROM_SOURCE_CACHE,
                        false); // what this test is testing
                config.setProperty(Key.SOURCE_CACHE, "FilesystemCache");
                config.setProperty(Key.SOURCE_CACHE_TTL, 300);
                config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
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

            final CacheFacade cacheFacade = new CacheFacade();

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
            try (ImageRequestHandler handler = new ImageRequestHandler(opList, null, new RequestContext(), callback, false, false);
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
