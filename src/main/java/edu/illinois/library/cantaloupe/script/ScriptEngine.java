package edu.illinois.library.cantaloupe.script;

import javax.script.ScriptException;

/**
 * Interface for classes that generally wrap a
 * {@link javax.script.ScriptEngine}, providing a higher-level interface
 * with separate concepts of loading code and invoking methods.
 */
public interface ScriptEngine {

    /**
     * Invokes a method.
     *
     * @param methodName Name of the method to invoke.
     * @param args Objects to pass to the method as arguments.
     * @return Method return value.
     * @throws ScriptException
     */
    Object invoke(String methodName, Object... args) throws ScriptException;

    /**
     * @param code Code to load into the script interpreter.
     * @throws ScriptException
     */
    void load(String code) throws ScriptException;

}
