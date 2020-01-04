package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * @see <a href="https://www.graalvm.org/docs/reference-manual/embed/">Embed
 *     Languages With the GraalVM Polyglot API</a>
 * @see <a href="https://github.com/oracle/truffleruby/blob/master/doc/user/jruby-migration.md">
 *     Migration from JRuby to TruffleRuby</a>
 */
final class TruffleRubyDelegateProxy implements DelegateProxy {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TruffleRubyDelegateProxy.class);

    /**
     * Name of the delegate class.
     */
    private static final String DELEGATE_CLASS_NAME = "CustomDelegate";

    /**
     * Name of the request context accessor method.
     */
    private static final String REQUEST_CONTEXT_METHOD = "context";

    /**
     * N.B.: the <a href="https://www.graalvm.org/docs/reference-manual/embed/">
     * docs</a> say that closing this is "optional but recommended."
     */
    private static final Context CONTEXT =
            Context.newBuilder().allowAllAccess(true).build();

    /**
     * Read/write lock used to maintain thread-safe code reloading.
     */
    private static final StampedLock LOCK = new StampedLock();

    /**
     * The delegate object, instantiated by {@link
     * #instantiateDelegate(RequestContext)}.
     */
    private Value delegate;

    private RequestContext requestContext;

    /**
     * Converts a Polyglot type to the most appropriate Java type. The
     * conversion is recursive in the case of arrays and maps.
     *
     * @param value Polyglot type.
     * @return Java equivalent, which may be {@code null}.
     */
    private static Object convertType(Value value) {
        // N.B.: the API is awkward in that there are not always "is*" methods
        // to match the "as*" methods. We do our best.
        if (value == null || value.isNull()) {
            return null;
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isNumber()) {
            try {
                return value.asLong();
            } catch (ClassCastException e) {
                return value.asDouble();
            }
        } else if (value.isString()) {
            return value.asString();
        }
        // Check for an array. Again, awkward.
        final Set<String> keys = value.getMemberKeys();
        if (keys.size() > 150) {
            final List<Object> list = new ArrayList<>();
            for (long i = 0, count = value.getArraySize(); i < count; i++) {
                Value element = value.getArrayElement(i);
                list.add(convertType(element));
            }
            return list;
        }
        // Fall back to assuming a hash.
        final Map<String, Object> map = new HashMap<>();
        value.getMemberKeys().forEach(key ->
                map.put(key, convertType(value.getMember(key))));
        return map;
    }

    /**
     * Loads the given code into the script engine.
     */
    static void load(String code) {
        LOGGER.info("Loading script code");
        final long stamp = LOCK.writeLock();
        try {
            CONTEXT.eval("ruby", code);
        } finally {
            LOCK.unlock(stamp);
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
        List<Object> key = List.of(args);
        key = new LinkedList<>(key);
        key.add(0, methodName);
        key.add(1, requestContext);
        return key;
    }

    TruffleRubyDelegateProxy(RequestContext context) {
        instantiateDelegate(context);
    }

    private void instantiateDelegate(RequestContext requestContext) {
        final long stamp = LOCK.readLock();
        final Stopwatch watch = new Stopwatch();
        try {
            delegate = CONTEXT.eval("ruby", DELEGATE_CLASS_NAME + ".new");
            setRequestContext(requestContext);
            LOGGER.trace("Instantiated delegate object in {}", watch);
        } catch (javax.script.ScriptException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            LOCK.unlock(stamp);
        }
    }

    /**
     * @param requestContext Context to set.
     * @throws ScriptException if the delegate script does not contain a {@link
     *         #REQUEST_CONTEXT_METHOD setter method for the context}.
     */
    @Override
    public void setRequestContext(RequestContext requestContext)
            throws ScriptException {
        // Set the delegate object context to an empty hash.
        Value hash = CONTEXT.eval("ruby", "{}");
        Value setterMethod = delegate.getMember(REQUEST_CONTEXT_METHOD + "=");
        if (setterMethod != null) {
            setterMethod.executeVoid(hash);

            // Populate the hash with the RequestContext properties.
            Value accessor = CONTEXT.eval("ruby",
                    "->(hash, key, value) { hash[key] = value }");
            requestContext.toMap().forEach((key, value) ->
                    accessor.execute(hash, key, value));

            this.requestContext = requestContext;
        } else {
            throw new ScriptException("Delegate class does not implement a " +
                    REQUEST_CONTEXT_METHOD + "=() method. Make sure it " +
                    "contains a line like: `attr_accessor :" +
                    REQUEST_CONTEXT_METHOD + "`");
        }
    }

    /**
     * @return Return value of {@link DelegateMethod#AUTHORIZE}.
     */
    @Override
    public Object authorize() throws ScriptException {
        Value result = invoke(DelegateMethod.AUTHORIZE);
        return convertType(result);
    }

    /**
     * @return Return value of {@link
     *         DelegateMethod#EXTRA_IIIF_INFORMATION_RESPONSE_KEYS}, or an
     *         empty map if it returned {@code nil}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getExtraIIIFInformationResponseKeys()
            throws ScriptException {
        Value result = invoke(DelegateMethod.EXTRA_IIIF_INFORMATION_RESPONSE_KEYS);
        Object javaResult = convertType(result);
        if (javaResult != null) {
            return (Map<String, Object>) javaResult;
        }
        return Collections.emptyMap();
    }

    /**
     * {@return Return value of {@link
     *          DelegateMethod#AZURESTORAGESOURCE_BLOB_KEY }. May be {@code
     *          null}.
     */
    @Override
    public String getAzureStorageSourceBlobKey() throws ScriptException {
        Value result = invoke(DelegateMethod.AZURESTORAGESOURCE_BLOB_KEY);
        return result.asString();
    }

    /**
     * @return Return value of {@link
     *         DelegateMethod#FILESYSTEMSOURCE_PATHMAME}. May be {@code null}.
     */
    @Override
    public String getFilesystemSourcePathname() throws ScriptException {
        Value result = invoke(DelegateMethod.FILESYSTEMSOURCE_PATHMAME);
        return result.asString();
    }

    /**
     * @return Map based on the return value of {@link
     *         DelegateMethod#HTTPSOURCE_RESOURCE_INFO}, or an empty map if
     *         it returned {@code nil}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String,?> getHttpSourceResourceInfo() throws ScriptException {
        Value result = invoke(DelegateMethod.HTTPSOURCE_RESOURCE_INFO);
        Object javaResult = convertType(result);
        if (javaResult instanceof String) {
            Map<String,String> map = new HashMap<>();
            map.put("uri", (String) javaResult);
            return map;
        } else if (javaResult instanceof Map) {
            return (Map<String,Object>) javaResult;
        }
        return Collections.emptyMap();
    }

    /**
     * @return Return value of {@link
     *         DelegateMethod#JDBCSOURCE_DATABASE_IDENTIFIER}. May be {@code
     *         null}.
     */
    @Override
    public String getJdbcSourceDatabaseIdentifier() throws ScriptException {
        Value result = invoke(DelegateMethod.JDBCSOURCE_DATABASE_IDENTIFIER);
        return result.asString();
    }

    /**
     * @return Return value of {@link DelegateMethod#JDBCSOURCE_MEDIA_TYPE}.
     */
    @Override
    public String getJdbcSourceMediaType() throws ScriptException {
        Value result = invoke(DelegateMethod.JDBCSOURCE_MEDIA_TYPE);
        return result.asString();
    }

    /**
     * @return Return value of {@link DelegateMethod#JDBCSOURCE_LOOKUP_SQL}.
     */
    @Override
    public String getJdbcSourceLookupSQL() throws ScriptException {
        Value result = invoke(DelegateMethod.JDBCSOURCE_LOOKUP_SQL);
        return result.asString();
    }

    /**
     * @return Return value of {@link DelegateMethod#METADATA}.
     */
    @Override
    public String getMetadata() throws ScriptException {
        Value result = invoke(DelegateMethod.METADATA);
        return result.asString();
    }

    /**
     * @return Return value of {@link DelegateMethod#OVERLAY}, or an empty map
     *         if it returned {@code nil}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String,Object> getOverlayProperties() throws ScriptException {
        Value result = invoke(DelegateMethod.OVERLAY);
        Object javaResult = convertType(result);
        if (javaResult != null) {
            return (Map<String, Object>) javaResult;
        }
        return Collections.emptyMap();
    }

    /**
     * @return Return value of {@link DelegateMethod#REDACTIONS}, or an empty
     *         list if it returned {@code nil}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String,Long>> getRedactions() throws ScriptException {
        Value result = invoke(DelegateMethod.REDACTIONS);
        Object javaResult = convertType(result);
        if (javaResult != null) {
            // TODO: conversion
            return Collections.unmodifiableList((List<Map<String, Long>>) javaResult);
        }
        return Collections.emptyList();
    }

    /**
     * @return Return value of {@link DelegateMethod#SOURCE}. May be
     *         {@code null}.
     */
    @Override
    public String getSource() throws ScriptException {
        Value result = invoke(DelegateMethod.SOURCE);
        return result.asString();
    }

    /**
     * @return Return value of {@link DelegateMethod#S3SOURCE_OBJECT_INFO},
     *         or an empty map if it returned {@code nil}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String,String> getS3SourceObjectInfo() throws ScriptException {
        Value result = invoke(DelegateMethod.S3SOURCE_OBJECT_INFO);
        Object javaResult = convertType(result);
        if (javaResult != null) {
            return (Map<String, String>) javaResult;
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
    private Value invoke(DelegateMethod method,
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
    private Value invoke(String method,
                         Object... args) throws ScriptException {
        return DelegateProxyService.isInvocationCacheEnabled() ?
                retrieveFromCacheOrInvoke(method, args) :
                invokeUncached(method, args);
    }

    private Value retrieveFromCacheOrInvoke(String method,
                                            Object... args) throws ScriptException {
        final Object cacheKey = getCacheKey(requestContext, method, args);
        Value returnValue = (Value) DelegateProxyService.getInvocationCache().get(cacheKey);
        if (returnValue != null && !returnValue.isNull()) {
            LOGGER.trace("invoke({}): cache hit (skipping invocation)", method);
        } else {
            LOGGER.trace("invoke({}): cache miss", method);
            returnValue = invokeUncached(method, args);
            if (returnValue != null && !returnValue.isNull()) {
                DelegateProxyService.getInvocationCache().put(cacheKey, returnValue);
            }
        }
        return returnValue;
    }

    private Value invokeUncached(String methodName,
                                 Object... args) throws ScriptException {
        final long stamp = LOCK.readLock();
        try {
            logInvocation(methodName, args);
            final Stopwatch watch = new Stopwatch();
            final Value retval = delegate.invokeMember(methodName, args);
            LOGGER.trace("invokeUncached(): {}() returned {} for args: ({}) in {}",
                    methodName, retval, args, watch);
            return retval;
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        } finally {
            LOCK.unlock(stamp);
        }
    }

    private void logInvocation(String methodName, Object... args) {
        String argsList = (args.length > 0) ?
                Arrays.stream(args)
                        .map(Object::toString)
                        .collect(Collectors.joining(", ")) : "none";
        LOGGER.trace("Invoking {}() with args: ({})", methodName, argsList);
    }

}
