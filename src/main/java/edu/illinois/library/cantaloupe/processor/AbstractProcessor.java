package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.io.OutputStream;
import java.util.Set;

/**
 * Abstract base processor from which all processors should inherit.
 */
abstract class AbstractProcessor {

    private Format sourceFormat;

    abstract public Set<Format> getAvailableOutputFormats();

    public Format getSourceFormat() {
        return sourceFormat;
    }

    /**
     * Limited implementation that performs some preflight checks. Subclasses
     * should override and call super.
     *
     * @param ops Operation list to process.
     * @param imageInfo Source image info.
     * @param outputStream Output stream to write to.
     * @throws ProcessorException
     */
    public void process(final OperationList ops,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }
    }

    public void setSourceFormat(Format format)
            throws UnsupportedSourceFormatException{
        this.sourceFormat = format;
        if (getAvailableOutputFormats().isEmpty()) {
            throw new UnsupportedSourceFormatException(
                    (Processor) this, format);
        }
    }

}
