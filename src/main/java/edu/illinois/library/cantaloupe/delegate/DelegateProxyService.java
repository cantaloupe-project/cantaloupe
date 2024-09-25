package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides access to the shared {@link DelegateProxy} instance.
 */
public final class DelegateProxyService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateProxyService.class);

    static final String DELEGATE_SCRIPT_VM_ARGUMENT =
            "cantaloupe.delegate_script";

    private static DelegateProxyService instance;

    private static boolean isScriptCodeLoaded;

    private ScriptWatcher scriptWatcher;

    private ScheduledExecutorService watcherExecutorService;

    private Future<?> watcherFuture;

    /**
     * @return {@link JavaDelegate} instance, if available.
     */
    static Optional<JavaDelegate> getJavaDelegate() {
        ServiceLoader<JavaDelegate> services =
                ServiceLoader.load(JavaDelegate.class);
        return services.findFirst();
    }

    /**
     * @return Whether a Java delegate is available, or the {@link
     *         #isScriptEnabled() delegate script is enabled}.
     */
    public static boolean isDelegateAvailable() {
        return getJavaDelegate().isPresent() || isScriptEnabled();
    }

    /**
     * @return Whether the delegate script is enabled.
     */
    public static boolean isScriptEnabled() {
        var config = Configuration.getInstance();
        return config.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false);
    }

    /**
     * For testing only!
     */
    static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * @return The shared instance. If the instance is being created from a
     *         script, the {@link #getScriptFile() script code} will be loaded
     *         into it.
     */
    public static synchronized DelegateProxyService getInstance() {
        if (instance == null) {
            instance = new DelegateProxyService();
        }
        // If we are using a delegate script, load the code into it.
        if (getJavaDelegate().isEmpty() && !isScriptCodeLoaded &&
                Configuration.getInstance().getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)) {
            try {
                Path file = getScriptFile();
                if (file != null) {
                    String code = Files.readString(file);
                    JRubyDelegateProxy.load(code);
                    isScriptCodeLoaded = true;
                }
            } catch (IOException | ScriptException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return instance;
    }

    /**
     * <p>Returns the absolute path to the delegate script, regardless of
     * whether the delegate script system is {@link #isScriptEnabled()
     * enabled}. The path is obtained from the {@link
     * #DELEGATE_SCRIPT_VM_ARGUMENT delegate script VM argument}, if set, or
     * the {@link Key#DELEGATE_SCRIPT_PATHNAME configuration} otherwise. If
     * neither are set, set, {@code null} is returned.</p>
     *
     * <p>The contents of the script are not validated.</p>
     *
     * @throws NoSuchFileException If the script specified in {@link
     *         Key#DELEGATE_SCRIPT_PATHNAME} does not exist.
     */
    static Path getScriptFile() throws NoSuchFileException {
        String value = System.getProperty("cantaloupe.delegate_script");
        if (value == null || value.isBlank()) {
            final Configuration config = Configuration.getInstance();
            // The script name may be an absolute pathname or a filename.
            value = config.getString(Key.DELEGATE_SCRIPT_PATHNAME, "");
        }
        if (!value.isBlank()) {
            Path script = findScript(value);
            if (!Files.exists(script)) {
                throw new NoSuchFileException("File not found: " + script);
            }
            return script;
        }
        return null;
    }

    /**
     * Finds the canonical location of a script based on the given filename or
     * absolute pathname. Existence of the underlying file is not checked.
     *
     * @param pathname Pathname or filename.
     */
    private static Path findScript(String pathname) {
        Path script = Paths.get(pathname);
        if (!script.isAbsolute()) {
            // Search for it in the same directory as the application config
            // (if available), or the current working directory if not.
            final Optional<Path> configFile = Configuration.getInstance().getFile();
            if (configFile.isPresent()) {
                script = configFile.get().getParent().resolve(script.getFileName());
            } else {
                script = Paths.get(".", script.getFileName().toString());
            }
            script = script.toAbsolutePath();
        }
        return script;
    }

    /**
     * @param code Code to load into the script interpreter.
     */
    public static void load(String code) throws ScriptException {
        JRubyDelegateProxy.load(code);
    }

    /**
     * <p>Acquires a new {@link DelegateProxy} instance, which may be backed
     * by either a {@link JavaDelegate}, if present on the classpath, or a
     * script-based delegate.</p>
     *
     * <p>This should normally be called only once at the beginning of a
     * request lifecycle, and the returned object passed around to wherever it
     * is needed.</p>
     *
     * @param context Request context.
     * @return        Shared delegate proxy.
     * @throws UnavailableException if a delegate is not available.
     */
    public DelegateProxy newDelegateProxy(RequestContext context)
            throws UnavailableException {
        if (getJavaDelegate().isPresent()) {
            LOGGER.debug("Instantiating a {}",
                    JavaDelegate.class.getSimpleName());
            var proxy = new JavaDelegateProxy(getJavaDelegate().get());
            proxy.setRequestContext(context);
            return proxy;
        } else if (isScriptEnabled()) {
            LOGGER.debug("Instantiating a {}",
                    JRubyDelegateProxy.class.getSimpleName());
            try {
                var proxy = new JRubyDelegateProxy();
                proxy.setRequestContext(context);
                return proxy;
            } catch (ScriptException e) {
                LOGGER.error("newDelegateProxy(): {}", e.getMessage(), e);
            }
        }
        throw new UnavailableException();
    }

    /**
     * Starts watching the delegate script for changes, only if the {@link
     * #getJavaDelegate() Java delegate} is not available.
     */
    public void startWatching() {
        if (getJavaDelegate().isPresent()) {
            return;
        }
        if (scriptWatcher == null) {
            scriptWatcher = new ScriptWatcher();
        }
        if (watcherExecutorService == null) {
            watcherExecutorService =
                    Executors.newSingleThreadScheduledExecutor();
        }
        watcherFuture = watcherExecutorService.submit(scriptWatcher);
    }

    /**
     * Stops watching the delegate script for changes, only if the {@link
     * #getJavaDelegate() Java delegate} is not available.
     */
    public void stopWatching() {
        if (getJavaDelegate().isPresent()) {
            return;
        }
        if (scriptWatcher != null) {
            scriptWatcher.stop();
            scriptWatcher = null;
        }
        if (watcherFuture != null) {
            watcherFuture.cancel(true);
        }
        watcherExecutorService.shutdown();
        watcherExecutorService = null;
    }

}
