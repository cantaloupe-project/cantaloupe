package edu.illinois.library.cantaloupe.script;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @see <a href="https://github.com/jruby/jruby/wiki/Embedding-with-JSR-223">
 *     Embedding JRuby with JSR223 - Code Examples</a>
 */
class RubyScriptEngine extends AbstractScriptEngine implements ScriptEngine {

    private static Logger logger = LoggerFactory.
            getLogger(RubyScriptEngine.class);

    /** Top-level Ruby module containing methods to invoke. */
    static final String TOP_MODULE = "Cantaloupe";

    private Cache<Object, Object> invocationCache;
    private final Object lock = new Object();
    private javax.script.ScriptEngine scriptEngine;
    private final AtomicBoolean scriptIsLoading = new AtomicBoolean(false);

    RubyScriptEngine() {
        final long maxSize = getMaxCacheSize();
        logger.info("Invocation cache limit: {}", maxSize);
        invocationCache = Caffeine.newBuilder().maximumSize(maxSize).build();
    }

    /**
     * @param methodName Name of the method being invoked.
     * @param args Method arguments.
     * @return The cache key corresponding to the given arguments.
     */
    private Object getCacheKey(String methodName, Object... args) {
        // The cache key is comprised of the method name at position 0 followed
        // by the arguments at succeeding positions.
        List<Object> key = Arrays.asList(args);
        key = new ArrayList<>(key);
        key.add(0, methodName);
        return key;
    }

    /**
     * @return The method invocation cache.
     */
    Cache<Object, Object> getInvocationCache() {
        return invocationCache;
    }

    private long getMaxCacheSize() {
        // TODO: this is very crude and needs tuning.
        final Runtime runtime = Runtime.getRuntime();
        return Math.round(runtime.maxMemory() / 1024f / 2f);
    }

    /**
     * @param methodName Full method name including module names.
     * @return Module name.
     * @throws ScriptException
     */
    String getModuleName(String methodName) {
        final String[] parts = StringUtils.split(methodName, "::");
        if (parts.length == 1) {
            return TOP_MODULE;
        }
        final List<String> partsArr = Arrays.asList(parts);
        return TOP_MODULE + "::" +
                StringUtils.join(partsArr.subList(0, partsArr.size() - 1), "::");
    }

    /**
     * @param methodName Full method name including module names.
     * @return Method name excluding module names.
     */
    String getUnqualifiedMethodName(String methodName) {
        String[] parts = StringUtils.split(methodName, "::");
        return parts[parts.length - 1];
    }

    /**
     * N.B. Clients should not modify the returned object nor any of its owned
     * objects, as this could disrupt the invocation cache.
     *
     * @param methodName Method to invoke, including all prefixes except the
     *                   top-level one in {@link #TOP_MODULE}.
     * @param args Arguments to pass to the method.
     * @return Return value of the method.
     * @throws ScriptException
     */
    @Override
    public Object invoke(String methodName, Object... args)
            throws ScriptException {
        final Stopwatch watch = new Stopwatch();

        // Block while the script is being (re)loaded.
        synchronized (lock) {
            while (scriptIsLoading.get()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        Object returnValue;
        final Configuration config = ConfigurationFactory.getInstance();
        if (config.getBoolean(METHOD_INVOCATION_CACHE_ENABLED_CONFIG_KEY, false)) {
            returnValue = retrieveFromCacheOrInvoke(methodName, args);
        } else {
            returnValue = doInvoke(methodName, args);
        }
        logger.debug("invoke({}::{}): exec time: {} msec",
                TOP_MODULE, methodName, watch.timeElapsed());
        return returnValue;
    }

    @Override
    public synchronized void load(String code) throws ScriptException {
        scriptIsLoading.set(true);
        logger.info("load(): loading script code");
        scriptEngine = new ScriptEngineManager().getEngineByName("jruby");
        scriptEngine.eval(code);
        scriptIsLoading.set(false);
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private Object retrieveFromCacheOrInvoke(String methodName, Object... args)
            throws ScriptException {
        final Object cacheKey = getCacheKey(methodName, args);
        Object returnValue = invocationCache.getIfPresent(cacheKey);
        if (returnValue != null) {
            logger.debug("invoke({}::{}): cache hit (skipping invocation)",
                    TOP_MODULE, methodName);
        } else {
            logger.debug("invoke({}::{}): cache miss", TOP_MODULE, methodName);
            returnValue = doInvoke(methodName, args);
            if (returnValue != null) {
                invocationCache.put(cacheKey, returnValue);
            }
        }
        return returnValue;
    }

    private Object doInvoke(String methodName, Object... args)
            throws ScriptException {
        try {
            return ((Invocable) scriptEngine).invokeMethod(
                    scriptEngine.eval(getModuleName(methodName)),
                    getUnqualifiedMethodName(methodName), args);
        } catch (NoSuchMethodException e) {
            throw new ScriptException(e);
        }
    }

}
