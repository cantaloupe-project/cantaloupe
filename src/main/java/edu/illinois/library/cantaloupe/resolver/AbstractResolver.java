package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;

abstract class AbstractResolver {

    protected Identifier identifier;
    protected Format sourceFormat;

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
        this.sourceFormat = null;
    }

}
