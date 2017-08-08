package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;

abstract class AbstractResolver {

    protected Identifier identifier;
    protected Format sourceFormat;
    protected RequestContext context;

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
        this.sourceFormat = null;
    }

    public void setContext(RequestContext context) {
        this.context = context;
    }
}
