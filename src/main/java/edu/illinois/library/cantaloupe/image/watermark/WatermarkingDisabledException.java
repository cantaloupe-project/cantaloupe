package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.processor.ProcessorException;

public class WatermarkingDisabledException extends ProcessorException {

    public WatermarkingDisabledException() {
        super("Watermarking is disabled.");
    }

}
