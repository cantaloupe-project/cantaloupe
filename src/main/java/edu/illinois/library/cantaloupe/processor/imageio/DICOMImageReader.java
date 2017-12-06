package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

final class DICOMImageReader extends AbstractImageReader {

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
    Compression getCompression(int imageIndex) throws IOException {
        return Compression.UNCOMPRESSED; // TODO: fix
    }

    @Override
    Metadata getMetadata(int imageIndex) throws IOException {
        if (iioReader == null) {
            createReader();
        }
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new NullMetadata(metadata, metadataFormat);
    }

    @Override
    Class<? extends javax.imageio.ImageReader> preferredIIOImplementation() {
        return null;
    }

}
