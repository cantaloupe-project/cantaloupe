package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.http.Server;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.eclipse.jetty.server.Handler;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Wraps a {@link Server} for convenient functional testing.
 */
public final class WebServer {

    public static final String BASIC_REALM = "Test Realm";
    public static final String BASIC_USER = "user";
    public static final String BASIC_SECRET = "secret";

    private final Server wrappedServer = new Server();

    public WebServer() throws IOException {
        Path imagesPath = TestUtil.getFixturePath().resolve("images");
        wrappedServer.setRoot(imagesPath);
        wrappedServer.setKeyStorePath(TestUtil.getFixture("keystore.jks"));
        wrappedServer.setKeyStorePassword("password");
        wrappedServer.setKeyManagerPassword("password");
        wrappedServer.setHTTP1Enabled(true);
        wrappedServer.setHTTP2Enabled(true);
        wrappedServer.setHTTPS1Enabled(true);
        wrappedServer.setHTTPS2Enabled(SystemUtils.isALPNAvailable());
    }

    public URI getHTTPURI() {
        return wrappedServer.getHTTPURI();
    }

    public URI getHTTPSURI() {
        return wrappedServer.getHTTPSURI();
    }

    public void setAcceptingRanges(boolean isAcceptingRanges) {
        wrappedServer.setAcceptingRanges(isAcceptingRanges);
    }

    public void setBasicAuthEnabled(boolean enabled) {
        wrappedServer.setAuthRealm(BASIC_REALM);
        wrappedServer.setAuthUser(BASIC_USER);
        wrappedServer.setAuthSecret(BASIC_SECRET);
        wrappedServer.setBasicAuthEnabled(enabled);
    }

    public void setHandler(Handler handler) {
        wrappedServer.setHandler(handler);
    }

    public void setHTTP1Enabled(boolean enabled) {
        wrappedServer.setHTTP1Enabled(enabled);
    }

    public void setHTTP2Enabled(boolean enabled) {
        wrappedServer.setHTTP2Enabled(enabled);
    }

    public void setHTTPS1Enabled(boolean enabled) {
        wrappedServer.setHTTPS1Enabled(enabled);
    }

    public void setHTTPS2Enabled(boolean enabled) {
        wrappedServer.setHTTPS2Enabled(enabled);
    }

    public void start() throws Exception {
        wrappedServer.start();
    }

    public void stop() throws Exception {
        wrappedServer.stop();
    }

}
