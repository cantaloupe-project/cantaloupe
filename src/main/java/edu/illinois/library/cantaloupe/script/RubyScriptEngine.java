package edu.illinois.library.cantaloupe.script;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collection;
import java.util.Map;

class RubyScriptEngine implements ScriptEngine {

    private static Logger logger = LoggerFactory.
            getLogger(RubyScriptEngine.class);

    /** Ruby module containing methods to invoke */
    private static final String MODULE = "Cantaloupe";

    private javax.script.ScriptEngine scriptEngine = new ScriptEngineManager().
            getEngineByName("jruby");

    /**
     * @param methodName Method to invoke
     * @return Return value of the invocation
     * @throws ScriptException
     */
    @Override
    public Object invoke(String methodName) throws ScriptException {
        final long msec = System.currentTimeMillis();
        final String invocationString = String.format("%s::%s",
                MODULE, methodName);
        final Object returnValue = scriptEngine.eval(invocationString);
        logger.debug("invoke({}::{}): exec time: {} msec",
                MODULE, methodName, System.currentTimeMillis() - msec);
        return returnValue;
    }

    /**
     * @param methodName Method to invoke
     * @param args See {@link #serializeAsRuby(Object)} for allowed types
     * @return Return value of the invocation
     * @throws ScriptException
     */
    @Override
    public Object invoke(String methodName, Object[] args)
            throws ScriptException {
        final long msec = System.currentTimeMillis();
        final String invocationString = String.format("%s::%s(%s)",
                MODULE, methodName, serializeAsRuby(args));
        final Object returnValue = scriptEngine.eval(invocationString);
        logger.debug("invoke({}::{}(*args)): exec time: {} msec",
                MODULE, methodName, System.currentTimeMillis() - msec);
        return returnValue;
    }

    @Override
    public void load(String code) throws ScriptException {
        scriptEngine.eval(code);
    }

    @Override
    public boolean methodExists(String methodName) throws ScriptException {
        return (boolean) scriptEngine.eval(
                String.format("%s.respond_to?(%s)",
                MODULE, serializeStringAsRuby(methodName)));
    }

    private String serializeAsRuby(Object[] args) {
        final StringBuilder rubyCode = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            rubyCode.append(serializeAsRuby(args[i]));
            if (i < args.length - 1) {
                rubyCode.append(", ");
            }
        }
        return rubyCode.toString();
    }

    /**
     * <p>Serializes the given arguments to Ruby source code:</p>
     *
     * <ul>
     *     <li>{@link Collection} as Array</li>
     *     <li>{@link Map} as Hash with String keys
     *     (from {@link Object#toString()})</li>
     *     <li>{@link Boolean} as Boolean</li>
     *     <li>{@link Number} as Numeric</li>
     *     <li><code>null</code> as <code>nil</code></li>
     *     <li>All other types as String</li>
     * </ul>
     *
     * @param object Java object
     * @return Ruby source code
     */
    public String serializeAsRuby(Object object) {
        StringBuilder rubyCode = new StringBuilder();
        if (object == null) {
            rubyCode.append("nil");
        } else if (object instanceof Number || object instanceof Boolean) {
            rubyCode.append(object.toString());
        } else if (object instanceof Collection) {
            rubyCode.append("[");
            Collection collection = (Collection) object;
            for (int i = 0, length = collection.size(); i < length; i++) {
                rubyCode.append(serializeAsRuby(collection.toArray()[i]));
                if (i < length - 1) {
                    rubyCode.append(", ");
                }
            }
            rubyCode.append("]");
        } else if (object instanceof Map) {
            rubyCode.append("{");
            Map map = (Map) object;
            int i = 0;
            for (Object key : map.keySet()) {
                rubyCode.append(serializeStringAsRuby(key.toString()));
                rubyCode.append(" => ");
                rubyCode.append(serializeAsRuby(map.get(key)));
                if (i < map.size() - 1) {
                    rubyCode.append(", ");
                }
                i++;
            }
            rubyCode.append("}");
        } else {
            rubyCode.append(serializeStringAsRuby(object.toString()));
        }
        return rubyCode.toString();
    }

    private String serializeStringAsRuby(String arg) {
        return "'" + StringUtils.replace(arg, "'", "\\'") + "'";
    }

}
