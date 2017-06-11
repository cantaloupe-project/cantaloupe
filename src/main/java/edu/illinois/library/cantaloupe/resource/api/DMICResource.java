package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.script.InvocationCache;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;

/**
 * Delegate method invocation cache resource.
 */
public class DMICResource extends APIResource {

    /**
     * @throws Exception
     */
    @Delete
    public Representation doPurge() throws Exception {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        if (engine != null) {
            final InvocationCache cache = engine.getInvocationCache();
            if (cache != null) {
                cache.purge();
            }
        }
        return new EmptyRepresentation();
    }

}
