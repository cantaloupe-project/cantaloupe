package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract class AbstractImageWriter {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(AbstractImageWriter.class);

    javax.imageio.ImageWriter iioWriter;
    OperationList opList;
    Metadata sourceMetadata;

    /**
     * @param opList Some operations can't be handled by processors and need
     *               to be handled by a writer instead. Any writer operations
     *               present in this list will be applied automatically.
     */
    AbstractImageWriter(final OperationList opList) {
        this.opList = opList;
        createWriter();
    }

    /**
     * @param opList Some operations can't be handled by processors and need
     *               to be handled by a writer instead. Any writer operations
     *               present in this list will be applied automatically.
     * @param sourceMetadata Metadata for the image being written as returned
     *                       from {@link ImageReader}.
     */
    AbstractImageWriter(final OperationList opList,
                        final Metadata sourceMetadata) {
        this(opList);
        this.sourceMetadata = sourceMetadata;
    }

    /**
     * <p>Embeds metadata from {@link #sourceMetadata} into the given tree.</p>
     *
     * <p>Writers for formats that don't support metadata may simply do
     * nothing.</p>
     *
     * @param baseTree Tree to embed the metadata into.
     */
    abstract void addMetadata(IIOMetadataNode baseTree) throws IOException;

    private List<javax.imageio.ImageWriter> availableIIOWriters() {
        final Iterator<javax.imageio.ImageWriter> it =
                ImageIO.getImageWritersByMIMEType(
                    opList.getOutputFormat().getPreferredMediaType().toString());

        final List<javax.imageio.ImageWriter> iioWriters = new ArrayList<>();
        while (it.hasNext()) {
            iioWriters.add(it.next());
        }
        return iioWriters;
    }

    private void createWriter() {
        this.iioWriter = negotiateImageWriter();

        LOGGER.debug("createWriter(): using {}",
                iioWriter.getClass().getName());
    }

    javax.imageio.ImageWriter getIIOWriter() {
        return iioWriter;

    }
    /**
     * @param writeParam Write parameters on which to base the metadata.
     * @param image Image to apply the metadata to.
     * @return Image metadata with added metadata corresponding to any
     *         writer-specific operations from
     *         {@link #AbstractImageWriter(OperationList, Metadata)}
     *         applied.
     */
    IIOMetadata getMetadata(final ImageWriteParam writeParam,
                            final RenderedImage image) throws IOException {
        final IIOMetadata derivativeMetadata = iioWriter.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(image),
                writeParam);
        final String formatName =
                derivativeMetadata.getNativeMetadataFormatName();
        final IIOMetadataNode baseTree =
                (IIOMetadataNode) derivativeMetadata.getAsTree(formatName);
        for (final Operation op : opList) {
            if (op instanceof MetadataCopy && sourceMetadata != null) {
                addMetadata(baseTree);
            }
        }
        derivativeMetadata.mergeTree(formatName, baseTree);
        return derivativeMetadata;
    }

    private javax.imageio.ImageWriter negotiateImageWriter() {
        javax.imageio.ImageWriter negotiatedWriter = null;

        final List<javax.imageio.ImageWriter> iioWriters =
                availableIIOWriters();
        boolean found = false;

        if (!iioWriters.isEmpty()) {
            final String[] preferredImplClasses = preferredIIOImplementations();

            if (preferredImplClasses.length > 0) {
                for (String preferredImplClass : preferredImplClasses) {
                    if (!found) {
                        for (javax.imageio.ImageWriter candidateWriter : iioWriters) {
                            if (preferredImplClass.equals(candidateWriter.getClass().getName())) {
                                negotiatedWriter = candidateWriter;
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (negotiatedWriter == null) {
                negotiatedWriter = iioWriters.get(0);
            }
        }
        return negotiatedWriter;
    }

    /**
     * N.B. This method returns a list of strings rather than classes because
     * some writers reside under the com.sun package which is private in
     * Java 9.
     *
     * @return Preferred writer implementation classes, in priority order, or
     *         an empty array if there is no preference.
     */
    String[] preferredIIOImplementations() {
        return new String[] {};
    }

    /**
     * Writes the given image to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     */
    abstract void write(RenderedImage image,
                        OutputStream outputStream) throws IOException;

}
