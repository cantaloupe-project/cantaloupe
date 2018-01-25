package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;

public class MockStreamResolver extends AbstractResolver
        implements StreamResolver {

    @Override
    public void checkAccess() {
    }

    @Override
    public Format getSourceFormat() {
        return null;
    }

    @Override
    public StreamSource newStreamSource() {
        return null;
    }

}
