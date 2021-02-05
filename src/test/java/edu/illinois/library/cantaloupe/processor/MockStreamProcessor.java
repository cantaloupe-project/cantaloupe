package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.source.StreamFactory;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Currently there are no StreamProcessors that have no dependencies. This is
 * used in tests to mock one.
 */
public class MockStreamProcessor implements StreamProcessor {

    private boolean isSeeking;
    private Format sourceFormat;
    private StreamFactory streamFactory;

    @Override
    public void close() {
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        return new HashSet<>();
    }

    @Override
    public Format getSourceFormat() {
        return sourceFormat;
    }

    @Override
    public StreamFactory getStreamFactory() {
        return streamFactory;
    }

    @Override
    public boolean isSeeking() {
        return isSeeking;
    }

    @Override
    public void process(OperationList opList,
                        Info sourceInfo,
                        OutputStream outputStream) {
        // no-op
    }

    @Override
    public Info readInfo() {
        return new Info();
    }

    public void setSeeking(boolean isSeeking) {
        this.isSeeking = isSeeking;
    }

    @Override
    public void setSourceFormat(Format format)
            throws SourceFormatException {
        this.sourceFormat = format;
    }

    @Override
    public void setStreamFactory(StreamFactory source) {
        this.streamFactory = source;
    }

    @Override
    public boolean supportsSourceFormat(Format format) {
        return true;
    }

}
