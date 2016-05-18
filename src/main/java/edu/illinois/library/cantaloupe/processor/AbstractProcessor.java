package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.RequestAttributes;

import java.util.Set;

abstract class AbstractProcessor {

    protected Format format;
    protected RequestAttributes requestAttributes;

    abstract public Set<Format> getAvailableOutputFormats();

    public Format getSourceFormat() {
        return this.format;
    }

    public void setRequestAttributes(RequestAttributes attrs) {
        requestAttributes = attrs;
    }

    public void setSourceFormat(Format format)
            throws UnsupportedSourceFormatException{
        this.format = format;
        if (getAvailableOutputFormats().size() < 1) {
            throw new UnsupportedSourceFormatException(
                    getClass().getSimpleName() + " does not support the " +
                            format + " source format");
        }
    }

}
