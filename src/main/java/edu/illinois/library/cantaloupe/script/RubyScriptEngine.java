package edu.illinois.library.cantaloupe.script;

import org.apache.commons.lang3.StringUtils;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a {@link javax.script.ScriptEngine}, providing a higher-level
 * interface with separate concepts of loading code and invoking functions.
 */
class RubyScriptEngine implements ScriptEngine {

    private javax.script.ScriptEngine scriptEngine = new ScriptEngineManager().
            getEngineByName("jruby");

    /**
     * @param functionName
     * @param args
     * @return
     * @throws ScriptException
     */
    @Override
    public Object invoke(String functionName, String[] args)
            throws ScriptException {
        final String invocationString = String.format("%s(%s)",
                functionName, formattedArgumentList(args));
        return scriptEngine.eval(invocationString);
    }

    @Override
    public void load(String code) throws ScriptException {
        scriptEngine.eval(code);
    }

    private String escapeArgument(String arg) {
        return "'" + StringUtils.replace(arg, "'", "\\'") + "'";
    }

    private String formattedArgumentList(String[] args) {
        final List<String> escapedArgs = new ArrayList<>();
        for (String arg : args) {
            escapedArgs.add(escapeArgument(arg));
        }
        return StringUtils.join(escapedArgs, ", ");
    }

}
