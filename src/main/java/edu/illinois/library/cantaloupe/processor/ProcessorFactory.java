package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to obtain an instance of a {@link Processor} for a given source format,
 * as defined in the configuration.
 */
public abstract class ProcessorFactory {

    private static Logger logger = LoggerFactory.
            getLogger(ProcessorFactory.class);

    public static Set<Processor> getAllProcessors() { // TODO: return classes
        return new HashSet<Processor>(Arrays.asList(
                new FfmpegProcessor(),
                new GraphicsMagickProcessor(),
                new ImageMagickProcessor(),
                new JaiProcessor(),
                new Java2dProcessor(),
                new KakaduProcessor(),
                new OpenJpegProcessor(),
                new PdfBoxProcessor()));
    }

    /**
     * Retrieves the best-match processor for the given source format. Its
     * source will not be set.
     *
     * @param sourceFormat
     * @return An instance suitable for handling the given source format, based
     *         on configuration settings. The source is already set.
     * @throws UnsupportedSourceFormatException
     * @throws ReflectiveOperationException
     * @throws IOException
     */
    public static Processor getProcessor(final SourceFormat sourceFormat)
            throws UnsupportedSourceFormatException,
            ReflectiveOperationException,
            IOException {
        Processor processor = null;
        try {
            processor = getProcessor(null, null, sourceFormat);
        } catch (IncompatibleResolverException e) {
            // this will never happen
            logger.error("BUG ALERT in getProcessor(Identifier, SourceFormat)", e);
        }
        return processor;
    }

    /**
     * @param resolver Resolver from which the processor will be reading
     * @param identifier
     * @param sourceFormat The source format for which to return an instance,
     *                     based on configuration settings. If unsure, use
     *                     <code>SourceFormat.UNKNOWN</code>.
     * @return An instance suitable for handling the given source format, based
     *         on configuration settings. The source is already set.
     * @throws IncompatibleResolverException
     * @throws ClassNotFoundException If a fallback processor is needed but not
     *         defined.
     * @throws UnsupportedSourceFormatException If the processor assigned to
     *         the given source format, or the fallback processor, does not
     *         support the format.
     * @throws ReflectiveOperationException If a defined processor class is
     *         not found or cannot be instantiated.
     * @throws IOException
     */
    public static Processor getProcessor(final Resolver resolver,
                                         final Identifier identifier,
                                         final SourceFormat sourceFormat)
            throws IncompatibleResolverException,
            UnsupportedSourceFormatException,
            ReflectiveOperationException,
            IOException {
        String processorName = getAssignedProcessorName(sourceFormat);
        if (processorName == null) {
            processorName = getFallbackProcessorName();
            if (processorName == null) {
                throw new ClassNotFoundException("A fallback processor is not defined.");
            }
        }
        final String className = ProcessorFactory.class.getPackage().getName() +
                "." + processorName;
        final Class class_ = Class.forName(className);
        final Processor processor = (Processor) class_.newInstance();

        if (resolver != null && !resolver.isCompatible(processor)) {
            throw new IncompatibleResolverException(resolver, processor);
        }

        processor.setSourceFormat(sourceFormat);

        if (identifier != null) {
            // Set the processor's source
            if (resolver instanceof FileResolver &&
                    processor instanceof FileProcessor) {
                ((FileProcessor) processor).setSourceFile(
                        ((FileResolver) resolver).getFile(identifier));
            } else if (resolver instanceof StreamResolver) {
                ((StreamProcessor) processor).setStreamSource(
                        ((StreamResolver) resolver).getStreamSource(identifier));
            }
        }
        return processor;
    }

    private static String getAssignedProcessorName(SourceFormat sourceFormat) {
        Configuration config = Application.getConfiguration();
        return config.getString("processor." +
                sourceFormat.getPreferredExtension());
    }

    private static String getFallbackProcessorName() {
        Configuration config = Application.getConfiguration();
        return config.getString("processor.fallback");
    }

}
