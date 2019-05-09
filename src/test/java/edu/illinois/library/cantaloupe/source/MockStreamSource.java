package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;
import org.apache.commons.io.input.NullInputStream;

import java.util.Collections;
import java.util.Iterator;

public class MockStreamSource extends AbstractSource implements StreamSource {

    @Override
    public void checkAccess() {
    }

    @Override
    public Iterator<Format> getFormatIterator() {
        return Collections.emptyIterator();
    }

    @Override
    public StreamFactory newStreamFactory() {
        return () -> new NullInputStream(8);
    }

}
