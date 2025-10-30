package edu.illinois.library.cantaloupe.delegate;

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

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import jakarta.annotation.PreDestroy;

/**
 * Spring-managed service that provides access to delegate proxy instances.
 * Uses dependency injection instead of Configuration.getInstance() for better testability.
 */
@Service
public class DelegateProxyService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateProxyService.class);

    static final String DELEGATE_SCRIPT_VM_ARGUMENT =
            "cantaloupe.delegate_script";

    // Static fallback instance for backward compatibility with non-Spring code
    private static DelegateProxyService staticInstance;
    private static boolean isScriptCodeLoaded;

    private final Configuration configuration;
    private ScriptWatcher scriptWatcher;
    private ScheduledExecutorService watcherExecutorService;
    private Future<?> watcherFuture;

    @Autowired
    public DelegateProxyService(Configuration configuration) {
        this.configuration = configuration;
        // Set the static instance for backward compatibility
        staticInstance = this;
    }

    /**
     * @return {@link JavaDelegate} instance, if available.
     */
    static Optional<JavaDelegate> getJavaDelegate() {
        ServiceLoader<JavaDelegate> services =
                ServiceLoader.load(JavaDelegate.class);
        return services.findFirst();
    }

    /**
     * @return Whether a Java delegate is available, or the delegate script is enabled.
     */
    public boolean isDelegateAvailable() {
        return getJavaDelegate().isPresent() || isScriptEnabled();
    }

    /**
     * Instance method for checking if script is enabled using injected Configuration.
     */
    public boolean isScriptEnabled() {
        return configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false);
    }

    /**
     * For testing only!
     */
    public static synchronized void clearInstance() {
        staticInstance = null;
    }

    /**
     * @return The shared instance. If the instance is being created from a
     *         script, the script code will be loaded into it.
     */
    public static synchronized DelegateProxyService getInstance() {
        if (staticInstance != null) {
            // Load script code if needed
            staticInstance.loadScriptCodeIfNeeded();
            return staticInstance;
        } else {
            // Fallback for non-Spring contexts - create instance with singleton Configuration
            var fallbackInstance = new DelegateProxyService(
                edu.illinois.library.cantaloupe.config.Configuration.getInstance());
            fallbackInstance.loadScriptCodeIfNeeded();
            return fallbackInstance;
        }
    }

    /**
     * Loads script code if using a delegate script and code hasn't been loaded yet.
     */
    private void loadScriptCodeIfNeeded() {
        if (getJavaDelegate().isEmpty() && !isScriptCodeLoaded &&
                configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)) {
            try {
                Path file = getScriptFileInternal();
                if (file != null) {
                    String code = Files.readString(file);
                    JRubyDelegateProxy.load(code);
                    isScriptCodeLoaded = true;
                }
            } catch (IOException | ScriptException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    /**
     * <p>Returns the absolute path to the delegate script, regardless of
     * whether the delegate script system is enabled. The path is obtained from the
     * delegate script VM argument, if set, or the configuration otherwise. If
     * neither are set, {@code null} is returned.</p>
     *
     * <p>The contents of the script are not validated.</p>
     *
     * @throws NoSuchFileException If the script specified in configuration does not exist.
     */
    public static Path getScriptFile() throws NoSuchFileException {
        if (staticInstance != null) {
            return staticInstance.getScriptFileInternal();
        } else {
            // Fallback for non-Spring contexts
            return getScriptFileWithConfiguration(
                edu.illinois.library.cantaloupe.config.Configuration.getInstance());
        }
    }

    /**
     * Instance method for getting script file using injected Configuration.
     */
    public Path getScriptFileInternal() throws NoSuchFileException {
        return getScriptFileWithConfiguration(configuration);
    }

    /**
     * Common logic for getting script file with a given Configuration instance.
     */
    private static Path getScriptFileWithConfiguration(Configuration config) throws NoSuchFileException {
        String value = System.getProperty("cantaloupe.delegate_script");
        if (value == null || value.isBlank()) {
            // The script name may be an absolute pathname or a filename.
            value = config.getString(Key.DELEGATE_SCRIPT_PATHNAME, "");
        }
        // Handle null values that might come from mocked configurations
        if (value != null && !value.isBlank()) {
            Path script = findScript(value, config);
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
     * @param config Configuration instance to use.
     */
    private static Path findScript(String pathname, Configuration config) {
        Path script = Paths.get(pathname);
        if (!script.isAbsolute()) {
            // Search for it in the same directory as the application config
            // (if available), or the current working directory if not.
            final Optional<Path> configFile = config.getFile();
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
        if (watcherExecutorService != null) {
            watcherExecutorService.shutdown();
            watcherExecutorService = null;
        }
    }

    /**
     * Cleanup method called when Spring context is destroyed.
     */
    @PreDestroy
    public void cleanup() {
        stopWatching();
    }
}
