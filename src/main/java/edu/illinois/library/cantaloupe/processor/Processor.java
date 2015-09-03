package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Parameters;

import java.io.OutputStream;

public interface Processor {

    public void process(Parameters p, OutputStream os) throws Exception;

}
