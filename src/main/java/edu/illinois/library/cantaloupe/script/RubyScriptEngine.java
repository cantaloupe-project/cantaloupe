package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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

    private final Object lock = new Object();
    private javax.script.ScriptEngine scriptEngine;
    private final AtomicBoolean scriptIsLoading = new AtomicBoolean(false);

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
        try {
            final Object returnValue = ((Invocable) scriptEngine).
                    invokeMethod(
                            scriptEngine.eval(getModuleName(methodName)),
                            getUnqualifiedMethodName(methodName), args);
            logger.debug("invoke({}::{}): exec time: {} msec",
                    TOP_MODULE, methodName, watch.timeElapsed());
            return returnValue;
        } catch (NoSuchMethodException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public synchronized void load(String code) throws ScriptException {
        scriptIsLoading.set(true);
        logger.info("load(): loading script code");
        scriptEngine = new ScriptEngineManager().getEngineByName("jruby");
        scriptEngine.eval(code);
        scriptIsLoading.set(false);
    }

}
