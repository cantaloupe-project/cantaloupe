package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resource.RequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.script.ScriptException;
import java.awt.image.RenderedImage;
import java.io.IOException;

abstract class AbstractImageIoImageWriter {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractImageIoImageWriter.class);

    protected RequestAttributes requestAttributes;

    AbstractImageIoImageWriter(RequestAttributes attrs) {
        requestAttributes = attrs;
    }

    abstract protected IIOMetadata addMetadataUsingBasicStrategy(
            IIOMetadata defaultMetadata) throws IOException;

    abstract protected IIOMetadata addMetadataUsingScriptStrategy(
            IIOMetadata defaultMetadata) throws IOException, ScriptException;

    IIOMetadata getMetadata(final ImageWriter writer,
                            final ImageWriteParam writeParam,
                            final RenderedImage image) throws IOException {
        final Configuration config = Configuration.getInstance();

        if (config.getBoolean(IccProfileService.ICC_ENABLED_CONFIG_KEY, false)) {
            logger.debug("getMetadata(): ICC profiles enabled ({} = true)",
                    IccProfileService.ICC_ENABLED_CONFIG_KEY);
            final IIOMetadata metadata = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(image),
                    writeParam);
            switch (config.getString(IccProfileService.ICC_STRATEGY_CONFIG_KEY, "")) {
                case "BasicStrategy":
                    addMetadataUsingBasicStrategy(metadata);
                    return metadata;
                case "ScriptStrategy":
                    try {
                        addMetadataUsingScriptStrategy(metadata);
                        return metadata;
                    } catch (ScriptException e) {
                        throw new IOException(e.getMessage(), e);
                    }
            }
        }
        logger.debug("ICC profile disabled ({} = false)",
                IccProfileService.ICC_ENABLED_CONFIG_KEY);
        return null;
    }

}
