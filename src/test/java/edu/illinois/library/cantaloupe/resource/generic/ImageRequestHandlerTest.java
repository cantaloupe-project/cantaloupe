package edu.illinois.library.cantaloupe.resource.generic;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.*;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ImageRequestHandlerTest extends BaseTest {

    @Nested
    public class BuilderTest {

        @Test
        void testBuildWithNoOperationListSet() {
            assertThrows(NullPointerException.class, () ->
                    ImageRequestHandler.builder().build());
        }

        @Test
        void testBuildWithDelegateProxyButNoRequestContextSet() throws Exception {
            var config = Configuration.getInstance();
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);

            RequestContext context = new RequestContext();
            DelegateProxy delegateProxy =
                    DelegateProxyService.getInstance().newDelegateProxy(context);
            assertThrows(NullPointerException.class, () ->
                    ImageRequestHandler.builder()
                            .withOperationList(new OperationList())
                            .withDelegateProxy(delegateProxy)
                            .build());
        }

        @Test
        void testBuildWithRequestContextButNoDelegateProxySet() {
            assertThrows(NullPointerException.class, () ->
                    ImageRequestHandler.builder()
                            .withOperationList(new OperationList())
                            .withRequestContext(new RequestContext())
                            .build());
        }

    }

    private static class IntrospectiveCallback implements ImageRequestHandler.Callback {
        private boolean isAuthorizeCalled,
                isWillStreamImageFromDerivativeCacheCalled,
                isWillProcessImageCalled;

        @Override
        public boolean authorize() {
            isAuthorizeCalled = true;
            return true;
        }

        @Override
        public void willStreamImageFromDerivativeCache() {
            isWillStreamImageFromDerivativeCacheCalled = true;
        }

        @Override
        public void willProcessImage(Processor processor, Info info) {
            isWillProcessImageCalled = true;
        }
    }

    @Test
    void testHandleCallsAuthorizationCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath().toString() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.JPG));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withCallback(callback)
                .withOperationList(opList)
                .build();
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isAuthorizeCalled);
        }
    }

    @Test
    void testHandleCallsCacheStreamingCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath().toString() + "/");
            config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            config.setProperty(Key.DERIVATIVE_CACHE, "HeapCache");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        final Identifier identifier = new Identifier("jpg-rgb-64x48x8.jpg");
        final Metadata metadata     = new Metadata();
        opList.setIdentifier(identifier);
        Encode encode = new Encode(Format.JPG);
        encode.setCompression(Compression.JPEG);
        encode.setQuality(80);
        encode.setMetadata(metadata);
        opList.add(encode);

        // Add an info to the derivative cache.
        CacheFacade facade = new CacheFacade();
        DerivativeCache cache = facade.getDerivativeCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 48)
                .withFormat(Format.JPG)
                .withIdentifier(identifier)
                .withMetadata(metadata)
                .build();
        cache.put(identifier, info);

        // Add an "image" to the derivative cache.
        try (OutputStream os = cache.newDerivativeImageOutputStream(opList)) {
            os.write(new byte[] { 0x35, 0x35, 0x35 });
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withCallback(callback)
                .withOperationList(opList)
                .build();
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isWillStreamImageFromDerivativeCacheCalled);
        }
    }

    @Test
    void testHandleCallsProcessingCallback() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath().toString() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.JPG));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withCallback(callback)
                .withOperationList(opList)
                .build();
             OutputStream outputStream = OutputStream.nullOutputStream()) {
            handler.handle(outputStream);
            assertTrue(callback.isWillProcessImageCalled);
        }
    }

    @Test
    void testHandleProcessesImage() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath().toString() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.JPG));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withCallback(callback)
                .withOperationList(opList)
                .build();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertTrue(outputStream.toByteArray().length > 5000);
        }
    }

    @Test
    void testHandleStreamsFromDerivativeCache() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath().toString() + "/");
            config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            config.setProperty(Key.DERIVATIVE_CACHE, "HeapCache");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        final Identifier identifier = new Identifier("jpg-rgb-64x48x8.jpg");
        final Metadata metadata     = new Metadata();
        opList.setIdentifier(identifier);
        Encode encode = new Encode(Format.JPG);
        encode.setCompression(Compression.JPEG);
        encode.setQuality(80);
        encode.setMetadata(metadata);
        opList.add(encode);

        // Add an info to the derivative cache.
        CacheFacade facade = new CacheFacade();
        DerivativeCache cache = facade.getDerivativeCache().orElseThrow();
        Info info = Info.builder()
                .withSize(64, 48)
                .withFormat(Format.JPG)
                .withIdentifier(identifier)
                .withMetadata(metadata)
                .build();
        cache.put(identifier, info);

        // Add an "image" to the derivative cache.
        final byte[] expected = new byte[] { 0x35, 0x35, 0x35 };
        try (OutputStream os = cache.newDerivativeImageOutputStream(opList)) {
            os.write(expected);
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withCallback(callback)
                .withOperationList(opList)
                .build();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertArrayEquals(expected, outputStream.toByteArray());
        }
    }

    @Test
    void testHandleWithFailedAuthorization() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath().toString() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.JPG));

        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withCallback(new ImageRequestHandler.Callback() {
                    @Override
                    public boolean authorize() {
                        return false;
                    }
                    @Override
                    public void willStreamImageFromDerivativeCache() {
                    }
                    @Override
                    public void willProcessImage(Processor processor, Info info) {
                    }
                })
                .withOperationList(opList)
                .build();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handle(outputStream);
            assertEquals(0, outputStream.toByteArray().length);
        }
    }

    @Test
    void testHandleWithIllegalPageIndex() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath().toString() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.JPG));

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withCallback(callback)
                .withOperationList(opList)
                .withPageIndex(99999)
                .build();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            assertThrows(IllegalClientArgumentException.class, () ->
                    handler.handle(outputStream));
        }
    }

    @Test
    void testHandleWithInvalidOperationList() throws Exception {
        { // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath().toString() + "/");
        }

        // Configure the request.
        final OperationList opList  = new OperationList();
        opList.setIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"));
        opList.add(new Encode(Format.JPG));
        opList.getOptions().put("page", "-1"); // invalid

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (ImageRequestHandler handler = ImageRequestHandler.builder()
                .withCallback(callback)
                .withOperationList(opList)
                .build();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            assertThrows(ValidationException.class, () ->
                    handler.handle(outputStream));
        }
    }

}
