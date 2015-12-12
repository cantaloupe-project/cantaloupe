package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.apache.commons.lang3.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

abstract class ResolverUtil {

    /**
     * @param code Ruby code containing the function to execute
     * @param functionName Name of the function to execute
     * @param args String arguments to pass to the function
     * @return Return value of the function
     * @throws ScriptException If the script failed to execute
     */
    public static String executeRubyFunction(final String code,
                                             final String functionName,
                                             final String[] args)
            throws ScriptException {
        final List<String> escapedArgs = new ArrayList<>();
        for (String arg : args) {
            escapedArgs.add("'" + StringUtils.replace(arg, "'", "\\'") + "'");
        }
        final String argsList = StringUtils.join(escapedArgs, ", ");
        return executeRubyScript(String.format("%s\n%s(%s)", code,
                functionName, argsList));
    }

    /**
     * Passes the given identifier to a function in the given script.
     *
     * @param code
     * @return Script result
     * @throws ScriptException If the script failed to execute
     */
    public static String executeRubyScript(String code) throws ScriptException {
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("jruby");
        try {
            return (String) engine.eval(code);
        } catch (Error e) {
            throw new ScriptException(e.getMessage());
        }
    }

    /**
     * Guesses the source format of a file based on the filename extension in
     * the given identifier.
     *
     * @param identifier
     * @return Inferred source format, or {@link SourceFormat#UNKNOWN} if
     * unknown.
     */
    public static SourceFormat inferSourceFormat(Identifier identifier) {
        String idStr = identifier.toString().toLowerCase();
        String extension = null;
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        int i = idStr.lastIndexOf('.');
        if (i > 0) {
            extension = idStr.substring(i + 1);
        }
        if (extension != null) {
            for (SourceFormat enumValue : SourceFormat.values()) {
                if (enumValue.getExtensions().contains(extension)) {
                    sourceFormat = enumValue;
                    break;
                }
            }
        }
        return sourceFormat;
    }

    /**
     * Some web servers have issues dealing with encoded slashes (%2F) in URL
     * identifiers. This method enables the use of an alternate string as a
     * path separator.
     *
     * @param identifier
     * @param currentSeparator
     * @param newSeparator
     * @return
     */
    public static Identifier replacePathSeparators(Identifier identifier,
                                                   String currentSeparator,
                                                   String newSeparator) {
        final String idStr = StringUtils.replace(identifier.toString(),
                currentSeparator, newSeparator);
        return new Identifier(idStr);
    }

}
