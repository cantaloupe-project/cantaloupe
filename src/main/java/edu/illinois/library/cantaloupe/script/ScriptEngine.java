package edu.illinois.library.cantaloupe.script;

import javax.script.ScriptException;

/**
 * Interface for classes that generally wrap a
 * {@link javax.script.ScriptEngine}, providing a higher-level interface
 * with separate concepts of loading code and invoking methods.
 */
public interface ScriptEngine {

    /**
     * Invokes a method with no arguments.
     *
     * @param methodName Name of the method to invoke without any arguments
     *                   or syntax.
     * @return Function result
     * @throws ScriptException
     */
    Object invoke(String methodName) throws ScriptException;

    /**
     * Invokes a method.
     *
     * @param methodName Name of the method to invoke without any arguments
     *                   or syntax.
     * @param args Objects to pass to the method as arguments. Will be
     *             automatically serialized to a given language for execution.
     * @return Method return value
     * @throws ScriptException
     */
    Object invoke(String methodName, Object[] args) throws ScriptException;

    /**
     * @param code Code to load into the script interpreter.
     * @throws ScriptException
     */
    void load(String code) throws ScriptException;

    /**
     * @param methodName Name of the method.
     * @return Whether the method has been defined.
     * @throws ScriptException
     */
    boolean methodExists(String methodName) throws ScriptException;

}
