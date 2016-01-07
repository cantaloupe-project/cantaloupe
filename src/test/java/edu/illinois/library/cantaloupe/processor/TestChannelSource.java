package edu.illinois.library.cantaloupe.processor;


import edu.illinois.library.cantaloupe.resolver.ChannelSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

class TestChannelSource implements ChannelSource {

    private File file;

    public TestChannelSource(File file) {
        this.file = file;
    }

    @Override
    public ReadableByteChannel newChannel() throws IOException {
        return new FileInputStream(file).getChannel();
    }
}
