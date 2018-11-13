package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.resource.iiif.v1.Quality;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Currently there are no StreamProcessors that have no dependencies. This is
 * used in tests to mock one.
 */
public class MockStreamProcessor implements StreamProcessor {

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
    public Set<ProcessorFeature> getSupportedFeatures() {
        return new HashSet<>();
    }

    @Override
    public Set<Quality> getSupportedIIIF1Qualities() {
        return new HashSet<>();
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIIIF2Qualities() {
        return new HashSet<>();
    }

    @Override
    public void process(OperationList opList, Info sourceInfo,
                        OutputStream outputStream) {
        // no-op
    }

    @Override
    public Info readInfo(){
        return new Info();
    }

    @Override
    public void setSourceFormat(Format format) {
        this.sourceFormat = format;
    }

    @Override
    public void setStreamFactory(StreamFactory source) {
        this.streamFactory = source;
    }

}
