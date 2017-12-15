package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

final class DICOMImageReader extends AbstractImageReader {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(DICOMImageReader.class);

    /**
     * @param sourceFile Source file to read.
     */
    DICOMImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.DCM);
    }

    /**
     * @param streamSource Source of streams to read.
     */
    DICOMImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.DCM);
    }

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.UNCOMPRESSED; // TODO: fix
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    Metadata getMetadata(int imageIndex) throws IOException {
        String metadataFormat;
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        if (metadata != null) {
            metadataFormat = metadata.getNativeMetadataFormatName();
        } else {
            metadataFormat = "DICOM";
        }
        return new NullMetadata(metadata, metadataFormat);
    }

}
