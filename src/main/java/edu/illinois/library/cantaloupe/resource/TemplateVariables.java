package edu.illinois.library.cantaloupe.resource;

import java.util.HashMap;
import java.util.Map;

import edu.illinois.library.cantaloupe.Application;

public class TemplateVariables {
    public static TemplateVariables getDefault(String basePath) {
        TemplateVariables set = new TemplateVariables();
        set.put("version", Application.getVersion());
        set.put("basePath", basePath != null ? basePath : "/");
        return set;
    }

    final Map<String,Object> vars = new HashMap<>();

    public void put(String key, Object value) {
        vars.put(key, value);
    }

    public Object get(String key) {
        return vars.get(key);
    }

    public Map<String,Object> getVars() {
        return vars;
    }
    
}
