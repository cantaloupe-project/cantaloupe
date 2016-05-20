package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.icc.IccProfile;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import java.awt.image.RenderedImage;
import java.io.IOException;

abstract class AbstractImageIoImageWriter {

    protected OperationList opList;

    /**
     * @param opList Some operations can't be handled by processors and need
     *               to be handled by a writer instead. Any writer operations
     *               present in this list will be applied automatically.
     */
    AbstractImageIoImageWriter(OperationList opList) {
        this.opList = opList;
    }

    abstract protected IIOMetadata embedIccProfile(
            IIOMetadata metadata, IccProfile profile) throws IOException;

    /**
     * @param writer Writer to obtain the default metadata from.
     * @param writeParam Write parameters on which to base the metadata.
     * @param image Image to apply the metadata to.
     * @return Image metadata with added metadata corresponding to any
     *         writer-specific operations from
     *         {@link #AbstractImageIoImageWriter(OperationList)} applied.
     * @throws IOException
     */
    IIOMetadata getMetadata(final ImageWriter writer,
                            final ImageWriteParam writeParam,
                            final RenderedImage image) throws IOException {
        for (final Operation op : opList) {
            if (op instanceof IccProfile) {
                final IIOMetadata metadata = writer.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromRenderedImage(image),
                        writeParam);
                return embedIccProfile(metadata, (IccProfile) op);
            }
        }
        return null;
    }

}
