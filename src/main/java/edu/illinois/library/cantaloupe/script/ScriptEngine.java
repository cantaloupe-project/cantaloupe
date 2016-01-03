package edu.illinois.library.cantaloupe.script;

import javax.script.ScriptException;

/**
 * Interface for classes that generally wrap a
 * {@link javax.script.ScriptEngine}, providing a higher-level concepts of
 * loading code and invoking functions defined in it.
 */
public interface ScriptEngine {

    /**
     * Invokes a function.
     *
     * @param functionName
     * @param args
     * @return Function result
     * @throws ScriptException
     */
    Object invoke(String functionName, String[] args) throws ScriptException;

    /**
     * @param code Code to load into the script interpreter.
     * @throws ScriptException
     */
    void load(String code) throws ScriptException;

    /**
     * @param methodName
     * @return
     * @throws ScriptException
     */
    boolean methodExists(String methodName) throws ScriptException;

}
