package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

import java.util.Set;

abstract class AbstractProcessor {

    static final String RESPECT_ORIENTATION_CONFIG_KEY =
            "metadata.respect_orientation";

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
                    getClass().getSimpleName() + " does not support the " +
                            format.getName() + " source format");
        }
    }

}
