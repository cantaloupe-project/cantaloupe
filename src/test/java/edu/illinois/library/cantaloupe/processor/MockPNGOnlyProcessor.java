package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

class MockPNGOnlyProcessor extends MockStreamProcessor {

    @Override
    public void setSourceFormat(Format format)
            throws SourceFormatException {
        if (Format.PNG.equals(format)) {
            super.setSourceFormat(format);
        } else {
            throw new SourceFormatException(format);
        }
    }

}
