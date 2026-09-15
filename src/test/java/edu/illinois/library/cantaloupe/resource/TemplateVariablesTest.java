package edu.illinois.library.cantaloupe.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TemplateVariablesTest {
    
    @Test
    void testGetCommonTemplateVars() {
        TemplateVariables vars = TemplateVariables.getDefault(null);
        assertEquals(((String) vars.get("basePath")), "/");
        assertNotNull(vars.get("version"));
    }
}
