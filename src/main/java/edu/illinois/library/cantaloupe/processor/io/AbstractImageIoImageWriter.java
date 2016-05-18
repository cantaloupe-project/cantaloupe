package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.resource.RequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import java.awt.image.RenderedImage;
import java.io.IOException;

abstract class AbstractImageIoImageWriter {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractImageIoImageWriter.class);

    protected RequestAttributes requestAttributes;

    AbstractImageIoImageWriter(RequestAttributes attrs) {
        requestAttributes = attrs;
    }

    abstract protected IIOMetadata embedIccProfile(
            IIOMetadata metadata, IccProfile profile) throws IOException;

    IIOMetadata getMetadata(final ImageWriter writer,
                            final ImageWriteParam writeParam,
                            final RenderedImage image) throws IOException {
        final IccProfileService service = new IccProfileService();
        if (service.isEnabled()) {
            logger.debug("getMetadata(): ICC profiles enabled ({} = true)",
                    IccProfileService.ICC_ENABLED_CONFIG_KEY);
            final IIOMetadata metadata = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(image),
                    writeParam);
            IccProfile profile = service.getProfile(
                    requestAttributes.getOperationList().getIdentifier(),
                    requestAttributes.getHeaders(),
                    requestAttributes.getClientIp());
            if (profile != null) {
                return embedIccProfile(metadata, profile);
            }
        }
        logger.debug("ICC profile disabled ({} = false)",
                IccProfileService.ICC_ENABLED_CONFIG_KEY);
        return null;
    }

}
