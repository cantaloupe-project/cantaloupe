package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * <p>Proxy for a delegate object. Invokes delegate object methods, optionally
 * caching the invocations.</p>
 *
 * <p>Instances should be acquired via {@link
 * DelegateProxyService#newDelegateProxy(RequestContext)}.</p>
 *
 * <p>Method return values should not be modified as this could disrupt the
 * {@link InvocationCache}.</p>
 *
 * @see <a href="https://github.com/jruby/jruby/wiki/RedBridge">JRuby Embed</a>
 * @see <a href="https://github.com/jruby/jruby/wiki/Embedding-with-JSR-223">
 *     Embedding JRuby with JSR223 - Code Examples</a>
 */
public final class DelegateProxy {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateProxy.class);

    /**
     * Name of the Ruby delegate class.
     */
    private static final String RUBY_DELEGATE_CLASS_NAME = "CustomDelegate";

    /**
     * Name of the setter method used to set the request context. Must be
     * present in the {@link #RUBY_DELEGATE_CLASS_NAME delegate class}.
     */
    private static final String RUBY_REQUEST_CONTEXT_SETTER = "context=";

    /**
     * JSR-223 interface to the script interpreter. Invoke methods by casting
     * this to {@link Invocable}.
     */
    private static ScriptEngine scriptEngine;

    /**
     * Caches delegate method invocations (arguments + return values).
     */
    private static final InvocationCache invocationCache =
            new HeapInvocationCache();

    /**
     * Read/write lock used to maintain thread-safety.
     */
    private static final StampedLock lock = new StampedLock();

    /**
     * The Ruby delegate object.
     */
    private Object delegate;

    private RequestContext requestContext;

    static {
        // N.B.: These must be set before the ScriptEngine is instantiated.

        System.setProperty("org.jruby.embed.compat.version", "JRuby2.3");

        // Available values are singleton, singlethread, threadsafe and
        // concurrent (JSR-223 default). See
        // https://github.com/jruby/jruby/wiki/RedBridge#Context_Instance_Type
        System.setProperty("org.jruby.embed.localcontext.scope", "concurrent");

        // Available values are transient, persistent, global (JSR-223 default)
        // and bsf. See
        // https://github.com/jruby/jruby/wiki/RedBridge#Local_Variable_Behavior_Options
        System.setProperty("org.jruby.embed.localvariable.behavior", "transient");

        scriptEngine = new ScriptEngineManager().getEngineByName("jruby");
    }

    public static InvocationCache getInvocationCache() {
        return invocationCache;
    }

    private static boolean isInvocationCacheEnabled() {
        return Configuration.getInstance().
                getBoolean(Key.DELEGATE_METHOD_INVOCATION_CACHE_ENABLED, false);
    }

    /**
     * Loads the given code into the script engine.
     */
    static void load(String code) throws ScriptException {
        LOGGER.info("Loading script code");

        final long stamp = lock.writeLock();
        try {
            scriptEngine.eval(code);
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * @param requestContext Request context.
     * @param methodName     Name of the method being invoked.
     * @param args           Method arguments.
     * @return               Cache key corresponding to the given arguments.
     */
    private static Object getCacheKey(RequestContext requestContext,
                                      String methodName,
                                      Object... args) {
        // The cache key is comprised of the method name at position 0, the
        // request context at position 1, and the arguments at succeeding
        // positions.
        List<Object> key = Arrays.asList(args);
        key = new LinkedList<>(key);
        key.add(0, methodName);
        key.add(1, requestContext);
        return key;
    }

    DelegateProxy(RequestContext context) {
        instantiateDelegate(context);
    }

    private void instantiateDelegate(RequestContext context) {
        final long stamp = lock.readLock();
        final Stopwatch watch = new Stopwatch();
        try {
            delegate = scriptEngine.eval("\n" +
                    RUBY_DELEGATE_CLASS_NAME + ".new" + "\n");
            setRequestContext(context);

            LOGGER.debug("Instantiated delegate object in {}", watch);
        } catch (javax.script.ScriptException e) {
            LOGGER.error(e.getMessage());
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * @param context Context to set.
     * @throws ScriptException if the delegate script does not contain a
     *                         {@link #RUBY_REQUEST_CONTEXT_SETTER setter
     *                         method for the context}.
     */
    public void setRequestContext(RequestContext context)
            throws ScriptException {
        invokeUncached(RUBY_REQUEST_CONTEXT_SETTER,
                Collections.unmodifiableMap(context.toMap()));
        requestContext = context;
    }

    /**
     * @return Return value of {@link DelegateMethod#AUTHORIZE}.
     */
    public Object authorize() throws ScriptException {
        return invoke(DelegateMethod.AUTHORIZE);
    }

    /**
     * @return Return value of {@link
     *         DelegateMethod#EXTRA_IIIF2_INFORMATION_RESPONSE_KEYS}, or an
     *         empty map if it returned {@literal nil}.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getExtraIIIFInformationResponseKeys()
            throws ScriptException {
        Object result = invoke(DelegateMethod.EXTRA_IIIF2_INFORMATION_RESPONSE_KEYS);
        if (result != null) {
            return (Map<String, Object>) result;
        }
        return Collections.emptyMap();
    }

    /**
     * {@return Return value of {@link
     *          DelegateMethod#AZURESTORAGESOURCE_BLOB_KEY }. May be
     *          {@literal null}.
     */
    public String getAzureStorageSourceBlobKey() throws ScriptException {
        Object result = invoke(DelegateMethod.AZURESTORAGESOURCE_BLOB_KEY);
        return (String) result;
    }

    /**
     * @return Return value of {@link
     *         DelegateMethod#FILESYSTEMSOURCE_PATHMAME}. May be {@literal
     *         null}.
     */
    public String getFilesystemSourcePathname() throws ScriptException {
        Object result = invoke(DelegateMethod.FILESYSTEMSOURCE_PATHMAME);
        return (String) result;
    }

    /**
     * @return Map based on the return value of {@link
     *         DelegateMethod#HTTPSOURCE_RESOURCE_INFO}, or an empty map if
     *         it returned {@literal nil}.
     */
    @SuppressWarnings("unchecked")
    public Map<String,?> getHttpSourceResourceInfo()
            throws ScriptException {
        Object result = invoke(DelegateMethod.HTTPSOURCE_RESOURCE_INFO);
        if (result instanceof String) {
            Map<String,String> map = new HashMap<>();
            map.put("uri", (String) result);
            return map;
        } else if (result instanceof Map) {
            return (Map<String,Object>) result;
        }
        return Collections.emptyMap();
    }

    /**
     * @return Return value of {@link
     *         DelegateMethod#JDBCSOURCE_DATABASE_IDENTIFIER}. May be
     *         {@literal null}.
     */
    public String getJdbcSourceDatabaseIdentifier() throws ScriptException {
        Object result = invoke(DelegateMethod.JDBCSOURCE_DATABASE_IDENTIFIER);
        return (String) result;
    }

    /**
     * @return Return value of {@link DelegateMethod#JDBCSOURCE_MEDIA_TYPE}.
     */
    public String getJdbcSourceMediaType() throws ScriptException {
        Object result = invoke(DelegateMethod.JDBCSOURCE_MEDIA_TYPE);
        return (String) result;
    }

    /**
     * @return Return value of {@link DelegateMethod#JDBCSOURCE_LOOKUP_SQL}.
     */
    public String getJdbcSourceLookupSQL() throws ScriptException {
        Object result = invoke(DelegateMethod.JDBCSOURCE_LOOKUP_SQL);
        return (String) result;
    }

    /**
     * @return Return value of {@link DelegateMethod#OVERLAY}, or an empty map
     *         if it returned {@literal nil}.
     */
    @SuppressWarnings("unchecked")
    public Map<String,Object> getOverlayProperties() throws ScriptException {
        Object result = invoke(DelegateMethod.OVERLAY);
        if (result != null) {
            return (Map<String, Object>) result;
        }
        return Collections.emptyMap();
    }

    /**
     * @return Return value of {@link DelegateMethod#REDACTIONS}, or an empty
     *         list if it returned {@literal nil}.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String,Long>> getRedactions() throws ScriptException {
        Object result = invoke(DelegateMethod.REDACTIONS);
        if (result != null) {
            return Collections.unmodifiableList((List<Map<String, Long>>) result);
        }
        return Collections.emptyList();
    }

    /**
     * @return Return value of {@link DelegateMethod#SOURCE}. May be
     *         {@literal null}.
     */
    public String getSource() throws ScriptException {
        Object result = invoke(DelegateMethod.SOURCE);
        return (String) result;
    }

    /**
     * @return Return value of {@link DelegateMethod#S3SOURCE_OBJECT_INFO},
     *         or an empty map if it returned {@literal nil}.
     */
    @SuppressWarnings("unchecked")
    public Map<String,String> getS3SourceObjectInfo() throws ScriptException {
        Object result = invoke(DelegateMethod.S3SOURCE_OBJECT_INFO);
        if (result != null) {
            return (Map<String, String>) result;
        }
        return Collections.emptyMap();
    }

    /**
     * N.B.: The returned object should not be modified, as this could disrupt
     * the invocation cache.
     *
     * @param method Method to invoke.
     * @param args   Arguments to pass to the method.
     * @return       Return value of the method.
     */
    private Object invoke(DelegateMethod method,
                          Object... args) throws ScriptException {
        return invoke(method.getMethodName(), args);
    }

    /**
     * N.B.: The returned object should not be modified, as this could disrupt
     * the invocation cache.
     *
     * @param method Method to invoke.
     * @param args   Arguments to pass to the method.
     * @return       Return value of the method.
     */
    private Object invoke(String method,
                          Object... args) throws ScriptException {
        return isInvocationCacheEnabled() ?
                retrieveFromCacheOrInvoke(method, args) :
                invokeUncached(method, args);
    }

    private Object retrieveFromCacheOrInvoke(String method, Object... args)
            throws ScriptException {
        final Object cacheKey = getCacheKey(requestContext, method, args);
        Object returnValue = invocationCache.get(cacheKey);

        if (returnValue != null) {
            LOGGER.debug("invoke({}): cache hit (skipping invocation)", method);
        } else {
            LOGGER.debug("invoke({}): cache miss", method);
            returnValue = invokeUncached(method, args);
            if (returnValue != null) {
                invocationCache.put(cacheKey, returnValue);
            }
        }
        return returnValue;
    }

    private Object invokeUncached(String methodName,
                                  Object... args) throws ScriptException {
        final long stamp = lock.readLock();

        final String argsList = (args.length > 0) ?
                Arrays.stream(args)
                        .map(Object::toString)
                        .collect(Collectors.joining(", ")) : "none";
        LOGGER.debug("invokeUncached(): invoking {} with args: ({})",
                methodName, argsList);

        final Stopwatch watch = new Stopwatch();
        try {
            final Object retval = ((Invocable) scriptEngine).invokeMethod(
                    delegate, methodName, args);

            if (!RUBY_REQUEST_CONTEXT_SETTER.equals(methodName)) {
                LOGGER.debug("invokeUncached(): {} returned {} for args: ({}) in {}",
                        methodName, retval, argsList, watch);
            }
            return retval;
        } catch (NoSuchMethodException e) {
            throw new ScriptException(e);
        } finally {
            lock.unlock(stamp);
        }
    }

}
