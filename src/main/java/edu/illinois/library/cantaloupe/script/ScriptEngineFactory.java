package edu.illinois.library.cantaloupe.script;

public abstract class ScriptEngineFactory {

    public static ScriptEngine getScriptEngine(String name) {
        switch (name) {
            case "jruby":
                return new RubyScriptEngine();
        }
        return null;
    }

}
