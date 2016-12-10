package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.operation.Format;

import java.util.Set;

abstract class AbstractProcessor {

    protected Format format;

    abstract public Set<Format> getAvailableOutputFormats();

    public Format getSourceFormat() {
        return this.format;
    }

    public void setSourceFormat(Format format)
            throws UnsupportedSourceFormatException{
        this.format = format;
        if (getAvailableOutputFormats().size() < 1) {
            throw new UnsupportedSourceFormatException(
                    (Processor) this, format);
        }
    }

}
