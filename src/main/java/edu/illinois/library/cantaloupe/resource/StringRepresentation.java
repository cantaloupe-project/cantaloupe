package edu.illinois.library.cantaloupe.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class StringRepresentation implements Representation {

    private String string;

    public StringRepresentation(String string) {
        this.string = string;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream, "UTF-8")) {
            writer.write(string);
        }
    }

}
