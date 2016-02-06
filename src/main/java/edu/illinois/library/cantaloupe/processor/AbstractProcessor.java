package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;

import java.util.Set;

abstract class AbstractProcessor {

    protected SourceFormat sourceFormat;

    abstract public Set<OutputFormat> getAvailableOutputFormats();

    public SourceFormat getSourceFormat() {
        return this.sourceFormat;
    }

    public void setSourceFormat(SourceFormat sourceFormat)
            throws UnsupportedSourceFormatException{
        this.sourceFormat = sourceFormat;
        if (getAvailableOutputFormats().size() < 1) {
            throw new UnsupportedSourceFormatException(
                    getClass().getSimpleName() + " does not support the " +
                            sourceFormat + " source format");
        }
    }

}
