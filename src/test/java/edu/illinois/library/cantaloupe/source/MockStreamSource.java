package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;

public class MockStreamSource extends AbstractSource
        implements StreamSource {

    @Override
    public void checkAccess() {
    }

    @Override
    public Format getSourceFormat() {
        return null;
    }

    @Override
    public StreamFactory newStreamFactory() {
        return null;
    }

}
