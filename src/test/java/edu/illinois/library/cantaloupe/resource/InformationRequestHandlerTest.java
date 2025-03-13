package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.*;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.source.StatResult;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class InformationRequestHandlerTest extends BaseTest {

    @Nested
    public class BuilderTest {

        @Test
        void testBuildWithNoIdentifierSet() {
            assertThrows(IllegalArgumentException.class, () ->
                    InformationRequestHandler.builder().build());
        }

        @Test
        void testBuildWithDelegateProxyButNoRequestContextSet() {
            DelegateProxy delegateProxy = TestUtil.newDelegateProxy();
            assertThrows(IllegalArgumentException.class, () ->
                    InformationRequestHandler.builder()
                            .withIdentifier(new Identifier("cats"))
                            .withDelegateProxy(delegateProxy)
                            .build());
        }

    }

    private static class IntrospectiveCallback implements InformationRequestHandler.Callback {
        private boolean isAuthorizeCalled,
                isSourceAccessedCalled,
                isKnowAvailableOutputFormatsCalled;

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
        public void knowAvailableOutputFormats(Set<Format> formats) {
            isKnowAvailableOutputFormatsCalled = true;
        }

    }

    @Test
    void testHandleCallsAuthorizationCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withCallback(callback)
                .withIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"))
                .build()) {
            handler.handle();
            assertTrue(callback.isAuthorizeCalled);
        }
    }

    @Test
    void testHandleCallsSourceAccessedCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withCallback(callback)
                .withIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"))
                .build()) {
            handler.handle();
            assertTrue(callback.isSourceAccessedCalled);
        }
    }

    @Test
    void testHandleCallsAvailableOutputFormatsCallback() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withCallback(callback)
                .withIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"))
                .build()) {
            handler.handle();
            assertTrue(callback.isKnowAvailableOutputFormatsCalled);
        }
    }

    @Test
    void testHandleReturnsInstanceFromDerivativeCache() throws Exception {
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
        final Identifier identifier = new Identifier("jpg-rgb-64x48x8.jpg");
        final Metadata metadata     = new Metadata();

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

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withCallback(callback)
                .withIdentifier(identifier)
                .build()) {
            Info cachedInfo = handler.handle();
            assertEquals(info, cachedInfo);
        }
    }

    @Test
    void testHandleSetsRequestContextKeysBeforeReturningInstanceFromDerivativeCache()
            throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, false);
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
            config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
            config.setProperty(Key.DERIVATIVE_CACHE, "HeapCache");
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb").toString());
        }

        // Configure the request.
        final Identifier identifier = new Identifier("jpg-rgb-64x48x8.jpg");
        final Metadata metadata     = new Metadata();

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

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        final RequestContext context = new RequestContext();
        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withCallback(callback)
                .withIdentifier(identifier)
                .withRequestContext(context)
                .build()) {
            handler.handle();
            assertEquals(1, context.getPageCount());
            assertEquals(new Dimension(64, 48), context.getFullSize());
            assertNotNull(context.getMetadata());
        }
    }

    @Test
    void testHandleReturnsInstanceFromProcessor() throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withCallback(callback)
                .withIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"))
                .build()) {
            Info info = handler.handle();
            assertNotNull(info);
        }
    }

    @Test
    void testHandleSetsRequestContextPageCountBeforeReturningInstanceFromProcessor()
            throws Exception {
        {   // Configure the application.
            final Configuration config = Configuration.getInstance();
            config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getImagesPath() + "/");
        }

        final IntrospectiveCallback callback = new IntrospectiveCallback();
        final RequestContext context = new RequestContext();
        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withCallback(callback)
                .withIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"))
                .withRequestContext(context)
                .build()) {
            handler.handle();
            assertEquals(1, context.getPageCount());
        }
    }

    @Test
    void testHandleReturnsNullWhenAuthorizationFails() throws Exception {
        try (InformationRequestHandler handler = InformationRequestHandler.builder()
                .withCallback(new InformationRequestHandler.Callback() {
                    @Override
                    public boolean authorize() {
                        return false;
                    }
                    @Override
                    public void sourceAccessed(StatResult result) {
                    }
                    @Override
                    public void knowAvailableOutputFormats(Set<Format> availableOutputFormats) {
                    }
                })
                .withIdentifier(new Identifier("jpg-rgb-64x48x8.jpg"))
                .build()) {
            Info info = handler.handle();
            assertNull(info);
        }
    }

}
