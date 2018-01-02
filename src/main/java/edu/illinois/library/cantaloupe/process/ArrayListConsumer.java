package edu.illinois.library.cantaloupe.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

abstract class ArrayListConsumer {

    /**
     * Lines of output from a command.
     */
    private ArrayList<String> outputLines = new ArrayList<>();

    /**
     * Clears the output.
     */
    public void clear() {
        outputLines.clear();
    }

    /**
     * Reads lines from the given {@link InputStream} and stores them
     * internally.
     */
    void consume(InputStream processInputStream) throws IOException {
        InputStreamReader isr = new InputStreamReader(processInputStream, "UTF-8");
        try (BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }
    }

    /**
     * @return Lines of output from a command.
     */
    public ArrayList<String> getOutput() {
        return outputLines;
    }

}
