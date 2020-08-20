package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

class MockPDFOnlyProcessor extends MockStreamProcessor {

    @Override
    public void setSourceFormat(Format format)
            throws SourceFormatException {
        if (Format.get("pdf").equals(format)) {
            super.setSourceFormat(format);
        } else {
            throw new SourceFormatException(format);
        }
    }

}
